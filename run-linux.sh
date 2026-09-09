#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$project_dir"

api_port="${ALPHA_API_PORT:-8080}"
api_token="${ALPHA_API_TOKEN:-alpha-local-development}"
db_container="${ALPHA_DB_CONTAINER:-alpha-income-mysql}"
db_host="${ALPHA_DB_HOST:-127.0.0.1}"
db_port="${ALPHA_DB_PORT:-3306}"
db_name="${ALPHA_DB_NAME:-alpha_income}"
db_user="${ALPHA_DB_USER:-alpha}"
db_password="${ALPHA_DB_PASSWORD:-alpha-local-development}"
mysql_image="${ALPHA_MYSQL_IMAGE:-mysql:8.4}"
db_volume="${ALPHA_DB_VOLUME:-alpha-income-mysql-data}"
db_bootstrap_file="${ALPHA_DB_BOOTSTRAP_FILE:-$project_dir/local-db/bootstrap.sql}"
db_bootstrap_database="${ALPHA_DB_BOOTSTRAP_DATABASE:-}"
manage_db="${ALPHA_MANAGE_DB:-1}"
api_log="$project_dir/target/alpha-api.log"
bootstrap_temp_file=""

if [[ -n "${ALPHA_API_DIR:-}" ]]; then
    api_dir="$ALPHA_API_DIR"
elif [[ -f "$project_dir/../Alpha-API/pom.xml" ]]; then
    api_dir="$project_dir/../Alpha-API"
elif [[ -f "$project_dir/alpha-api/pom.xml" ]]; then
    api_dir="$project_dir/alpha-api"
elif [[ -f "$project_dir/Alpha-API/pom.xml" ]]; then
    api_dir="$project_dir/Alpha-API"
else
    echo "Alpha-API was not found next to or inside $project_dir." >&2
    echo "Set ALPHA_API_DIR to the Alpha-API checkout and try again." >&2
    exit 1
fi
api_dir="$(cd -- "$api_dir" && pwd)"
if [[ ! -f "$api_dir/pom.xml" || ! -f "$api_dir/mvnw" ]]; then
    echo "ALPHA_API_DIR must contain the Alpha-API pom.xml and mvnw files (found '$api_dir')." >&2
    exit 1
fi

if [[ ! "$api_port" =~ ^[0-9]+$ ]] || (( api_port < 1 || api_port > 65535 )); then
    echo "ALPHA_API_PORT must be a port number between 1 and 65535." >&2
    exit 1
fi

if [[ ! "$db_port" =~ ^[0-9]+$ ]] || (( db_port < 1 || db_port > 65535 )); then
    echo "ALPHA_DB_PORT must be a port number between 1 and 65535." >&2
    exit 1
fi

if [[ "$manage_db" != "0" && "$manage_db" != "1" ]]; then
    echo "ALPHA_MANAGE_DB must be either 0 or 1." >&2
    exit 1
fi

if [[ "$manage_db" == "1" ]]; then
    if [[ ! -f "$db_bootstrap_file" || ! -r "$db_bootstrap_file" ]]; then
        echo "ALPHA_DB_BOOTSTRAP_FILE must point to a readable SQL file (found '$db_bootstrap_file')." >&2
        exit 1
    fi
    if [[ -n "$db_bootstrap_database" && ! "$db_bootstrap_database" =~ ^[A-Za-z0-9_]+$ ]]; then
        echo "ALPHA_DB_BOOTSTRAP_DATABASE may contain only letters, numbers, and underscores." >&2
        exit 1
    fi
    if [[ -n "$db_bootstrap_database" && "$db_bootstrap_database" != "$db_name" ]]; then
        echo "ALPHA_DB_BOOTSTRAP_DATABASE must match ALPHA_DB_NAME so the selected dump database is restored into the configured API database." >&2
        echo "  ALPHA_DB_BOOTSTRAP_DATABASE=$db_bootstrap_database" >&2
        echo "  ALPHA_DB_NAME=$db_name" >&2
        exit 1
    fi
    database_section_count="$(grep -c '^-- Current Database: ' "$db_bootstrap_file" || true)"
    if (( database_section_count > 1 )) && [[ -z "$db_bootstrap_database" ]]; then
        echo "The SQL file contains multiple databases. Set ALPHA_DB_BOOTSTRAP_DATABASE to the one that should be restored." >&2
        exit 1
    fi
    if [[ -n "$db_bootstrap_database" && "$database_section_count" -gt 0 ]]; then
        database_marker="-- Current Database: \`$db_bootstrap_database\`"
        if ! grep -Fq -- "$database_marker" "$db_bootstrap_file"; then
            echo "ALPHA_DB_BOOTSTRAP_DATABASE='$db_bootstrap_database' was not found in '$db_bootstrap_file'." >&2
            exit 1
        fi
    fi
fi

if ! command -v java >/dev/null 2>&1; then
    echo "Java was not found. Install JDK 22 or newer and try again." >&2
    exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
    echo "Maven was not found. Install Maven 3.8 or newer and try again." >&2
    exit 1
fi

if ! command -v curl >/dev/null 2>&1; then
    echo "curl was not found. Install curl and try again." >&2
    exit 1
fi

if ! command -v setsid >/dev/null 2>&1; then
    echo "setsid was not found. Install util-linux and try again." >&2
    exit 1
fi

java_major="$({ java -XshowSettings:properties -version; } 2>&1 | awk -F'= ' '/java.specification.version/ { print $2; exit }')"
java_major="${java_major#1.}"
if [[ ! "$java_major" =~ ^[0-9]+$ ]] || (( java_major < 22 )); then
    echo "JDK 22 or newer is required (found Java ${java_major:-unknown})." >&2
    exit 1
fi

config_file="$project_dir/src/main/resources/application.properties"
if [[ ! -f "$config_file" ]]; then
    cp "$project_dir/application.properties.example" "$config_file"
    echo "Created $config_file with local defaults."
fi

set_property() {
    local key="$1"
    local value="$2"
    local temp_file
    temp_file="$(mktemp "${config_file}.XXXXXX")"
    awk -F= -v target="$key" '$1 != target { print }' "$config_file" > "$temp_file"
    printf '%s=%s\n' "$key" "$value" >> "$temp_file"
    mv "$temp_file" "$config_file"
}

# The desktop client reads these values from its classpath rather than the
# environment, so keep its ignored development config in sync with the API.
set_property "api.base.url" "http://127.0.0.1:$api_port"
set_property "api.token" "$api_token"

api_pid=""
db_started=0
cleanup_done=0

cleanup() {
    local status=$?
    if (( cleanup_done )); then
        return "$status"
    fi
    cleanup_done=1

    if [[ -n "$bootstrap_temp_file" ]]; then
        rm -f -- "$bootstrap_temp_file" || true
        bootstrap_temp_file=""
    fi

    if [[ -n "$api_pid" ]] && { kill -0 "$api_pid" 2>/dev/null || kill -0 -- "-$api_pid" 2>/dev/null; }; then
        echo "Stopping Alpha API..."
        kill -TERM -- "-$api_pid" 2>/dev/null || kill -TERM "$api_pid" 2>/dev/null || true
        for _ in {1..20}; do
            kill -0 -- "-$api_pid" 2>/dev/null || break
            sleep 0.25
        done
        if kill -0 -- "-$api_pid" 2>/dev/null; then
            kill -KILL -- "-$api_pid" 2>/dev/null || kill -KILL "$api_pid" 2>/dev/null || true
        fi
        wait "$api_pid" 2>/dev/null || true
    fi

    if (( db_started )); then
        echo "Stopping local MySQL..."
        docker stop --time 10 "$db_container" >/dev/null 2>&1 || true
    fi

    return "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

if (( manage_db )); then
    if ! command -v docker >/dev/null 2>&1; then
        echo "Docker was not found. Install/start Docker, or set ALPHA_MANAGE_DB=0 to use an existing MySQL server." >&2
        exit 1
    fi
    if ! docker info >/dev/null 2>&1; then
        echo "The Docker daemon is not available. Start Docker and try again." >&2
        echo "Alternatively, set ALPHA_MANAGE_DB=0 and configure ALPHA_DB_* for an existing MySQL server." >&2
        exit 1
    fi

    if docker container inspect "$db_container" >/dev/null 2>&1; then
        managed_label="$(docker container inspect --format '{{ index .Config.Labels "com.alpha-income.local-db" }}' "$db_container")"
        if [[ "$managed_label" != "true" ]]; then
            echo "A Docker container named '$db_container' already exists but is not managed by this launcher." >&2
            echo "Set ALPHA_DB_CONTAINER to another name or ALPHA_MANAGE_DB=0 to use your own database." >&2
            exit 1
        fi

        container_args="$(docker container inspect --format '{{json .Args}}' "$db_container")"
        if [[ "$container_args" != *"--lower-case-table-names=1"* ]]; then
            echo "The existing '$db_container' container predates the local schema setup." >&2
            echo "If it has no local data you need, remove it and its volume, then run this launcher again:" >&2
            echo "  docker rm -f $db_container" >&2
            echo "  docker volume rm $db_volume" >&2
            echo "Warning: removing the volume permanently deletes its local database data." >&2
            exit 1
        fi

        if [[ "$(docker container inspect --format '{{.State.Running}}' "$db_container")" != "true" ]]; then
            echo "Starting local MySQL..."
            docker start "$db_container" >/dev/null
            db_started=1
        else
            echo "Using the running local MySQL container '$db_container'."
        fi
    else
        echo "Creating local MySQL container '$db_container'..."
        docker run -d \
            --name "$db_container" \
            --label com.alpha-income.local-db=true \
            -e "MYSQL_ROOT_PASSWORD=$db_password" \
            -e "MYSQL_DATABASE=$db_name" \
            -e "MYSQL_USER=$db_user" \
            -e "MYSQL_PASSWORD=$db_password" \
            -p "127.0.0.1:$db_port:3306" \
            -v "$db_volume:/var/lib/mysql" \
            "$mysql_image" \
            --lower-case-table-names=1 >/dev/null
        db_started=1
    fi

    echo "Waiting for MySQL..."
    mysql_ready=0
    for _ in {1..90}; do
        if docker exec "$db_container" mysqladmin ping --silent >/dev/null 2>&1; then
            mysql_ready=1
            break
        fi
        if [[ "$(docker container inspect --format '{{.State.Running}}' "$db_container" 2>/dev/null || true)" != "true" ]]; then
            break
        fi
        sleep 1
    done
    if (( ! mysql_ready )); then
        echo "MySQL did not become ready. Recent container output:" >&2
        docker logs --tail 40 "$db_container" >&2 || true
        exit 1
    fi

    bootstrap_restore_file="$db_bootstrap_file"
    if [[ -n "$db_bootstrap_database" && "$database_section_count" -gt 0 ]]; then
        # --one-database still sends USE statements for databases whose CREATE
        # DATABASE statements it skipped. Keep the dump preamble (including
        # its session-variable setup) and the requested database section only.
        bootstrap_temp_file="$(mktemp)"
        database_marker="-- Current Database: \`$db_bootstrap_database\`"
        awk -v wanted="$database_marker" '
            BEGIN { in_target = 0; seen_database = 0 }
            {
                line = $0
                sub(/\r$/, "", line)
                if (line ~ /^-- Current Database: `/) {
                    seen_database = 1
                    if (line == wanted) {
                        in_target = 1
                        print $0
                        next
                    }
                    if (in_target) exit
                    in_target = 0
                    next
                }
                if (!seen_database || in_target) print $0
            }
        ' "$db_bootstrap_file" > "$bootstrap_temp_file"
        bootstrap_restore_file="$bootstrap_temp_file"
    fi

    if [[ -n "$db_bootstrap_database" ]]; then
        echo "Restoring '$db_bootstrap_database' from $db_bootstrap_file..."
        # A mysqldump may contain privileged session metadata (for example,
        # SET GLOBAL INNODB_STATS_AUTO_RECALC). The container root password is
        # the configured local DB password, so use root only for this restore;
        # the API still connects with db_user below.
        bootstrap_mysql_args=(--user=root "$db_name")
    else
        echo "Creating/updating the local test schema from $db_bootstrap_file..."
        bootstrap_mysql_args=(--user="$db_user" "$db_name")
    fi
    if ! docker exec -i \
        -e "MYSQL_PWD=$db_password" \
        "$db_container" \
        mysql "${bootstrap_mysql_args[@]}" \
        < "$bootstrap_restore_file" >/dev/null; then
        echo "Failed to create or seed the local test database from '$db_bootstrap_file'." >&2
        exit 1
    fi
fi

if curl --silent --output /dev/null --connect-timeout 1 --max-time 2 "http://127.0.0.1:$api_port/"; then
    echo "Port $api_port is already in use; refusing to start a second API." >&2
    exit 1
fi

mkdir -p "$project_dir/target"
: > "$api_log"

datasource_url="${ALPHA_DB_URL:-jdbc:mysql://$db_host:$db_port/$db_name?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Australia/Melbourne}"

echo "Starting Alpha API from $api_dir..."
(
    cd "$api_dir"
    exec setsid env \
        API_TOKEN="$api_token" \
        SERVER_ADDRESS=127.0.0.1 \
        SERVER_PORT="$api_port" \
        SPRING_DATASOURCE_URL="$datasource_url" \
        SPRING_DATASOURCE_USERNAME="$db_user" \
        SPRING_DATASOURCE_PASSWORD="$db_password" \
        SPRING_JPA_HIBERNATE_DDL_AUTO=update \
        bash ./mvnw spring-boot:run
) > "$api_log" 2>&1 &
api_pid=$!

echo "Waiting for Alpha API (log: $api_log)..."
api_ready=0
for _ in {1..180}; do
    if curl --fail --silent --output /dev/null \
        --connect-timeout 1 --max-time 2 \
        -H "Authorization: Bearer $api_token" \
        "http://127.0.0.1:$api_port/actuator/health"; then
        api_ready=1
        break
    fi
    if ! kill -0 "$api_pid" 2>/dev/null; then
        break
    fi
    sleep 1
done

if (( ! api_ready )); then
    echo "Alpha API did not become ready. Recent API output:" >&2
    tail -n 80 "$api_log" >&2 || true
    exit 1
fi

echo "Alpha API is ready at http://127.0.0.1:$api_port."

if [[ "${XDG_SESSION_TYPE:-}" == "wayland" || -n "${WAYLAND_DISPLAY:-}" ]]; then
    if [[ -z "${DISPLAY:-}" ]]; then
        echo "This JavaFX build needs XWayland, but DISPLAY is not set." >&2
        echo "Install/enable XWayland in your compositor, then try again." >&2
        exit 1
    fi

    # JavaFX 22 and FX-BorderlessScene use the GTK/X11 windowing path.
    export GDK_BACKEND=x11
fi

set +e
mvn javafx:run "$@"
client_status=$?
set -e
exit "$client_status"

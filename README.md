# Alpha Income Management

## Run locally on Linux/Wayland

The desktop client requires:

- JDK 22 or newer (a JDK, not only a JRE)
- Maven 3.8 or newer
- Docker (for the local MySQL database)
- `curl` and `setsid` (normally provided by `curl` and `util-linux`)
- GTK 3
- XWayland enabled in the Wayland compositor

On Arch Linux/CachyOS, install those dependencies with:

```bash
sudo pacman -S jdk-openjdk maven docker curl util-linux gtk3 xorg-xwayland
```

The application currently uses JavaFX 22 and `FX-BorderlessScene`. That windowing combination runs through XWayland; the launcher selects the GTK X11 backend automatically when it detects a Wayland session.

Run:

```bash
./run-linux.sh
```

The launcher finds `Alpha-API` beside this checkout, starts a persistent local MySQL database in Docker, applies the local test schema, starts the API, waits for it to become healthy, and then opens the desktop client. Closing the client stops the API and any database container started by the launcher; the database data is retained in the `alpha-income-mysql-data` Docker volume.

The local database includes a test store and an administrator with access to every screen:

```text
Username: admin
Password: admin
Store:    Local Test Store
```

These development credentials are reset to their defaults on every launcher run. They are only exposed through services bound to `127.0.0.1`.

### Restore a database backup for local testing

The launcher applies `local-db/bootstrap.sql` by default. To restore one database from a multi-database SQL export, set `ALPHA_DB_BOOTSTRAP_FILE`, `ALPHA_DB_BOOTSTRAP_DATABASE`, and `ALPHA_DB_NAME` to the same database name. The database selector makes the launcher ignore the other schemas in the dump.

The supplied production export contains the application tables in `rosterdb_prod`, so use a separate container and volume for the test copy:

```bash
ALPHA_DB_BOOTSTRAP_FILE="$PWD/local-db/backup_2026-09-09.sql" \
ALPHA_DB_BOOTSTRAP_DATABASE=rosterdb_prod \
ALPHA_DB_NAME=rosterdb_prod \
ALPHA_DB_CONTAINER=alpha-income-prod-test \
ALPHA_DB_VOLUME=alpha-income-prod-test-data \
./run-linux.sh
```

Use a new container/volume rather than your normal local test volume so the two datasets stay separate. The imported dump's users and passwords will be used; `admin/admin` is only created by the default local bootstrap. Keep production exports out of source control and do not distribute a build containing that data.

On the first run, the launcher creates the ignored file `src/main/resources/application.properties` from `application.properties.example`. Each local run synchronises its API URL and token with the locally started API.

## Configure Gemini invoice scanning

AI invoice and supplier-statement scanning uses Gemini Flash. To enable it locally:

1. Create an API key in [Google AI Studio](https://aistudio.google.com/app/apikey). The Google project behind the key must have Gemini API access and billing/quota appropriate for your scan volume.
2. Open the ignored local file `src/main/resources/application.properties`. If it does not exist, run `./run-linux.sh` once or copy `application.properties.example` there.
3. Set the key without quotes:

   ```properties
   gemini.api.key=your-google-ai-studio-key
   gemini.model=gemini-3.8-flash
   gemini.statement.model=gemini-3.8-flash
   gemini.statement.max-output-tokens=65536
   gemini.statement.pages-per-request=4
   gemini.statement.parallel.max-concurrent=3
   gemini.statement.completeness-attempts=2
   gemini.service-tier=priority
   gemini.request.timeout-seconds=600
   gemini.file-processing-timeout-seconds=300
   gemini.thinking-level=low
   gemini.parallel.max-concurrent=3
   gemini.retry.max-attempts=5
   gemini.retry.initial-delay-seconds=2
   gemini.retry.max-delay-seconds=30
   ```

4. Fully restart the application. Maven/your IDE must copy the updated resource before the running process can see it.

For CI or deployed environments, prefer the `GEMINI_API_KEY` environment variable so the credential is not included in a build artifact:

```bash
GEMINI_API_KEY='your-google-ai-studio-key' ./run-linux.sh
```

Runtime precedence is Java system property (`-Dgemini.api.key=...`), environment variable, then `application.properties`. Do not commit or distribute an application JAR built while a real key is present in `src/main/resources/application.properties`, because Maven packages that resource into the JAR.

`gemini.service-tier=priority` requests Gemini Priority inference for model extraction. Priority requires a Tier 2 or Tier 3 Gemini API project and is billed at a premium. Set it to `standard` to opt out, or override it at launch with `GEMINI_SERVICE_TIER=standard`. If Google exhausts available priority capacity and gracefully downgrades a request, Alpha writes that downgrade to its application log. File upload itself is not inference and therefore does not use a service tier.

PDFs must be no larger than 50 MB or 1,000 pages. Supplier statements are physically split into four-page chunks by default, each chunk is uploaded through Gemini's Files API, and up to three chunks are extracted concurrently after the first chunk establishes the reporting period. Every response must account for every supplied page—even pages with zero relevant rows—and its per-page counts must equal the number of returned transactions. A failed completeness check is retried, then reported as an error instead of silently importing a partial result. The temporary chunk page numbers and evidence boxes are mapped back to the original PDF before review. Tune this with `gemini.statement.pages-per-request` (1-12), `gemini.statement.parallel.max-concurrent` (1-6), and `gemini.statement.completeness-attempts` (1-4).

Gemini infers a statement's reporting period from an explicit printed period when available, otherwise from the statement date, and only falls back to the latest transaction month when neither is present. It returns invoices and credits whose transaction dates fall inside that period. The app independently reapplies the inferred date range and loads Z-Office data for every calendar month covered by it, so statement reconciliation does not depend on the month currently displayed in the invoicing screen. When Gemini finds an explicitly printed whole-statement amount, closing balance, or amount due, the results screen compares it with the signed sum of extracted invoices and credits and displays the variance; missing totals remain clearly marked as unavailable rather than being inferred. Statement extraction uses `gemini.statement.model`, medium PDF media resolution, and the larger `gemini.statement.max-output-tokens` allowance. When multiple individual invoice PDFs are selected, up to `gemini.parallel.max-concurrent` files are uploaded and extracted concurrently; the default is three and the accepted range is one to eight. Each upload is deleted after extraction. Extracted rows retain the user's selected-file order. Gemini supplies individual confidence values for supplier, reference, date, document type, and amount; the corresponding review-table cell is highlighted red at 90% or below. An invoice without a visible due date defaults to 30 days after its invoice date; credits remain without a due date. The scan result starts as a full-width reconciliation table. Choosing Review opens a focused workspace with the large, highlighted PDF on the left and an editable invoice or credit form on the right; saving returns to the same list and marks the row as saved. Text-layer matching fills in a missing Gemini box when possible.

Temporary HTTP 408, 429, and 5xx responses during model extraction are retried automatically with exponential backoff and jitter. The defaults make five total attempts, starting around two seconds apart and capping each wait around 30 seconds. If all attempts fail, the expanded error in the app identifies the failed stage and attempt count. HTTP 401/403 usually means the key or project access is wrong; persistent HTTP 429 responses can mean the project has reached its current quota.

The defaults can be overridden with environment variables. For example, to use an existing MySQL server rather than Docker:

```bash
ALPHA_MANAGE_DB=0 \
ALPHA_DB_HOST=127.0.0.1 \
ALPHA_DB_PORT=3306 \
ALPHA_DB_NAME=alpha_income \
ALPHA_DB_USER=alpha \
ALPHA_DB_PASSWORD=your-password \
./run-linux.sh
```

Set `ALPHA_API_DIR` if the API checkout is somewhere other than beside/inside this project. Other available overrides are `ALPHA_API_PORT`, `ALPHA_API_TOKEN`, `ALPHA_DB_URL`, `ALPHA_DB_CONTAINER`, `ALPHA_DB_VOLUME`, `ALPHA_DB_BOOTSTRAP_FILE`, `ALPHA_DB_BOOTSTRAP_DATABASE`, and `ALPHA_MYSQL_IMAGE`.

Application output is written to `AlphaIncome.log`.

### Build without launching

```bash
mvn package
```

The packaged jDeploy input is written to `target/jpackage-input`. The JAR there intentionally does not bundle JavaFX; use `./run-linux.sh` for development rather than `java -jar`.

### Troubleshooting

- `DISPLAY is not set`: install or enable XWayland in the compositor.
- `The Docker daemon is not available`: start Docker (for example, `sudo systemctl start docker`) or use an existing MySQL server with `ALPHA_MANAGE_DB=0`.
- GTK library errors: install your distribution's GTK 3 package (commonly `gtk3` or `libgtk-3-0`).
- Rendering issues: try software rendering with `./run-linux.sh -Dprism.order=sw`.

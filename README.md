# Alpha Income Management

## Run locally on Linux/Wayland

The desktop client requires:

- JDK 26 or newer (a JDK, not only a JRE)
- Maven 3.8 or newer
- Docker (for the local MySQL database)
- `curl` and `setsid` (normally provided by `curl` and `util-linux`)
- GTK 3
- XWayland enabled in the Wayland compositor

On Arch Linux/CachyOS, install those dependencies with:

```bash
sudo pacman -S jdk-openjdk maven docker curl util-linux gtk3 xorg-xwayland
```

The application uses JavaFX 26 and `FX-BorderlessScene`. That windowing combination runs through XWayland. Both the development script and the packaged application launcher select the GTK X11 backend before JavaFX starts when they detect a Wayland session.

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

## Configure Google Drive document scanning

AI invoice and supplier-statement scanning runs in Alpha API so its queue continues processing when no desktop client is open.

1. Create an API key in [Google AI Studio](https://aistudio.google.com/app/apikey). The Google project behind the key must have Gemini API access and billing/quota appropriate for your scan volume.
2. In Google Cloud, enable Google Drive API and create an OAuth 2.0 Web application client. Register the Alpha API callback exactly, for example `http://127.0.0.1:8080/document-ai/google/callback` locally or the HTTPS equivalent in production.
3. Configure the Alpha API process. Environment variables are recommended:

   ```bash
   GEMINI_API_KEY='your-google-ai-studio-key' \
   GOOGLE_DRIVE_CLIENT_ID='your-oauth-client-id' \
   GOOGLE_DRIVE_CLIENT_SECRET='your-oauth-client-secret' \
   GOOGLE_DRIVE_REDIRECT_URI='http://127.0.0.1:8080/document-ai/google/callback' \
   GOOGLE_DRIVE_CREDENTIALS_ENCRYPTION_KEY='a-stable-long-random-secret' \
   ./run-linux.sh
   ```

   The sibling Alpha API repository contains `application.properties.example` with all tuning options. Do not rotate the credential-encryption key without first reconnecting stores; it protects saved Google refresh tokens.

4. Grant `Document AI - Configure` to users who may administer a store's Drive connection, folders, and schedule. Grant `Document AI - Review` to users who may open the review queue, trigger **Scan now**, and review or dismiss its batches. The local `admin` user receives both permissions.
5. Open **Settings** from the side menu, connect Google Drive for the current store, choose watched folders, and save the automatic interval. Invoice folders only ingest PDF names containing `MARKED_OFF` (case-insensitive). Statement folders ingest every PDF directly inside the folder.
6. Open **AI scan** from Invoicing to review queued documents or use **Scan now** at any time. Drive file ID plus content revision and filename are deduplicated, so overlapping scans do not enqueue the same PDF twice. Replacing a file with a new revision, or renaming it, creates a new review item (provided it remains a direct child of a watched folder and invoice files still contain `MARKED_OFF`).

After Gemini successfully processes an invoice, Alpha API adds the TagSpaces `SCANNED` tag and moves it into the source folder's `ENTERED/` staging folder. It remains there until review accepts it. On acceptance, Alpha creates `FYyyyy/Month yyyy/` beneath that staging folder, moves the PDF there, and adds `COMPLETED` to the same tag block—for example, `[RECEIVED MARKED_OFF]` becomes `[RECEIVED MARKED_OFF SCANNED COMPLETED]`. A file without tags receives `[SCANNED]` first and then `[SCANNED COMPLETED]`. The fiscal year follows the Australian July–June year, so September 2025 is filed under `FY2026/September 2025`. Dismissing a queue batch or deleting a source document from the review screen deletes its Drive PDF while retaining the database audit record. Because this requires Drive file-management access, stores connected before this behavior was added must reconnect Google Drive and approve the broader Drive permission.

Google credentials are encrypted at rest per store in Alpha API. The OAuth callback uses a one-time, ten-minute state value, and PDF/configuration endpoints additionally require a short-lived user session, employment access to that store, and the corresponding Document AI permission. Other Alpha Income frontends can use the same API: successful `POST /users/{username}/verify-password` responses include `X-Alpha-Session`, which the client sends on `/document-ai/**` requests alongside the normal API bearer token.

For CI or deployed environments, prefer the `GEMINI_API_KEY` environment variable so the credential is not included in a build artifact:

```bash
GEMINI_API_KEY='your-google-ai-studio-key' ./run-linux.sh
```

Runtime precedence is Java system property (`-Dgemini.api.key=...`), environment variable, then the Alpha API's `application.properties`. Do not commit or distribute an API JAR built while a real key is present in its resources.

`gemini.service-tier=priority` requests Gemini Priority inference for model extraction. Priority requires a Tier 2 or Tier 3 Gemini API project and is billed at a premium. Set it to `standard` to opt out, or override it at launch with `GEMINI_SERVICE_TIER=standard`. If Google exhausts available priority capacity and gracefully downgrades a request, Alpha writes that downgrade to its application log. File upload itself is not inference and therefore does not use a service tier.

PDFs must be no larger than 50 MB or 1,000 pages. Supplier statements are physically split into four-page chunks by default, each chunk is uploaded through Gemini's Files API, and up to three chunks are extracted concurrently after the first chunk establishes the reporting period. Every response must account for every supplied page—even pages with zero relevant rows—and its per-page counts must equal the number of returned transactions. A failed completeness check is retried, then reported as an error instead of silently importing a partial result. The temporary chunk page numbers and evidence boxes are mapped back to the original PDF before review. Tune this with `gemini.statement.pages-per-request` (1-12), `gemini.statement.parallel.max-concurrent` (1-6), and `gemini.statement.completeness-attempts` (1-4).

Gemini infers a statement's reporting period from an explicit printed period when available, otherwise from the statement date, and only falls back to the latest transaction month when neither is present. It returns invoices and credits whose transaction dates fall inside that period. The app independently reapplies the inferred date range and loads Z-Office data for every calendar month covered by it, so statement reconciliation does not depend on the month currently displayed in the invoicing screen. When Gemini finds an explicitly printed whole-statement amount, closing balance, or amount due, the results screen compares it with the signed sum of extracted invoices and credits and displays the variance; missing totals remain clearly marked as unavailable rather than being inferred. Statement extraction uses `gemini.statement.model`, medium PDF media resolution, and the larger `gemini.statement.max-output-tokens` allowance. The API groups new invoice PDFs from one watched-folder run into a review batch and keeps each statement as its own review entry. Each Gemini upload is deleted after extraction. Gemini supplies individual confidence values for supplier, reference, date, document type, and amount; the corresponding review-table cell is highlighted red at 90% or below. Extracted supplier names are also matched against the store's saved supplier contacts; strong, unambiguous matches use the saved contact name, including ordinary initials and repeated-initial shorthand such as `CH2` for Clifford Hallom Healthcare, while uncertain matches remain flagged for review. An invoice without a visible due date defaults to 30 days after its invoice date; credits remain without a due date. Choosing a queued batch opens its reconciliation rows; choosing Review then opens the highlighted PDF and editable invoice or credit form. A batch remains queued until all rows are accepted and saved, or it is explicitly dismissed.

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

The packaged runtime defaults to the quality rendering profile, preserving the
original transitions and transparent-window appearance. On lower-power PCs the
balanced or low-power profile can be selected to reduce animation and compositor
work.
The profile can be overridden when diagnosing a machine:

```text
-Dalpha.performance.mode=quality    # packaged default; original animations
-Dalpha.performance.mode=balanced   # shorter animations; no layout animation
-Dalpha.performance.mode=low-power  # shorter transform animations
-Dalpha.performance.mode=off        # no animations
-Dalpha.window.transparent=true     # packaged default; original window styling
-Dalpha.window.transparent=false    # lower compositor cost on integrated GPUs
```

jDeploy uses Direct3D with a software fallback on Windows and OpenGL ES with a
software fallback on Linux. The options are platform-qualified in
`package.json`, so a Windows renderer is never requested on Linux (or vice
versa).

### Troubleshooting

- `DISPLAY is not set`: install or enable XWayland in the compositor.
- `The Docker daemon is not available`: start Docker (for example, `sudo systemctl start docker`) or use an existing MySQL server with `ALPHA_MANAGE_DB=0`.
- GTK library errors: install your distribution's GTK 3 package (commonly `gtk3` or `libgtk-3-0`).
- Rendering issues: try software rendering with `./run-linux.sh -Dprism.order=sw`.

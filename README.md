# Alpha Income Management

## Live Medicare income

With `z.live.enabled=true`, report loads fetch fresh **Govt Recovery** and
**Script Count** values for their entire requested date range through Alpha API
and the connected Z forwarder. BAS totals include the full selected month;
the Xero CSV fills each day's script count and fresh Medicare value, using
explicit zeros for no-script days after a successful live fetch. Monthly Summary uses those
values in income and profit calculations, includes Medicare income even before
staff enter that day's EOD figures, and reloads before either clipboard export.
Prior-year budget comparisons and script-count graphs use the same
shared report service. Failed live requests surface an error instead of
substituting old CSV income, and a failed Xero data load leaves the selected
output file untouched. Manual-import mode (`z.live.enabled=false`) continues
to use imported data.

The EOD Xero export refreshes all Z metrics for the selected month, writes
properly quoted CSV rows with 31 columns, and includes saved EOD values.
See [the export comparison and accounting limitations](docs/xero-export-review.md).

Deploy the matching Alpha API with `POST /till-report/refresh/script-totals`
before releasing this client. It uses the forwarder's existing `ScriptTotals`
operation and updates only Govt Recovery and Script Count in Alpha's database;
no forwarder upgrade or scheduled-export backfill is required for these live
report requests. The live agent must be configured and connected.

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

1. Follow the [business Gemini API key setup guide](docs/gemini-business-setup.md) to create a business-owned project and key, enable paid billing, set spending controls, and hand the key to the Alpha API maintainer.
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
5. Open **Settings** from the side menu, connect Google Drive for the current store, choose watched folders, and save the automatic interval. Enter **Every [number] [minutes / hours / days]**, for example every 2 hours, 3 days, or 30 days. Intervals must be whole numbers from 5 minutes to 365 days; intervals longer than 7 days require the matching Alpha API update. Existing schedules retain their exact interval. Saving sets the next scan one interval from now, and each completed scan schedules the next run one interval after completion. Invoice folders only ingest PDF names containing `MARKED_OFF` (case-insensitive). Statement folders ingest every PDF directly inside the folder.
6. Open **AI scan** from Invoicing to review queued documents or use **Scan now** at any time. Drive file ID plus content revision and filename are deduplicated, so overlapping scans do not enqueue the same PDF twice. Replacing a file with a new revision, or renaming it, creates a new review item (provided it remains a direct child of a watched folder and invoice files still contain `MARKED_OFF`).

Qualifying individual invoice and credit PDFs are now **entered automatically by Alpha API** after the existing supplier, confidence and Z-Office amount checks pass. PDFs with exceptions remain in review. Statements wait for supplier invoice entry, then compare each statement row with the amounts entered from independent invoice/credit scans. They do not create invoice entries or query Z-Office amounts. Missing and ambiguous references keep reconciliation open; verified amount differences can be acknowledged, or matched invoice/credit amounts, dates and notes can be corrected and saved together with the statement. See [invoice-first statement reconciliation and rollout](docs/statement-reconciliation.md).

For a monthly review, choose **Review all invoices** on the right of the scan toolbar to open every currently ready invoice batch for the selected store in one reconciliation table. Review and accept the exceptions, then use **Save accepted documents** once to save the entries and complete each original batch. Individual **Review** buttons remain available. Statements are always reviewed individually so their printed and extracted overall totals can be compared. Failed, queued, and scanning invoice batches stay in the queue; retry failed scans separately. The combined invoice table identifies each source PDF. Symbion invoice reconciliation tries the exact reference first, then the equivalent numeric reference with or without its leading `I` (for example, `I222534680` and `222534680`). Leading zeroes are preserved. The source PDF reference stays on the scanned row; review and saving default to the matched Z-Office reference, while explicit user edits are kept. This fallback applies only to Symbion invoices; other suppliers and credit prefixes remain distinct. Missing reference lookups are cached within the load so repeated rows do not repeat the same API requests. If saving succeeds but completing or filing a batch fails, **Retry completion** resumes the unfinished batches without saving those entries again in the same review session.

**Review all invoices** refreshes the queue before choosing ready batches. Saving attempts completion of every included batch even if another batch fails, and any failure message lists all unfinished batches. If a completion response times out or a retry reports that the batch is no longer ready, the client checks its backend status and accepts only a confirmed completion. After saving, the queue indicates whether the reviewed batches cleared and whether other invoice batches are now ready; batches that become ready during a review require another review.

After Gemini successfully processes an invoice, Alpha API adds the TagSpaces `SCANNED` tag and moves it into the source folder's `ENTERED/` staging folder. It remains there until automatic entry succeeds or a reviewer accepts it. On entry, Alpha creates `FYyyyy/Month yyyy/` beneath that staging folder, moves the PDF there, and adds `COMPLETED` to the same tag block—for example, `[RECEIVED MARKED_OFF]` becomes `[RECEIVED MARKED_OFF SCANNED COMPLETED]`. A file without tags receives `[SCANNED]` first and then `[SCANNED COMPLETED]`. The fiscal year follows the Australian July–June year, so September 2025 is filed under `FY2026/September 2025`. Dismissing a queue batch or removing a source document from the review screen moves its Drive PDF to Google Drive trash while retaining the database audit record. The PDF can then be restored using Google Drive's normal trash recovery workflow. Because this requires Drive file-management access, stores connected before this behavior was added must reconnect Google Drive and approve the broader Drive permission.

The reconciliation toolbar shows the selected-row count and groups bulk edit and acceptance actions separately. **Edit selected** opens a labelled editor for supplier, document date, due date, or notes. For suppliers, choose a saved contact or select **New supplier** and enter a name. Applying changes updates the review draft; accept those rows and save accepted documents to persist them. A new supplier is created only at save time, while an equivalent existing name is reused. New names shared by several invoices or credits are resolved once per review. Saved rows are excluded from bulk editing, and due-date changes apply only to invoices.

Z-Office credits appear in both the **Credits** list and individual PDF reconciliation when the API has a configured agent for the store. The list shows Alpha's saved amount, the Z-Office amount, and the source; Z-Office-only rows are read-only. Scanned credits use their own cached, exact-reference batch lookups and can be refreshed from the PDF review form. Credit amounts are signed negative in statement reconciliation and shown as positive magnitudes in the credit form. See [credit lookup and rollout details](docs/z-office-credits.md).

Individual invoice review opening uses the MySQL invoice lookup cache and imported amount datapoints first. Matching cached amounts never require a live Z-Office call. Missing, ambiguous, or mismatched amounts are rechecked using exact-reference `InvoiceBatch` queries of at most 100 references, including Symbion aliases. The desktop splits large reviews into requests of 50 invoice rows and shows a progress bar with a completed-record count across the entire review. Fresh exceptional results are reused for two minutes (`alpha.z-agent.invoice-retry-seconds` in Alpha API); the row's manual refresh button bypasses that delay. An agent failure stops the current load's live attempts and starts a short per-store cooldown across subsequent chunks, preserving cached amounts. Deploy the updated **z-db-forwarder first, Alpha API second, desktop last**. See [statement loading and deployment details](docs/statement-loading.md).

Google Drive remains the source of truth for PDF content. New queue records store the Drive file ID, source revision, filename, scan status, and extracted reconciliation data, but not a copy of the PDF body. Alpha API downloads the source by its stable Drive file ID only when Gemini processes it or a reviewer opens the preview. The nullable `pdf_data` database column remains solely as a compatibility fallback for queue records created by older API builds.

Google credentials are encrypted at rest per store in Alpha API. The OAuth callback uses a one-time, ten-minute state value, and PDF/configuration endpoints additionally require a short-lived user session, employment access to that store, and the corresponding Document AI permission. Other Alpha Income frontends can use the same API: successful `POST /users/{username}/verify-password` responses include `X-Alpha-Session`, which the client sends on `/document-ai/**` requests alongside the normal API bearer token.

For CI or deployed environments, prefer the `GEMINI_API_KEY` environment variable so the credential is not included in a build artifact:

```bash
GEMINI_API_KEY='your-google-ai-studio-key' ./run-linux.sh
```

Runtime precedence is Java system property (`-Dgemini.api.key=...`), environment variable, then the Alpha API's `application.properties`. Do not commit or distribute an API JAR built while a real key is present in its resources.

`gemini.service-tier=priority` requests Gemini Priority inference for model extraction. Priority requires a Tier 2 or Tier 3 Gemini API project and is billed at a premium. Set it to `standard` to opt out, or override it at launch with `GEMINI_SERVICE_TIER=standard`. If Google exhausts available priority capacity and gracefully downgrades a request, Alpha writes that downgrade to its application log. File upload itself is not inference and therefore does not use a service tier.

PDFs must be no larger than 50 MB or 1,000 pages. Supplier statements are physically split into four-page chunks by default, each chunk is uploaded through Gemini's Files API, and up to three chunks are extracted concurrently after the first chunk establishes the reporting period. Statement responses group transactions under explicit page objects, including pages with zero relevant rows. Alpha derives the counts and evidence page numbers from those groups instead of asking Gemini to tally long transaction arrays. Missing or duplicate pages still fail coverage validation. The final corrective attempt retries a failed multi-page range in smaller sequential chunks within the existing worker limit, then reports an error if coverage remains invalid instead of silently importing a partial result. The temporary chunk page numbers and evidence boxes are mapped back to the original PDF before review. Tune this with `gemini.statement.pages-per-request` (1-12), `gemini.statement.parallel.max-concurrent` (1-6), and `gemini.statement.completeness-attempts` (1-4).

For dense statements such as Symbion tables with around 70 rows per page, start Alpha API with `GEMINI_STATEMENT_PAGES_PER_REQUEST=1` and `GEMINI_STATEMENT_PARALLEL_MAX_CONCURRENT=2`. Apply the scanner update, restart the API, and retry the failed statement batch; the original PDF does not need conversion or manual splitting. Invoice and credit rows with blank amounts are retained as provisional zero amounts without inventing OCR confidence. A unique Z-Office match confirming $0.00 automatically accepts the row when supplier, reference, date and type are confident. A missing/ambiguous match, uncertain identity or any non-zero Z-Office amount still needs review; the normal variance is shown. The review form also permits genuine zero-value invoices and credits. Printed total candidates carry their exact labels, amount types and page types. Alpha prioritises the whole-statement payable amount, then a statement total or closing balance, and excludes discounted payables, GST/BAS summaries, opening balances and page subtotals. Chunk order and proximity to the extracted transaction sum do not determine the chosen value. Conflicting equally ranked totals remain unavailable for review rather than silently replacing one another.

Gemini infers a statement's reporting period from an explicit printed period when available, otherwise from the statement date, and only falls back to the latest transaction month when neither is present. It returns invoices and credits whose transaction dates fall inside that period. Statement matching uses retained entered scan values across the listed references, regardless of the month displayed in the invoicing screen. When Gemini finds an explicitly printed whole-statement amount, closing balance, or amount due, the results screen compares it with the signed sum of extracted invoices and credits and displays the variance; missing totals remain clearly marked as unavailable rather than being inferred. Statement extraction uses `gemini.statement.model`, medium PDF media resolution, and the larger `gemini.statement.max-output-tokens` allowance. The API groups new invoice PDFs from one watched-folder run into a review batch and keeps each statement as its own review entry. Each Gemini upload is deleted after extraction. Gemini supplies individual confidence values for supplier, reference, date, document type, and amount; the corresponding review-table cell is highlighted red at 90% or below. Extracted supplier names are also matched against the store's saved supplier contacts; strong, unambiguous matches use the saved contact name, including ordinary initials and repeated-initial shorthand such as `CH2` for Clifford Hallam Healthcare. Explicit CH2 aliases also recognize the ParagonCare brand and the full Clifford Hallam Healthcare issuer name with appended address text such as Eastern Creek NSW. These aliases use an existing CH2 contact; exact saved-contact matches take precedence and uncertain matches remain flagged for review. An invoice without a visible due date defaults to 30 days after its invoice date; credits remain without a due date. Choosing an exception batch or statement opens its reconciliation rows; choosing Review then opens the highlighted PDF and editable invoice or credit form for invoice exceptions, or the matched invoice/credit values for statement rows. Use the preview toolbar's **Show invoice PDF** / **Show credit PDF** toggle (which changes to **Show statement PDF**) to inspect both documents without losing draft edits. Accepted invoice corrections are saved together with final statement completion. An invoice exception batch remains queued until its rows are accepted and saved, or it is explicitly dismissed. Statements are completed after their independent reconciliation.

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

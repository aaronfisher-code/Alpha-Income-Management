# Statement review loading

A statement previously sent all its invoice rows to `/invoices/reconciliation/resolve` in one HTTP request. The API read the MySQL cache, then queried the site's z-agent once per missing or mismatched reference. Symbion aliases could double those queries. Recently checked exceptions were retried on every opening. The accumulated database and network latency could exceed the gateway timeout.

## Updated path

- The desktop sends at most 50 invoice rows per HTTP request, advances a visible progress bar and completed-record count after each chunk, and merges repeated references without creating duplicate matches. The bar animates during supplier/document preparation, uses one overall total for combined batches, and disappears when loading completes or fails. Genuine ambiguous matches remain ambiguous. Bulk supplier edits also use bounded requests.
- The API reads lookup identities and amount datapoints in bulk from MySQL. A matching cached amount, including zero, requires no live query. Missing, ambiguous, and mismatched lookups are reused for 120 seconds after a successful check. A new imported amount remains visible immediately. Explicit row refresh always bypasses the retry window.
- Unresolved references and Symbion aliases use `InvoiceBatch`, at most 100 distinct references per agent request. Each batch uses one parameterized, read-only SQL query. Its full invoice totals are grouped by invoice reference and supplier without filtering by receipt date, so deliveries across multiple months are not partially summed. A single-reference retry retains the existing `Invoices` query.
- Each successful batch writes lookup results and amount datapoints using JDBC batch operations within one transaction. Missing and ambiguous results invalidate stale single amounts. Invalid or failed agent responses never write missing results.
- Requests run sequentially to avoid flooding a site's SQL Server. A failed batch stops further agent queries and triggers a 30-second per-store cooldown shared by subsequent desktop chunks. Existing cached amounts remain usable; unresolved rows remain review exceptions. Successful manual refresh clears that cooldown.
- Oversized callers are limited to ten agent batches and a 30-second budget between batches. An already running agent query retains the configured agent timeout. If work remains after the budget, the API returns a retryable error; completed lookups stay cached for the next attempt. Background prefetch defers incomplete amount enrichment without failing a successful PDF extraction.

For 500 uncached ordinary invoice rows, the desktop makes ten bounded HTTP requests and at most ten batch queries. For 500 Symbion rows needing both reference forms, it also makes at most ten batch queries, compared with up to 1,000 individual queries previously. The API regression test covering a single 500-reference request verifies five agent queries. Reopening a fully cached matching statement requires no agent queries.

## Rollout

1. Install/restart the updated `z-db-forwarder` at the stores. Include its new `queries/live-invoice-batch.sql` mapping alongside the binary.
2. Deploy the matching `Alpha-API` change. It uses the existing MySQL tables; no manual schema migration is required. Optionally configure `alpha.z-agent.invoice-retry-seconds` (default 120; 0 disables the successful-result retry delay).
3. Release the desktop update.

An older forwarder returns an actionable conflict asking for a forwarder update. The API does not fall back to hundreds of individual queries. Existing individual invoice and credit operations remain supported.

## Verification

Regression coverage includes a 500-row load, 1,100-row request limits with resumable cached progress, fresh exception reuse, store isolation, offline-agent behavior across ten desktop chunks, manual refresh recovery, zero amounts, ambiguous references, Symbion aliases, invalid agent responses, desktop chunk failures, and parameter validation/XML escaping in the forwarder. SQL mapping guard tests check the new query is read-only. SQL execution plans and real gateway timings still need validation against a store's SQL Server after deployment; local fixtures do not establish production latency.

Local verification: all 189 desktop tests pass; 145 API tests pass; all 58 forwarder tests pass. The existing `AlphaApiApplicationTests.contextLoads` test fails because its mock servlet context lacks `jakarta.websocket.server.ServerContainer`; the same failure was reproduced against the unchanged API source. Tests ran against H2 in MySQL compatibility mode and mocked agent responses, not a live store database.

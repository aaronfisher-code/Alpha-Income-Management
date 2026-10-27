# Z-Office credits

Credits are available in the main Credits list and in statement/PDF review. This uses the forwarder's existing `Credits` operation for individual/monthly reads plus a new `CreditBatch` operation for large reviews.

## Credits list

Initial loading and subsequent refreshes both use `GET /credits/reconciliation/table-data?storeId=...&yearMonth=yyyy-MM`. Alpha's saved credits remain editable. A Z-Office-only credit is read-only, with its source shown as Z-Office. When one Alpha credit uniquely matches one Z credit by reference and supplier, the list shows both Alpha's saved amount and the Z-Office amount, with source Alpha + Z. Hover over the source for the Z status. Alpha amounts, IDs and user notes are preserved. Shared references belonging to different suppliers and ambiguous duplicate credits are kept separate.

Monthly Z snapshots live in MySQL, including empty months. A fresh snapshot needs no agent call; an expired snapshot requires one monthly `Credits` query. An agent failure retains the previous snapshot and imposes a 30-second per-store cooldown. When no snapshot is available, only the saved Alpha credits can be displayed during an outage. Opening the list never inserts source records into Alpha's accepted `credits` table.

## Statement and PDF review

Credit references are loaded alongside invoices in chunks of at most 50 document rows per desktop request. The API groups up to 100 credit references into one `CreditBatch` query. Single-credit lookups continue to use `Credits`. The mixed-document routes are `/invoices/reconciliation/documents/cached` and `/invoices/reconciliation/documents/resolve`; the existing invoice-only endpoints retain their behavior for older desktops.

Credit lookups have their own MySQL cache, separate from invoice identities and invoice datapoints. Date-bounded monthly snapshots do not populate the exact-reference cache: a credit with the same number could exist in another month and must remain an ambiguous match. They preserve missing and ambiguous results, never convert invalid amounts into zero, and preserve exact credit references (including punctuation and leading zeroes). Invoice and credit matches are indexed separately, so an invoice numbered 123 cannot match a credit numbered 123. Supplier matching is required for a named scanned credit. A valid zero credit remains a real match.

Expected credit values are negative in the reconciliation table. The review form displays the positive credit magnitude and calculates variance using the credit sign. Changing the form's document type hides an incompatible expected amount until a matching source lookup is available. The refresh button works for invoices and credits; `POST /credits/reconciliation/refresh` bypasses the credit cache for an explicit refresh. Draft edits survive failures. The linked source invoice reference is retained when an accepted credit with the matched reference is saved to Alpha. Saving reviewed credits continues to use the existing Alpha save workflow and never writes to Z-Office.

The Drive worker prefetches credit amounts after successful extraction. Missing batch support or an enrichment time limit does not mark a successfully extracted PDF as failed. Very large API callers are capped at ten credit batches and a 30-second budget between queries; completed progress is cached for a retry. Agent failures stop further credit attempts across subsequent desktop chunks.

## Source query and freshness

`Credits` and `CreditBatch` include Saved, Paid and Returned credits (`A`, `P`, `F`); Deleted (`D`) credits are omitted. Separate Z credit headers stay separate even when reference, date, supplier and status match. The batch operation uses a bound XML parameter containing 1–100 printable references, each at most 128 characters, and a fixed read-only SQL mapping. It has no date filter, so statement dates cannot exclude the matching credit. Monthly reads remain date-bounded.

`alpha.z-agent.credit-cache-seconds` defaults to 120 (0–3600 allowed). Unlike stable matching invoice amounts, credit lookups expire after this interval because credits can change status or be deleted. Explicit refresh bypasses that delay and invalidates monthly snapshots for the next list load. Cached values remain available during an outage. Expired credit values are marked stale and require review instead of automatically accepting a matching amount; the PDF form prompts for refresh. Missing data during an outage does not establish that Z-Office currently has no matching credit.

## Rollout

1. Update each store's z-db-forwarder binary and ship `queries/live-credit-batch.sql` plus the updated `queries/live-credits.sql`.
2. Deploy Alpha API. Its startup creates `credit_lookup_cache` and `credit_month_cache`; no changes to the Z database or manual SQL migration are required.
3. Release the desktop update. API-side agent configuration controls availability; the new credit display does not require the desktop's legacy live-Z flag.

An older API falls back to saved Alpha credits for the main list. Mixed statement credit lookup and manual credit refresh require the new API; an older API returns an error instead of silently treating an unsupported lookup as a missing credit. An older forwarder receives an actionable update-required error for credit batches. Individual/monthly credits remain compatible with the existing operation.

## Verification

Coverage includes 500-credit batching, cache reuse and expiry, store isolation, genuine zero values, deleted/missing credits, malformed responses, duplicate references, invoice/credit number collisions, different suppliers, monthly source merging without overwriting Alpha amounts, refresh failure recovery, retry limits, manual PDF refresh for both types, original invoice linkage, and keeping source-only credits out of the Alpha save-duplicate lookup. Forwarder tests cover bounded credit batches, XML escaping, read-only mappings and preservation of distinct credit headers.

Tests use H2 in MySQL compatibility mode and mocked agent responses. Production SQL execution plans, source status meanings and response latency still need confirmation against a store's Z-Office database after deployment. The pre-existing API `AlphaApiApplicationTests.contextLoads` WebSocket mock-container failure is excluded from the broader API verification run.

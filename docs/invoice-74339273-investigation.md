# Invoice 74339273 lookup investigation

The screenshots show CH2 invoice `74339273 RI`, dated 20 October 2026, with PDF total $321.29. Z-Office shows receipt invoice `74339273` on order `57750`, supplier `ParagonCare CH2`, and total $321.30. The one-cent difference is within Alpha's existing 20-cent tolerance. Local API and desktop regression fixtures confirm the row is accepted when returned by the forwarder.

## Confirmed cause

The user ran [the focused read-only SQL](diagnostics/invoice-74339273.sql) against APSS and supplied all four result sets. They establish:

- The parent order status is `Receiving (Saved)`, with a null order-level received date and invoice reference.
- Three receipt lines have the exact reference `74339273`, with quantities/costs `1 × 61.26`, `2 × 110.42` and `3 × 3.33`, all flagged GST. The invoice-level total including GST is $321.30.
- The same order also contains invoices `74339296` and `74335297`; the whole order must not be totalled as one invoice.
- The exact live query returned no rows because its `o.Status = 'Received'` predicate excluded this order. Supplier identity, suffix matching and the amount difference were not the cause.

## Correction

The Z database forwarder's exact single and batch invoice queries now include both `Received` and `Receiving (Saved)` orders, while grouping by receipt invoice and supplier. Window totals include saved receiving lines for references discovered in their existing bounded completed-order window. References absent from that window are resolved by the existing exact lookup fallback. Monthly range reads and scheduled invoice exports retain their completed-order rules.

Deploy the three updated live invoice SQL files to the forwarder on the Z-Office server. Another API or desktop rebuild is not required for this correction. The prepared patch includes backup/restore deployment instructions and a read-only verification query. After updating, click **Refresh Z-Office amount** on this invoice; the existing matching logic accepts its one-cent variance. No rescan or cache-table deletion is needed.

Regression fixtures execute the actual shipped SQL joins, status predicates and aggregation with the supplied receipt values. They preserve invoice separation and test exclusion of unrelated order states. External SQL Server execution and the final deployed result have not been run from this workspace.

## Follow-up: explicit null date parsing

After saved receiving orders became visible, the API log exposed `DateTimeParseException: Text 'null' could not be parsed` at `InvoiceAmountCacheService.imported`. The forwarder correctly serialises the parent order's missing received date as JSON `null`; Jackson's no-default `asText()` converted that node to the literal text `null`, which the API then passed to `LocalDate.parse`.

Both the reconciliation importer and ordinary single-invoice importer now read this optional field using `asText("")`, preserving null/missing/blank dates as an absent Java date. Explicit-null HTTP regressions cover refresh, batch resolution and window loading; all return the amount and retain automatic acceptance. The scanned invoice date remains the saved document date.

The request logger previously printed its default response status `200` from `finally` before the container processed the exception. It now logs an exceptional request as failed rather than finished successfully. The parser correction requires rebuilding/redeploying **Alpha API**. Keep the corrected forwarder SQL; no additional desktop change is needed for this follow-up.

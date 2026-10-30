# 27 October 2026 till discrepancy

The two supplied SQL result sets establish the calculation mismatch: Alpha's
takings are **$50.00 higher** than the Z report because an active, entirely
account-charged receipt contains a $50 agency item whose debtor invoice has
status `D`. The current query counts the sale but excludes that debtor link
from the deduction. The receipt has no cash/card payment in the supplied
results. The cause of the reported $50 variance is now isolated; the business
action and timing behind the status change are not shown by these queries.

The comparison assumes image 2 is for the same store and completed till
period as the 27 October row in image 1; its date/header is outside the photo.

| Payment | Alpha entered | Z report | Entered minus report |
| --- | ---: | ---: | ---: |
| Cash | $200.00 | $200.90 | −$0.90 |
| Cards (EFTPOS + AMEX) | $4,408.44 + $96.04 = $4,504.48 | $4,504.48 | $0.00 |
| Total | $4,704.48 | $4,705.38 | **−$0.90** |

The displayed daily variance is −$50.90. The desktop calculates entered cash
+ EFTPOS + AMEX + Square + cheque − total takings, so the implied takings input
is $4,704.48 − (−$50.90) = **$4,755.38**. The supplied SQL output now confirms
this reconstructed value. Floating-point rounding cannot explain $50.

The running balance is internally consistent: $1.71 on 26 October minus $50.90
equals −$49.19 on 27 October. If this is the only error and takings should be
$4,705.38, the corrected 27 October running balance would be $0.81, and the
29 October balance would be −$0.05 instead of −$50.05.

## Where the input comes from

The desktop's `EODDataPoint.calculateTillBalances` subtracts the total provided
by `TillBalanceCalculator`, which sums matching `Total Takings` rows.
`EODDataEntryPageController.fillTable` requests a live monthly refresh and
falls back to stored data when live Z is unavailable. Opening an individual
day requests a fresh single-day total. Alpha API chooses forwarder
`TillReport` for a monthly request and `TillTakings` for a single-day takings
request, then replaces the stored metric window with the response.

Before the 0.2.1 correction, both forwarder SQL queries reconstructed total
takings as:

```text
active RX/N/A/D/DP receipt-item subtotals
− active receipt-item subtotals linked to active I/S debtor transactions
+ payment rounding on active receipts
```

The Z report provides these comparison targets:

| Component | Report amount |
| --- | ---: |
| Total till turnover | $15,270.58 |
| Government contributions | $9,811.70 |
| Sale subtotal implied by turnover minus government | $5,458.88 |
| Plus debtor account payments | $112.18 |
| Less debtor charges | $865.74 |
| Plus rounding | $0.06 |
| Total takings | **$4,705.38** |

The other additions/deductions visible in the photo are zero. The arithmetic
is $5,458.88 + $112.18 − $865.74 + $0.06 = $4,705.38.

## Findings from the supplied SQL results

The resolved business period is 2585: **26 October 18:58:13.947 to
27 October 18:35:31.680**. There is one resolved period for the day. The query
reproduces the discrepancy from source data, so stale Alpha data alone cannot
explain it.

| Component | Current reconstruction | Z report | Reconstruction minus report |
| --- | ---: | ---: | ---: |
| Sale subtotal | $5,422.69 | $5,458.88 | −$36.19 |
| Debtor account payments | $112.18 | $112.18 | $0.00 |
| Debtor charges deducted | $779.55 | $865.74 | −$86.19 |
| Rounding | $0.06 | $0.06 | $0.00 |
| Total takings | **$4,755.38** | **$4,705.38** | **+$50.00** |

The net difference is −$36.19 − (−$86.19) = **+$50.00**.

Result 7 contains the key evidence:

```text
itemType=A, debtorTransactionStatus=D, debtorTransactionType=I,
linkCount=1, linkedItemSubtotal=50.00, debtorTransactionAmount=50.00
```

These links were selected through active receipt items. Result 6 also shows
that all three active agency items have active parent receipts, totalling
$95.10, while result 5 deducts just two agency items totalling $45.10. The
remaining $50 item is counted in sales but its status-D debtor invoice is
excluded by `dt.Status = 'A'` in both forwarder queries. The active-receipt
ACCOUNT CHARGE payments total **$829.55**, exactly $50 above the query's
$779.55 debtor deduction. Together these results isolate the status mismatch;
they do not show whether that charge was reversed, deleted, reallocated or
otherwise changed by staff or Z.

The separate **$36.19** is an RX item and parent receipt with status `C`
(result 6). The follow-up confirms its linked debtor record has type `S`,
status `D` and amount $36.19; the parent receipt's only payment is an ACCOUNT
CHARGE of $36.19. This accounts for the matching $36.19 difference in report
sales and debtor charges and has no net effect on takings.

Deducting the additional $50 account charge from the current reconstruction
would produce $4,705.38 and restore the visible −$0.90 daily variance. This
is the reconciled target, not authorization to blindly include every deleted
debtor transaction. An obsolete link on a sale later paid by cash/card must
not create a false account deduction. Both `till-report.sql` and
`till-takings.sql` require the same verified rule. The 0.2.1 implementation
described below applies that rule to both queries.

## Confirmed receipt-level evidence

The user executed the [focused follow-up query](diagnostics/till-2026-10-27-debtor-links.sql)
and supplied all three result grids:

| Receipt time, 27 October | Receipt/item state | Items | Recorded payments | Relevant debtor link |
| --- | --- | --- | --- | --- |
| 14:34:36.800 | Active receipt and items | Agency $50 + two RX items totalling $50 | One ACCOUNT CHARGE of $100 | Agency invoice: type I, status D, $50; no active I/S link |
| 15:30:50.700 | Receipt and item status C | One RX item of $36.19 | One ACCOUNT CHARGE of $36.19 | RX charge: type S, status D, $36.19; no active I/S link |

The first receipt is entirely account-charged: its payment equals the $100
item total. There is no split tender to allocate and no cash/card payment in
the supplied output. Nevertheless, the agency portion contributes $50 to
Alpha's reconstructed cash/card takings because its charge link fails
`dt.Status = 'A'`. The two RX items are outside the follow-up's affected-item
filter; their individual debtor links were not selected, so the follow-up
does not independently describe those links.

The correction belongs in the forwarder's debtor-charge reconstruction for
both full and lightweight till queries. A fully account-charged active
receipt must retain its account-charge treatment when the linked debtor
record is inactive. Before deploying a general rule, compare other completed
periods and cover fully account-charged receipts, genuine cash/card receipts,
split tender and status-C receipts. The investigated receipt provides enough
evidence to explain this incident; further queries are not needed to locate
the reported $50.

## Implemented correction and deployment

The adjacent `z-db-forwarder` repository now contains the fix as version
**0.2.1**. Both till queries retain the existing active-charge deduction and
also deduct a status-D I/S-linked active sale/refund item when its active
parent receipt is paid exclusively by ACCOUNT CHARGE and those payments
exactly cover the complete active receipt-item subtotal. Unknown payment
methods, missing payments, partial account charges and mixed tender do not
qualify for this exception. Duplicate links cannot multiply the deduction.

The forwarder needs to be upgraded with both changed query files and the
updated `deployment/create-readonly-service-account.sql`, which adds SELECT
on `dbo.PaymentMethods` for the service user. Its normal installer applies
that grant. API and desktop builds do not change; refresh the stored October
till figures through the existing EOD page after the forwarder upgrade.
The forwarder repository's `docs/till-account-charge-fix.md` contains full
package and SQL-only deployment instructions, expected values and rollback.

All 70 .NET forwarder tests and 16 SQL fixture tests pass. The fixtures
execute each shipped debtor CTE, reproduce the incident, and verify mixed
tender, cancelled rows and duplicate-link handling. The incident test also
fails against the original query, demonstrating that it detects this error.
The SQL fixtures use SQLite and integer cents; SQL Server permissions,
performance and real-store results remain deployment verification steps.
The source changes have not been published or deployed.

## Capture the evidence

First confirm the full Z report's store, date and period. Preserve Alpha's
current stored rows before refreshing, because refresh replaces them. The
following MySQL queries only read source tables. Set the affected store ID
instead of the example `1`:

```sql
SET @storeId = 1;

SELECT `date`, storeID, cash, eftpos, amex, googleSquare, cheque,
       ROUND(cash + eftpos + amex + googleSquare + cheque, 2) AS enteredTotal
FROM eoddatapoints
WHERE storeID = @storeId AND `date` BETWEEN '2026-10-26' AND '2026-10-28'
ORDER BY `date`;

SELECT storeID, assignedDate, periodStartDate, periodEndDate, `key`, quantity, amount
FROM tillreportdatapoints
WHERE storeID = @storeId AND assignedDate BETWEEN '2026-10-26' AND '2026-10-28'
  AND `key` IN ('Total Takings', 'Total Till Turnover',
                'Total Government Contribution', 'Plus Debtor Account Payments',
                'Less Debtor Charges', 'Plus Total Rounding')
ORDER BY assignedDate, `key`;
```

The existing authenticated `GET /z-data/till-report?storeId=...&startDate=2026-10-27&endDate=2026-10-27`
can compare the forwarder's current full report against the saved snapshot
without calling the metric persistence/refresh endpoint. That route uses the
broker's short request cache. Retain the response timestamp and deployed
forwarder version when comparing it with the desktop's fresh single-day
refresh, which uses the lightweight query.

Run [the SQL Server diagnostic](diagnostics/till-2026-10-27.sql) in SSMS
against the affected store's APSS database. It writes only session-local
temporary/table variables and returns aggregate figures with no patient,
debtor, receipt, prescription or stock identifiers. Save all seven result
grids. It covers 26–28 October and uses recorded close/successor timestamps;
it deliberately does not guess roster fallback boundaries.

Result 3 compares the reconstructed total and its four components to the
photo. Its difference columns satisfy:

```text
takings difference = sale difference + debtor payment difference
                    − debtor charge difference + rounding difference
```

Results 1–2 expose duplicate or unexpected periods. Results 4–7 expose payment
codes, debtor classification gaps, excluded item types and parent-receipt
status differences. The payment-method amounts are gross tender, potentially
including change; they must not replace net takings just because one total
looks close. Likewise, a DebtorID-only deduction is a diagnostic comparison,
not a proposed fix.

If the live/source reconstruction is $4,705.38 but the saved snapshot is
$4,755.38, investigate stale cache, deployment versions, imports and period
assignment. If the reconstruction is $4,755.38, locate the differing component
and inspect the corresponding source records locally. A $50 difference in one
grid is a lead; confirm its ownership and report treatment before changing
the query. If monthly and single-day results differ, compare both query paths
and their exact boundaries.

## Validation and current limit

The existing `TillBalanceCalculatorTest` suite passes (3 tests). The supplied
amounts were also checked directly against the compiled desktop calculator:
$4,705.38 produces −$0.90, while $4,755.38 reproduces −$50.90.
The user executed both SQL diagnostics against APSS and supplied all seven
initial result grids and all three follow-up grids. The findings above are
based on those results. Decimal arithmetic checks reproduce $4,755.38 using
the current deduction and $4,705.38 with the additional $50 account-charge
deduction. The local desktop configuration points to localhost; no direct
production connection was used. The forwarder source changes are described
above; no business records have been changed.

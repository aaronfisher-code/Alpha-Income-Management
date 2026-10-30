/*
  Read-only investigation of the 27 October 2026 $50 takings discrepancy.
  Adapted from z-db-forwarder/database/diagnose-total-takings.sql.
  Only session-local temporary/table variables are written; source tables are read.
  Uses actual close/successor boundaries, without inventing roster fallback times.
  Confirm both screenshots cover this store and the same completed till period.
  ReceiptPayments.Amount is gross tender: payment totals are diagnostic only.

  Run this entire script in SSMS against APSS and export each result grid as
  CSV. It returns aggregate values only: no patient, debtor, prescription,
  receipt, or stock identifiers are selected.
*/
USE [APSS];
SET NOCOUNT ON;

DECLARE @FromDate date = '20261026';
DECLARE @ToDateExclusive date = '20261029';

/* Add other old-report Total Takings values here when available. */
DECLARE @Expected TABLE (
    assignedDate date PRIMARY KEY,
    expectedTotalTakings decimal(19, 4) NOT NULL
);

INSERT INTO @Expected (assignedDate, expectedTotalTakings)
VALUES ('20261027', 4705.38);

/*
  Result 1: raw till-period rows. This exposes duplicate starts, NULL close
  dates, and the exact successor time that the forwarder treats as the end of
  an historical open period.
*/
SELECT
    N'01 raw till periods' AS resultSet,
    tp.ID AS tillPeriodId,
    tp.StartDate AS periodStart,
    tp.ClosedDate AS recordedClosedDate,
    successor.nextStart,
    COALESCE(tp.ClosedDate, successor.nextStart) AS effectiveEnd,
    CAST(COALESCE(tp.ClosedDate, successor.nextStart) AS date) AS assignedDate
FROM dbo.TillPeriods AS tp
OUTER APPLY (
    SELECT MIN(nextPeriod.StartDate) AS nextStart
    FROM dbo.TillPeriods AS nextPeriod
    WHERE nextPeriod.StartDate > tp.StartDate
) AS successor
WHERE tp.StartDate >= DATEADD(day, -7, CAST(@FromDate AS datetime))
  AND tp.StartDate < CAST(@ToDateExclusive AS datetime)
ORDER BY tp.StartDate, tp.ID;

IF OBJECT_ID('tempdb..#PeriodWindows') IS NOT NULL
    DROP TABLE #PeriodWindows;

/* Match the forwarder's canonical choice for duplicate open period starts. */
;WITH SourcePeriods AS (
    SELECT
        tp.ID AS tillPeriodId,
        tp.StartDate AS periodStart,
        tp.ClosedDate AS recordedClosedDate
    FROM dbo.TillPeriods AS tp
    WHERE tp.ClosedDate IS NOT NULL
      AND tp.StartDate < CAST(@ToDateExclusive AS datetime)
      AND tp.ClosedDate >= CAST(@FromDate AS datetime)

    UNION ALL

    SELECT
        tp.ID,
        tp.StartDate,
        tp.ClosedDate
    FROM dbo.TillPeriods AS tp
    INNER JOIN (
        SELECT StartDate, MAX(ID) AS latestOpenPeriodId
        FROM dbo.TillPeriods
        WHERE ClosedDate IS NULL
          AND StartDate < CAST(@ToDateExclusive AS datetime)
          AND StartDate >= DATEADD(day, -7, CAST(@FromDate AS datetime))
        GROUP BY StartDate
    ) AS latestOpen
        ON latestOpen.latestOpenPeriodId = tp.ID
),
ResolvedPeriods AS (
    SELECT
        source.tillPeriodId,
        source.periodStart,
        source.recordedClosedDate,
        successor.nextStart,
        COALESCE(source.recordedClosedDate, successor.nextStart) AS periodEnd
    FROM SourcePeriods AS source
    OUTER APPLY (
        SELECT MIN(nextPeriod.StartDate) AS nextStart
        FROM dbo.TillPeriods AS nextPeriod
        WHERE nextPeriod.StartDate > source.periodStart
    ) AS successor
)
SELECT
    tillPeriodId,
    periodStart,
    recordedClosedDate,
    nextStart,
    periodEnd,
    CAST(periodEnd AS date) AS assignedDate
INTO #PeriodWindows
FROM ResolvedPeriods
WHERE periodEnd IS NOT NULL
  AND periodEnd > periodStart
  AND periodStart < CAST(@ToDateExclusive AS datetime)
  AND periodEnd >= CAST(@FromDate AS datetime);

/* Result 2: period windows actually used by the aggregate diagnostics. */
SELECT
    N'02 resolved period windows' AS resultSet,
    tillPeriodId,
    periodStart,
    periodEnd,
    assignedDate,
    DATEDIFF(minute, periodStart, periodEnd) AS periodMinutes
FROM #PeriodWindows
ORDER BY periodStart, tillPeriodId;

/*
  Result 3: daily comparison of the two competing Total Takings methods.

  itemFormulaTotal is the original report reconstruction:
      RX/N/A/D/DP subtotals - linked debtor item subtotals + rounding

  nonAccountPaymentTotal is a gross-tender diagnostic comparison only.
  The difference columns identify which component diverges from the supplied
  report without exposing transaction-level data.
*/
;WITH PerPeriod AS (
    SELECT
        period.assignedDate,
        period.tillPeriodId,
        COALESCE(items.saleItemSubtotal, 0) AS saleItemSubtotal,
        COALESCE(items.debtorPaymentSubtotal, 0) AS debtorPaymentSubtotal,
        COALESCE(items.takingsItemSubtotal, 0) AS takingsItemSubtotal,
        COALESCE(linkedDebtors.debtorLinkedItemSubtotal, 0) AS debtorLinkedItemSubtotal,
        COALESCE(debtorIds.debtorIdItemSubtotal, 0) AS debtorIdItemSubtotal,
        COALESCE(payments.roundingAmount, 0) AS roundingAmount,
        COALESCE(payments.allPaymentAmount, 0) AS allPaymentAmount,
        COALESCE(payments.accountCodePaymentAmount, 0) AS accountCodePaymentAmount,
        COALESCE(payments.nonAccountPaymentAmount, 0) AS nonAccountPaymentAmount
    FROM #PeriodWindows AS period
    OUTER APPLY (
        SELECT
            SUM(CASE WHEN ri.Type IN ('RX', 'N', 'A', 'D')
                THEN ri.Subtotal ELSE 0 END) AS saleItemSubtotal,
            SUM(CASE WHEN ri.Type = 'DP'
                THEN ri.Subtotal ELSE 0 END) AS debtorPaymentSubtotal,
            SUM(CASE WHEN ri.Type IN ('RX', 'N', 'A', 'D', 'DP')
                THEN ri.Subtotal ELSE 0 END) AS takingsItemSubtotal
        FROM dbo.ReceiptItems AS ri
        WHERE ri.Date >= period.periodStart
          AND ri.Date <= period.periodEnd
          AND (ri.Date < period.periodEnd
               OR period.periodEnd = period.recordedClosedDate)
          AND ri.Status = 'A'
    ) AS items
    OUTER APPLY (
        SELECT SUM(ri.Subtotal) AS debtorLinkedItemSubtotal
        FROM dbo.ReceiptItems AS ri
        WHERE ri.Date >= period.periodStart
          AND ri.Date <= period.periodEnd
          AND (ri.Date < period.periodEnd
               OR period.periodEnd = period.recordedClosedDate)
          AND ri.Status = 'A'
          AND EXISTS (
              SELECT 1
              FROM dbo.DebtorTransactions AS dt
              WHERE dt.ReceiptItemID = ri.ID
                AND dt.Status = 'A'
                AND dt.Type IN ('I', 'S')
          )
    ) AS linkedDebtors
    OUTER APPLY (
        SELECT SUM(ri.Subtotal) AS debtorIdItemSubtotal
        FROM dbo.ReceiptItems AS ri
        WHERE ri.Date >= period.periodStart
          AND ri.Date <= period.periodEnd
          AND (ri.Date < period.periodEnd
               OR period.periodEnd = period.recordedClosedDate)
          AND ri.Status = 'A'
          AND ri.Type IN ('RX', 'N', 'A', 'D')
          AND ri.DebtorID IS NOT NULL
    ) AS debtorIds
    OUTER APPLY (
        SELECT
            SUM(rp.RoundingAmount) AS roundingAmount,
            SUM(rp.Amount) AS allPaymentAmount,
            SUM(CASE WHEN pm.Code = 'AC' THEN rp.Amount ELSE 0 END)
                AS accountCodePaymentAmount,
            SUM(CASE WHEN COALESCE(pm.Code, '') <> 'AC' THEN rp.Amount ELSE 0 END)
                AS nonAccountPaymentAmount
        FROM dbo.Receipts AS r
        INNER JOIN dbo.ReceiptPayments AS rp
            ON rp.ReceiptID = r.ID
        LEFT JOIN dbo.PaymentMethods AS pm
            ON pm.ID = rp.PaymentMethodID
        WHERE r.Date >= period.periodStart
          AND r.Date <= period.periodEnd
          AND (r.Date < period.periodEnd
               OR period.periodEnd = period.recordedClosedDate)
          AND r.Status = 'A'
    ) AS payments
),
Daily AS (
    SELECT
        assignedDate,
        SUM(saleItemSubtotal) AS saleItemSubtotal,
        SUM(debtorPaymentSubtotal) AS debtorPaymentSubtotal,
        SUM(takingsItemSubtotal) AS takingsItemSubtotal,
        SUM(debtorLinkedItemSubtotal) AS debtorLinkedItemSubtotal,
        SUM(debtorIdItemSubtotal) AS debtorIdItemSubtotal,
        SUM(roundingAmount) AS roundingAmount,
        SUM(allPaymentAmount) AS allPaymentAmount,
        SUM(accountCodePaymentAmount) AS accountCodePaymentAmount,
        SUM(nonAccountPaymentAmount) AS nonAccountPaymentAmount
    FROM PerPeriod
    GROUP BY assignedDate
)
SELECT
    N'03 daily formula comparison' AS resultSet,
    daily.assignedDate,
    CAST(daily.saleItemSubtotal AS decimal(19, 4)) AS saleItemSubtotal,
    CAST(daily.debtorPaymentSubtotal AS decimal(19, 4)) AS debtorPaymentSubtotal,
    CAST(daily.debtorLinkedItemSubtotal AS decimal(19, 4)) AS linkedDebtorDeduction,
    CAST(daily.debtorIdItemSubtotal AS decimal(19, 4)) AS debtorIdDeductionCandidate,
    CAST(daily.roundingAmount AS decimal(19, 4)) AS roundingAmount,
    CAST(ROUND(
        daily.takingsItemSubtotal
        - daily.debtorLinkedItemSubtotal
        + daily.roundingAmount, 2) AS decimal(19, 4)) AS itemFormulaTotal,
    CAST(daily.allPaymentAmount AS decimal(19, 4)) AS allPaymentTotal,
    CAST(daily.accountCodePaymentAmount AS decimal(19, 4)) AS accountCodePaymentTotal,
    CAST(daily.nonAccountPaymentAmount AS decimal(19, 4)) AS nonAccountPaymentTotal,
    expected.expectedTotalTakings,
    CASE WHEN daily.assignedDate = '20261027'
        THEN CAST(daily.saleItemSubtotal - 5458.88 AS decimal(19, 4))
        END AS saleSubtotalDifferenceFromReport,
    CASE WHEN daily.assignedDate = '20261027'
        THEN CAST(daily.debtorPaymentSubtotal - 112.18 AS decimal(19, 4))
        END AS debtorPaymentsDifferenceFromReport,
    CASE WHEN daily.assignedDate = '20261027'
        THEN CAST(daily.debtorLinkedItemSubtotal - 865.74 AS decimal(19, 4))
        END AS debtorChargesDifferenceFromReport,
    CASE WHEN daily.assignedDate = '20261027'
        THEN CAST(daily.roundingAmount - 0.06 AS decimal(19, 4))
        END AS roundingDifferenceFromReport,
    CAST(ROUND(
        daily.takingsItemSubtotal
        - daily.debtorLinkedItemSubtotal
        + daily.roundingAmount
        - expected.expectedTotalTakings, 2) AS decimal(19, 4)) AS itemFormulaDifference,
    CAST(ROUND(
        daily.nonAccountPaymentAmount
        - expected.expectedTotalTakings, 2) AS decimal(19, 4)) AS paymentFormulaDifference
FROM Daily AS daily
LEFT JOIN @Expected AS expected
    ON expected.assignedDate = daily.assignedDate
WHERE daily.assignedDate >= @FromDate
  AND daily.assignedDate < @ToDateExclusive
ORDER BY daily.assignedDate
OPTION (RECOMPILE);

/*
  Result 4: payment-method breakdown. This will reveal the exact payment code
  responsible if an account or non-takings method is currently being counted.
*/
SELECT
    N'04 payment method breakdown' AS resultSet,
    period.assignedDate,
    COALESCE(pm.Code, N'<NULL>') AS paymentCode,
    COALESCE(pm.Name, N'<NULL>') AS paymentName,
    COUNT_BIG(*) AS paymentRowCount,
    COUNT_BIG(DISTINCT r.ID) AS receiptCount,
    CAST(SUM(rp.Amount) AS decimal(19, 4)) AS paymentAmount,
    CAST(SUM(rp.SurchargeAmount) AS decimal(19, 4)) AS surchargeAmount,
    CAST(SUM(rp.RoundingAmount) AS decimal(19, 4)) AS roundingAmount
FROM #PeriodWindows AS period
INNER JOIN dbo.Receipts AS r
    ON r.Date >= period.periodStart
   AND r.Date <= period.periodEnd
   AND (r.Date < period.periodEnd
        OR period.periodEnd = period.recordedClosedDate)
   AND r.Status = 'A'
INNER JOIN dbo.ReceiptPayments AS rp
    ON rp.ReceiptID = r.ID
LEFT JOIN dbo.PaymentMethods AS pm
    ON pm.ID = rp.PaymentMethodID
WHERE period.assignedDate >= @FromDate
  AND period.assignedDate < @ToDateExclusive
GROUP BY period.assignedDate, pm.Code, pm.Name
ORDER BY period.assignedDate, paymentAmount DESC, paymentCode
OPTION (RECOMPILE);

/*
  Result 5: debtor-classification gaps. A non-zero row where hasDebtorId = 1
  and hasActiveISLink = 0 is a candidate for the missing debtor deduction.
*/
;WITH DebtorClassification AS (
    SELECT
        period.assignedDate,
        ri.Type AS itemType,
        CASE WHEN ri.DebtorID IS NULL THEN 0 ELSE 1 END AS hasDebtorId,
        CASE WHEN EXISTS (
            SELECT 1
            FROM dbo.DebtorTransactions AS dt
            WHERE dt.ReceiptItemID = ri.ID
              AND dt.Status = 'A'
              AND dt.Type IN ('I', 'S')
        ) THEN 1 ELSE 0 END AS hasActiveISLink,
        ri.Subtotal
    FROM #PeriodWindows AS period
    INNER JOIN dbo.ReceiptItems AS ri
        ON ri.Date >= period.periodStart
       AND ri.Date <= period.periodEnd
       AND (ri.Date < period.periodEnd
            OR period.periodEnd = period.recordedClosedDate)
       AND ri.Status = 'A'
    WHERE period.assignedDate >= @FromDate
      AND period.assignedDate < @ToDateExclusive
      AND ri.Type IN ('RX', 'N', 'A', 'D')
)
SELECT
    N'05 debtor classification' AS resultSet,
    assignedDate,
    itemType,
    hasDebtorId,
    hasActiveISLink,
    COUNT_BIG(*) AS itemCount,
    CAST(SUM(Subtotal) AS decimal(19, 4)) AS itemSubtotal
FROM DebtorClassification
WHERE hasDebtorId = 1 OR hasActiveISLink = 1
GROUP BY assignedDate, itemType, hasDebtorId, hasActiveISLink
ORDER BY assignedDate, hasDebtorId DESC, hasActiveISLink DESC, itemType
OPTION (RECOMPILE);

/* Result 6: detect omitted types or active items under non-active receipts.
   Production filters item status independently from parent receipt status.
   Any difference here is evidence to investigate, not a proven filter change. */
SELECT
    N'06 item and parent receipt classification' AS resultSet,
    period.assignedDate,
    ri.Status AS itemStatus,
    ri.Type AS itemType,
    r.Status AS parentReceiptStatus,
    COUNT_BIG(*) AS itemCount,
    CAST(SUM(ri.Subtotal) AS decimal(19, 4)) AS itemSubtotal,
    CAST(SUM(ri.GovtContribution) AS decimal(19, 4)) AS governmentContribution
FROM #PeriodWindows AS period
INNER JOIN dbo.ReceiptItems AS ri
    ON ri.Date >= period.periodStart
   AND ri.Date <= period.periodEnd
   AND (ri.Date < period.periodEnd
        OR period.periodEnd = period.recordedClosedDate)
LEFT JOIN dbo.Receipts AS r ON r.ID = ri.ReceiptID
WHERE period.assignedDate = '20261027'
GROUP BY period.assignedDate, ri.Status, ri.Type, r.Status
ORDER BY ri.Status, ri.Type, r.Status;

/* Result 7: classify all debtor link states for active items.
   Do not period-filter DebtorTransactions.Date: item ownership controls this.
   Multiple debtor links can repeat the same item in this result only;
   result 3 uses EXISTS and does not multiply the deduction. */
SELECT
    N'07 linked debtor transaction classification' AS resultSet,
    ri.Type AS itemType,
    dt.Status AS debtorTransactionStatus,
    dt.Type AS debtorTransactionType,
    COUNT_BIG(*) AS linkCount,
    CAST(SUM(ri.Subtotal) AS decimal(19, 4)) AS linkedItemSubtotal,
    CAST(SUM(dt.Amount) AS decimal(19, 4)) AS debtorTransactionAmount
FROM #PeriodWindows AS period
INNER JOIN dbo.ReceiptItems AS ri
    ON ri.Date >= period.periodStart
   AND ri.Date <= period.periodEnd
   AND (ri.Date < period.periodEnd
        OR period.periodEnd = period.recordedClosedDate)
   AND ri.Status = 'A'
LEFT JOIN dbo.DebtorTransactions AS dt ON dt.ReceiptItemID = ri.ID
WHERE period.assignedDate = '20261027'
GROUP BY ri.Type, dt.Status, dt.Type
ORDER BY ri.Type, dt.Status, dt.Type;

IF NOT EXISTS (SELECT 1 FROM #PeriodWindows WHERE assignedDate = '20261027')
    SELECT N'No source close/successor resolves to 27 October. Inspect result 1 and Alpha roster fallback metadata; do not treat missing data as zero.' AS diagnosticWarning;

DROP TABLE #PeriodWindows;

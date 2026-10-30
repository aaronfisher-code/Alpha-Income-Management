/*
  Read-only follow-up for the supplied 27 October diagnostic results.
  Run against the affected store's APSS database in SSMS.
  Uses the exact confirmed period 2585 boundaries, with an exclusive successor.
  No source tables are changed. No patient, debtor, receipt or item IDs,
  free text, or card details are selected.
*/
USE [APSS];
SET NOCOUNT ON;

DECLARE @PeriodStart datetime = '2026-10-26T18:58:13.947';
DECLARE @PeriodEnd datetime = '2026-10-27T18:35:31.680';

/* Result 1: all debtor links for the affected active agency item and the
   status-C RX item. Do not date-filter debtor transactions: the item owns
   the business-period assignment. Each row is one link, not one item;
   multiple links may repeat an item's subtotal. */
SELECT
    N'01 affected item links' AS resultSet,
    ri.Date AS itemDate,
    ri.Status AS itemStatus,
    ri.Type AS itemType,
    ri.Subtotal AS itemSubtotal,
    r.Status AS parentReceiptStatus,
    dt.Status AS debtorTransactionStatus,
    dt.Type AS debtorTransactionType,
    dt.Amount AS debtorTransactionAmount,
    CASE WHEN EXISTS (
        SELECT 1 FROM dbo.DebtorTransactions AS activeLink
        WHERE activeLink.ReceiptItemID = ri.ID
          AND activeLink.Status = 'A'
          AND activeLink.Type IN ('I', 'S')
    ) THEN 1 ELSE 0 END AS hasActiveChargeLink
FROM dbo.ReceiptItems AS ri
LEFT JOIN dbo.Receipts AS r ON r.ID = ri.ReceiptID
LEFT JOIN dbo.DebtorTransactions AS dt ON dt.ReceiptItemID = ri.ID
WHERE ri.Date >= @PeriodStart AND ri.Date < @PeriodEnd
  AND (
      (ri.Status = 'A' AND ri.Type = 'A' AND EXISTS (
          SELECT 1 FROM dbo.DebtorTransactions AS inactiveLink
          WHERE inactiveLink.ReceiptItemID = ri.ID
            AND inactiveLink.Status = 'D'
            AND inactiveLink.Type IN ('I', 'S')
      ))
      OR (ri.Status = 'C' AND ri.Type = 'RX')
  )
ORDER BY ri.Date, ri.Type, dt.Status, dt.Type;

/* Result 2: payment context for the affected parent receipts.
   Select each receipt once using EXISTS, so several matching item or debtor
   links cannot multiply payment sums. A receipt can use split tender, so
   these totals alone do not allocate a payment to an individual item.
   Amount is gross tender for cash and may include change. */
SELECT
    N'02 affected receipt payments' AS resultSet,
    r.Date AS receiptDate,
    r.Status AS receiptStatus,
    pm.Code AS paymentCode,
    pm.Name AS paymentName,
    COUNT_BIG(*) AS paymentRowCount,
    SUM(rp.Amount) AS grossPaymentAmount,
    SUM(rp.RoundingAmount) AS roundingAmount
FROM dbo.Receipts AS r
INNER JOIN dbo.ReceiptPayments AS rp ON rp.ReceiptID = r.ID
LEFT JOIN dbo.PaymentMethods AS pm ON pm.ID = rp.PaymentMethodID
WHERE EXISTS (
    SELECT 1 FROM dbo.ReceiptItems AS ri
    WHERE ri.ReceiptID = r.ID
      AND ri.Date >= @PeriodStart AND ri.Date < @PeriodEnd
      AND (
          (ri.Status = 'A' AND ri.Type = 'A' AND EXISTS (
              SELECT 1 FROM dbo.DebtorTransactions AS inactiveLink
              WHERE inactiveLink.ReceiptItemID = ri.ID
                AND inactiveLink.Status = 'D'
                AND inactiveLink.Type IN ('I', 'S')
          ))
          OR (ri.Status = 'C' AND ri.Type = 'RX')
      )
)
GROUP BY r.ID, r.Date, r.Status, pm.Code, pm.Name
ORDER BY r.Date, r.Status, pm.Code;

/* Result 3: all item types/statuses in those receipts. This reveals mixed
   sales or cancellations without showing item descriptions or identifiers. */
SELECT
    N'03 affected receipt items' AS resultSet,
    r.Date AS receiptDate,
    r.Status AS receiptStatus,
    ri.Status AS itemStatus,
    ri.Type AS itemType,
    COUNT_BIG(*) AS itemCount,
    SUM(ri.Subtotal) AS itemSubtotal
FROM dbo.Receipts AS r
INNER JOIN dbo.ReceiptItems AS ri ON ri.ReceiptID = r.ID
WHERE EXISTS (
    SELECT 1 FROM dbo.ReceiptItems AS candidate
    WHERE candidate.ReceiptID = r.ID
      AND candidate.Date >= @PeriodStart AND candidate.Date < @PeriodEnd
      AND (
          (candidate.Status = 'A' AND candidate.Type = 'A' AND EXISTS (
              SELECT 1 FROM dbo.DebtorTransactions AS inactiveLink
              WHERE inactiveLink.ReceiptItemID = candidate.ID
                AND inactiveLink.Status = 'D'
                AND inactiveLink.Type IN ('I', 'S')
          ))
          OR (candidate.Status = 'C' AND candidate.Type = 'RX')
      )
)
GROUP BY r.ID, r.Date, r.Status, ri.Status, ri.Type
ORDER BY r.Date, ri.Status, ri.Type;

/*
  Run in SSMS against the same Z-Office/APSS database used by the forwarder.
  Read-only diagnostic for invoice 74339273, order 57750, 20 October 2026.
  The GUI shows invoice total $321.30; the PDF shows $321.29.
  Return all four result sets. No source records are changed.
*/
SET NOCOUNT ON;
DECLARE @InvoiceNumber varchar(128) = '74339273';
DECLARE @OrderNumber int = 57750;

/* 1: Identify the database and its comparison rules. */
SELECT N'01 database' AS resultSet,
       CONVERT(nvarchar(128), SERVERPROPERTY('ServerName')) AS serverName,
       DB_NAME() AS databaseName,
       CONVERT(nvarchar(128), DATABASEPROPERTYEX(DB_NAME(), 'Collation')) AS databaseCollation;

/* 2: The order header shown by Receive Order is not necessarily the reference
   on the individual receipt rows used by the live invoice query. */
SELECT N'02 order headers' AS resultSet,
       o.ID AS orderID, o.OrderNo AS orderNumber, o.Status AS orderStatus,
       o.ReceivedDate AS receivedDate, s.Name AS supplierName,
       N'[' + COALESCE(CONVERT(nvarchar(128), o.InvoiceNo), N'<NULL>') + N']' AS headerInvoice,
       CASE WHEN o.Status = 'Received' THEN 1 ELSE 0 END AS passesLiveStatusFilter,
       CASE WHEN s.ID IS NOT NULL THEN 1 ELSE 0 END AS hasSupplier,
       (SELECT COUNT_BIG(*) FROM dbo.OrderStock AS stock WHERE stock.OrderID = o.ID) AS stockRows
FROM dbo.Orders AS o
LEFT JOIN dbo.Suppliers AS s ON s.ID = o.SupplierID
WHERE o.OrderNo = @OrderNumber
   OR o.InvoiceNo LIKE '%' + @InvoiceNumber + '%'
   OR EXISTS (
       SELECT 1 FROM dbo.OrderStock AS stock
       INNER JOIN dbo.OrderStockReceived AS received ON received.OrderStockID = stock.ID
       WHERE stock.OrderID = o.ID AND received.InvoiceNo LIKE '%' + @InvoiceNumber + '%'
   )
ORDER BY o.OrderNo, o.ID;

/* 3: Left joins preserve missing receipt rows. Brackets and bytes reveal
   spaces/control characters; each row also shows the production predicates. */
SELECT N'03 receipt lines' AS resultSet,
       o.OrderNo AS orderNumber, os.ID AS stockLineID,
       o.Status AS orderStatus, s.Name AS supplierName,
       N'[' + COALESCE(CONVERT(nvarchar(128), o.InvoiceNo), N'<NULL>') + N']' AS headerInvoice,
       N'[' + COALESCE(CONVERT(nvarchar(128), osr.InvoiceNo), N'<NULL>') + N']' AS receiptInvoice,
       LEN(osr.InvoiceNo) AS receiptReferenceLength,
       DATALENGTH(osr.InvoiceNo) AS receiptReferenceBytes,
       CONVERT(varbinary(256), CONVERT(nvarchar(128), osr.InvoiceNo)) AS receiptReferenceUtf16,
       osr.Qty AS receivedQty, osr.InvoiceCostEx AS unitCostExGst, osr.GST AS gstFlag,
       ROUND(COALESCE(osr.Qty, 0) * COALESCE(osr.InvoiceCostEx, 0)
           * CASE WHEN osr.GST = 1 THEN 1.1 ELSE 1.0 END, 2) AS calculatedLineTotal,
       CASE WHEN osr.OrderStockID IS NOT NULL THEN 1 ELSE 0 END AS hasReceipt,
       CASE WHEN osr.InvoiceNo = @InvoiceNumber THEN 1 ELSE 0 END AS passesExactReferenceFilter,
       CASE WHEN o.Status = 'Received' THEN 1 ELSE 0 END AS passesLiveStatusFilter,
       CASE WHEN s.ID IS NOT NULL THEN 1 ELSE 0 END AS hasSupplier
FROM dbo.Orders AS o
LEFT JOIN dbo.Suppliers AS s ON s.ID = o.SupplierID
LEFT JOIN dbo.OrderStock AS os ON os.OrderID = o.ID
LEFT JOIN dbo.OrderStockReceived AS osr ON osr.OrderStockID = os.ID
WHERE o.OrderNo = @OrderNumber
   OR o.InvoiceNo LIKE '%' + @InvoiceNumber + '%'
   OR EXISTS (
       SELECT 1 FROM dbo.OrderStock AS stock
       INNER JOIN dbo.OrderStockReceived AS received ON received.OrderStockID = stock.ID
       WHERE stock.OrderID = o.ID AND received.InvoiceNo LIKE '%' + @InvoiceNumber + '%'
   )
ORDER BY o.OrderNo, os.ID;

/* 4: The actual single-reference live query, without date or application-cache
   filters. If this returns no rows, the loss occurs before API reconciliation. */
SELECT N'04 live lookup result' AS resultSet,
       osr.InvoiceNo AS invoiceNumber,
       CAST(MIN(o.ReceivedDate) AS date) AS receivedDate,
       s.Name AS supplierName,
       COUNT_BIG(*) AS lineCount,
       SUM(COALESCE(osr.Qty, 0)) AS quantity,
       CAST(ROUND(SUM(COALESCE(osr.Qty, 0) * COALESCE(osr.InvoiceCostEx, 0)
           * CASE WHEN osr.GST = 1 THEN 1.1 ELSE 1.0 END), 2) AS decimal(19, 4)) AS amountIncludingGst
FROM dbo.Orders AS o
INNER JOIN dbo.Suppliers AS s ON s.ID = o.SupplierID
INNER JOIN dbo.OrderStock AS os ON os.OrderID = o.ID
INNER JOIN dbo.OrderStockReceived AS osr ON osr.OrderStockID = os.ID
WHERE o.Status = 'Received'
  AND osr.InvoiceNo IS NOT NULL
  AND osr.InvoiceNo = @InvoiceNumber
GROUP BY osr.InvoiceNo, s.Name;

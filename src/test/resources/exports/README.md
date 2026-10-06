# Original Xero CSV fixture

legacy-xero-september-2026.csv was generated with the original PrintWriter
body and searchTillData method from EODDataEntryPageController at commit
ae7c533fa0c6b5049ad6c73d2a45a17c830efa49, before extracting or correcting the
exporter. It contains only synthetic data.

The inputs are specified in XeroEodExporterAuditTest: store 1, September 2026,
entered days 1/16/30, tender amounts 2/3/4/5/6, Medschecks 7, stock 123.45,
scripts on file 8, SMS 9, and "Legacy note", plus distinct diagnostic metrics.
Manual mode was selected. Do not regenerate this fixture with the new
exporter: it is independent evidence of the previous column mappings.

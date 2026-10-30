# Gemini supplier matching

The background Drive scanner in Alpha API resolves OCR supplier names against the store's configured invoice suppliers before retaining the scan result, fetching expected amounts or attempting automatic entry. This allows a printed legal name such as Australian Pharmaceutical Industries Pty Limited to resolve to the saved API contact without adding supplier-specific aliases.

Exact, unambiguous saved-name matches use the existing local matcher. Other names are sent to Gemini as text with the configured supplier IDs and names. No PDF is uploaded again. The response must select a supplied ID or return no match, with a confidence score. The prompt requires branch/location identity and rejects ambiguous choices.

Names repeated within a PDF or subsequent PDFs in the same store scan reuse one result. Each run loads a fresh contact list; results are never shared between stores. Requests contain at most 32 distinct OCR names, have a 30-second timeout, and do not retry provider failures.

A model match must meet the existing 90% confidence threshold and resolve to an unambiguous saved contact. The retained supplier confidence is the lower of OCR confidence and matching confidence. A model match cannot raise an existing low OCR score. Missing matches, invalid responses, duplicate contacts and provider failures retain the OCR name and require review. Other extracted values and PDF evidence remain intact.

The matcher runs on Alpha API and uses its existing Gemini API key, API base URL and service tier. It defaults to the configured `gemini.model`. Set `gemini.supplier.model` or `GEMINI_SUPPLIER_MODEL` on Alpha API to choose a separate model. No Gemini credential or matching call is added to the desktop client.

Deploy and restart the updated Alpha API to enable this for new scans. Existing completed extractions are not changed merely by opening the review queue; retry or rescan an affected queued PDF to run matching before reviewing it again.

Tests cover the text-only request, configured-ID validation, uncertain and duplicate matches, repeated-name caching, store isolation, preserved OCR confidence, cancellation, and matching before background amount lookup. Provider responses are mocked; real Gemini matching quality still needs checking against representative invoices after rollout.

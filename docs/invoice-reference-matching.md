# Supplier invoice reference matching

CH2 / Paragon Care invoices can print a numeric reference with an `RI` suffix while Z-Office stores only the number. For example, `74040054 RI` and `74040054RI` can match the internal reference `74040054`.

The desktop and Alpha API first try the scanned reference, then the supplier-specific alternative. A numeric scan can also find an internal number ending in `RI` or ` RI`. An exact match takes precedence even when an alternative has an amount closer to the scan. Leading zeros are preserved. This rule applies only to numeric invoice references for CH2 and its recognised Paragon Care / Clifford Hallam Healthcare issuer names. Credits, unrelated suppliers and other suffixes retain their existing matching rules.

Matching uses existing imported amounts and the invoice lookup cache before making bounded Z-Office requests. Background scanning, review loading and manual amount refresh all use the alternatives. Known supplier identities must belong to CH2 / Paragon Care; amount-only imports without supplier metadata remain usable. Multiple eligible matches require review.

The original scanned reference remains in the extraction evidence. Review and automatic entry default to the matched internal reference, and explicit reviewer changes remain intact. Without an internal match, the scanned reference is retained. Statement reconciliation also recognises the optional suffix on saved invoice references and treats duplicate forms as ambiguous.

Deploy and restart the updated Alpha API and release the updated desktop client together. The change uses the existing invoice lookup endpoints and forwarder operations. It requires no schema migration or OCR prompt change. Existing queued extractions can use the fallback when their amounts are loaded again.

Regression coverage includes spaced and unspaced suffixes, reverse matching, leading zeros, exact-match precedence, supplier isolation, ambiguity, cached imports, batch lookup, manual refresh, automatic entry, desktop saving, explicit reviewer edits and statement duplicate detection. Z-Office and Drive are mocked or replaced with local fixtures; production deployment is still required.

# Generic invoice reference matching

Invoice matching uses the same rules for every supplier. It no longer has reference-specific checks for Symbion, CH2 or particular affixes.

An exact reference match takes priority, ignoring the case, spaces and punctuation already handled by invoice reconciliation. If there is no eligible exact match, a reference with a single uninterrupted number and leading or trailing text can be compared with that bare number. Examples include `74040054 RI`, `INV-74040054`, and an unfamiliar supplier's `74040054 XYZ` versus `74040054`.

The complete number, including leading zeros, must match. Multiple numeric groups such as `INV2026-74040054` are not collapsed. Two different affixed references, such as `A74040054` and `B74040054`, remain distinct; neither is automatically equated to the other. Credit reference matching retains its existing strict rules.

Candidate selection uses supplier identity and preserves ambiguity. It never selects one of several alternatives because its amount is closer. A fallback requires a known matching supplier, the same document date, sufficient extracted-field confidence, and the existing amount tolerance before automatic acceptance. Missing supplier/date metadata or a date difference leaves the candidate in review. Amount-only imports can still provide an expected amount for that review.


Combined supplier labels can repeat the same identity in two forms, for example `ParagonCare CH2` or `Australian Pharmaceutical Industries API`. Both parts must independently identify the same supplier using the existing exact-name, acronym or established supplier-alias rules. Extra unrelated names and branch qualifiers are not discarded.

Invoice `74180032 RI` for CH2, dated 1 October 2026 and totalling $1,813.06, exposed a gap after the generic reference update: the numeric reference correctly matched `74180032`, but the strict supplier check rejected Z-Office's combined label `ParagonCare CH2`, resulting in `NOT_IN_Z_OFFICE`. The regression fixture reproduced that status before the supplier-label fix. Refresh now also explains when a returned reference was rejected for its supplier instead of describing it as absent. These checks reproduce the supplied screenshot values locally; they do not establish what a particular production agent returned.

The API probes the printed reference and its bare number. It also indexes actual reference formats observed in imported amounts and the shared Z-Office receipt window, so a numeric scan can discover an existing internal prefix or suffix without guessing supplier-specific variations. The index is store-scoped and uses bounded database reads and writes. Existing references are backfilled once per store/API process; subsequent invoice imports and successful window and reference lookups update the index. If a previously unseen internal affix has not been imported or included in a receipt window, it cannot be invented from a numeric scan; keep the shared invoice window enabled for discovery.

The source reference stays in extraction evidence. Review and saving default to the selected internal reference; explicit reviewer changes remain intact. Statement reconciliation recognises generic alternatives while preventing duplicate forms from consuming the same entered invoice twice. A later empty lookup removes an earlier cached alternative.

Deploy and restart the updated Alpha API and release the updated desktop client. Alpha API automatically creates `invoice_reference_index`; it needs the same schema-creation permission already used for its invoice caches. No OCR prompt or forwarder-operation change is required. Existing queued extractions use the fallback when their amounts are loaded again.

Targeted verification covers unfamiliar suppliers and affixes, reverse discovery, leading zeros, multipart references, supplier isolation, missing identity evidence, exact-match priority, ambiguity, cache invalidation, bounded loading, refresh, automatic entry, desktop saving, explicit edits and statement reconciliation. Tests use local fixtures and mocked external services; production rollout is still required. On the current Java 27 development runtime, the API's older Mockito dependency needs `-Dnet.bytebuddy.experimental=true` for its tests.

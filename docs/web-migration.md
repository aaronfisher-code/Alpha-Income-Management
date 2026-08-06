# JavaFX to React migration status

## Implemented web foundation

- Vite, React, strict TypeScript, React Router, TanStack Query and PWA application shell.
- Montserrat font files and supplied Alpha/logo/login assets, with reusable colour, spacing, table, dialog, field and navigation styles derived from FXML/CSS.
- Responsive 60px header and 80px/260px sidebar. Browser chrome owns window controls.
- Cookie session authentication contracts, first-login password change, logout, store selection and permission-filtered navigation.
- Java-model-compatible DTOs and explicit resource methods for users, stores, permissions, employments, targets, EOD/till reports, payments/contacts, invoices/credits/suppliers, roster/special dates/leave, BAS and budget.
- CSRF headers for mutations, local-only shared bearer support, a production shared-token build guard, same-origin API defaults, and secure multipart legacy import.
- Reusable labelled fields/selects, month controls, tables, modals, side panels, toggles, status feedback, SVG gauges and charts.
- Interactive prototype workflows for staff targets, EOD, account payments, roster/leave, employees/permissions, invoices/credits/suppliers, BAS, budget/targets, monthly summary and settings/import.
- CSV downloads, clipboard operations and print-to-PDF roster output. No sensitive offline response cache or offline financial mutation queue.
- Unit, API-contract, component, navigation/CRUD and visual test scaffolding at all four target viewports.

## External work required before cutover

These items cannot be completed from this desktop-client repository alone:

1. Provide a safe API/test environment and representative non-production data.
2. Implement `POST /auth/login`, `GET /auth/session`, `POST /auth/logout`, authenticated password change, CSRF issuance, server-side store/permission enforcement and `POST /legacy-import` on the API.
3. Connect each feature's prototype state to its corresponding `src/api/resources.ts` query/mutation, then validate response/error variants against the live contracts.
4. Run the JavaFX client against the representative dataset and capture every page, dialog, popover, validation, loading, hover, selection, empty and permission state.
5. Replace the current deterministic web baselines with staff-approved JavaFX reference captures where relevant; retain web regression snapshots separately.
6. Validate spreadsheet parsing and generated accounting CSV/PDF output byte-for-byte (or field-for-field where metadata differs) against JavaFX.
7. Complete keyboard/assistive-technology and current Chrome/Edge smoke testing, API authorization tests, staff acceptance and rollback rehearsal.

## Reference capture checklist

Use the same dataset, timezone (`Australia/Melbourne`), font files and 100% display scale for both clients. Capture at 1500×800 first, then 1366×768, 1024×768 and 768×1024 for the web implementation.

- Login: empty, focused, invalid, loading, server error and first-password setup.
- Shell: every permission set, store selector open, profile menu, sidebar collapsed/expanded, active/hover items and session expiry.
- Each page: loading, populated, empty and API error states; month/week navigation and horizontal overflow.
- Every add/edit/delete/import/export dialog or panel, including validation and confirmation states.
- Targets: WTD/MTD/YTD, graph/table, both target levels and stable chart fixtures.
- Roster: recurring shifts, modifications, special dates, leave overlays and print output.
- Financial tables: positive/negative/zero variance, totals, adjusted/unadjusted and imported/missing records.

Staff sign-off on these captures is the parity contract. Antialiasing-only variance may use the configured 1% pixel threshold; layout, content, typography and colour differences require review.

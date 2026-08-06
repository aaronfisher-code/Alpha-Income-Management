# Alpha Income Web

React + TypeScript browser/PWA replacement for the Alpha Income JavaFX client. The Java application remains unchanged in `src/main` and can continue to ship during parity review.

## Run locally

```bash
cp .env.example .env
npm install
npm run dev
```

Local development uses deterministic representative data by default. Enter any non-empty username and password; use `firstlogin` as the username to exercise mandatory password setup. Set `VITE_USE_MOCKS=false` to use the same-origin API.

## Verification

```bash
npm run lint
npm test
npm run build
npx playwright install chromium
npm run test:e2e
```

Playwright runs pinned Chromium at 1500×800, 1366×768, 1024×768 and 768×1024. Update baselines only after approved UI changes:

```bash
npm run test:e2e:update
```

## Environment and security

- `VITE_API_BASE_URL` defaults to `/api`; same-origin deployment is expected.
- `VITE_API_TOKEN` is accepted in development for legacy prototype access only. `npm run build` refuses to build if it is present.
- Production authentication uses secure session cookies. The API client sends credentials, adds the `X-XSRF-TOKEN` header from the conventional `XSRF-TOKEN` cookie for mutations, and never caches API responses in the service worker.
- Financial mutations are never queued offline. The PWA cache contains only application-shell assets.
- Spreadsheet migration is multipart upload to `POST /legacy-import`; the browser has no database credentials or direct MySQL path.

## Source layout

- `src/api`: DTO-compatible resource contracts and the cookie/CSRF client.
- `src/auth`: session login, logout and first-login password change.
- `src/components`: JavaFX-matched shell and reusable accessible controls.
- `src/features`: one module per business workflow.
- `src/utils`: calculations, formatting, CSV/download and roster recurrence.
- `e2e`: workflow and visual tests.

See [the migration status](../docs/web-migration.md) before production cutover.

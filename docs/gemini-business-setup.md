# Gemini API key setup for the business

For Alpha Income Management invoice and supplier-statement scanning. Google setup instructions checked on 23 October 2026; screen labels may change.

## Before starting

Have a business-managed Google account, a person authorised to accept Google's terms, and a business payment method ready. Ask IT to help if your organisation manages Google Cloud access. Keep project ownership and billing with the business, and nominate a backup administrator using their own account.

The business administrator completes steps 1–4. The person maintaining Alpha API completes step 5.

## 1. Open Google AI Studio and select a project

Sign in to [Google AI Studio](https://aistudio.google.com/) with the business account and accept the terms. Open **Dashboard → Projects**. Create or select a business project, for example **Alpha Invoice Scanning**. A new account may already have a default project; an existing Cloud project can be added through **Import projects**. Record its project ID. [Google's project instructions](https://ai.google.dev/gemini-api/docs/api-key#managing-api-keys-in-google-ai-studio).

## 2. Enable paid API billing

For the selected project, click **Set up billing**, link or create the business Cloud Billing account, and complete the payment details. Google may require prepaid credits; follow the amount and currency shown. Confirm the project shows a paid tier and any required prepaid balance is positive. [Google's billing instructions](https://ai.google.dev/gemini-api/docs/billing).

Use a billing-enabled project for real invoices. Google's unpaid-service terms allow submitted content and responses to be used for product improvement and reviewed by people. For paid API services, Google says it does not use prompts, uploaded documents, or responses to improve its products. Limited safety logging still applies; paid access does not promise Australian-only processing. [Google's data-use terms](https://ai.google.dev/gemini-api/terms#paid-services).

## 3. Set spending controls

Agree an initial monthly allowance with the business. On AI Studio's **Spend** page, set the project's **Monthly spend cap**, where available. Google describes this feature as experimental, with processing delays that can allow overages. If using prepaid auto-reload, set its monthly auto-charge limit too. [Google's spend controls](https://ai.google.dev/gemini-api/docs/billing#spend-caps).

Add a monthly budget in [Cloud Billing](https://console.cloud.google.com/billing), scoped to this project, with alerts to the business billing contact, for example at 50%, 90%, and 100%. An alerts-only budget sends notifications; it does not stop spending. [Budget instructions](https://docs.cloud.google.com/billing/docs/how-to/budgets).

## 4. Create and hand over the key

Open [AI Studio → API keys](https://aistudio.google.com/app/apikey), select **Create API key**, and choose the business project. Give it a recognisable name such as **Alpha API Production**. New AI Studio keys are authorization keys restricted to the Gemini API by default. If creation is blocked, ask the project administrator to resolve the [required permissions](https://ai.google.dev/gemini-api/docs/api-key#troubleshooting-key-creation-permissions).

Save the key in the business password manager and share it with the Alpha API maintainer through secure sharing. Keep the key out of email, screenshots, source control, and setup documents. Record the project ID, key name, billing contact, and administrator separately from the secret.

## 5. Install and verify in Alpha API

The maintainer should save these settings in the **Alpha API server's** deployment secrets or protected environment configuration, then restart the API:

```text
GEMINI_API_KEY=<key from the business password manager>
GEMINI_SERVICE_TIER=standard
```

Explicitly select `standard` for a new account: the sibling Alpha API example configuration requests `priority`. Priority requires higher paid tiers and costs more. [Google's inference-tier explanation](https://blog.google/innovation-and-ai/technology/developers-tools/introducing-flex-and-priority-inference/).

The Gemini key enables extraction. Google Drive also needs its own OAuth connection; follow the [Drive scanning setup](../README.md#configure-google-drive-document-scanning).

In Alpha Income Management, confirm extraction is available in **Settings**, connect the store's Drive folders, and run **Scan now** with a synthetic invoice PDF whose name contains `MARKED_OFF`. Confirm it reaches the review queue, check the extracted fields, and confirm usage appears against the business project in AI Studio before enabling scheduled scans of real documents.

If scanning fails, check the API process received the key, billing is active, prepaid credits are available where required, and the configured model is accessible. HTTP 401/403 generally indicates credential or access problems; repeated 429 responses can indicate quota exhaustion. If a key needs replacing, install and test its replacement before revoking the old key.

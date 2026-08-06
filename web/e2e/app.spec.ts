import { expect, test } from '@playwright/test'

test.beforeEach(async ({ page }) => {
  await page.goto('/login')
  await page.getByLabel('Username').fill('demo')
  await page.getByLabel('Password').fill('password')
  await page.getByRole('button', { name: 'Sign in' }).click()
  await expect(page).toHaveURL(/\/targets$/)
})

test('navigates every permitted workflow and switches store', async ({ page }) => {
  const routes = [['EOD Data Entry', '/eod'], ['Account Payments', '/payments'], ['Staff Roster', '/roster'], ['Manage Employees', '/employees'], ['Invoice Tracking', '/invoices'], ['BAS Checker', '/bas'], ['Budget & Expenses', '/budget'], ['Monthly Summary', '/summary'], ['Settings', '/settings']] as const
  for (const [label, path] of routes) {
    await page.getByRole('link', { name: label }).click()
    await expect(page).toHaveURL(new RegExp(`${path}$`))
  }
  await page.getByLabel('Current store').selectOption('2')
  await expect(page.getByLabel('Current store')).toHaveValue('2')
})

test('creates an account payment through an accessible side panel', async ({ page }) => {
  await page.getByRole('link', { name: 'Account Payments' }).click()
  await page.getByRole('button', { name: 'Add payment' }).click()
  await page.getByRole('combobox', { name: 'Supplier' }).click()
  await page.getByRole('option', { name: 'NDIS' }).click()
  await page.getByLabel('Invoice Number').fill('NDIS-100')
  await page.getByLabel('Unit Amount ($)').fill('450')
  await page.getByRole('button', { name: 'Save' }).click()
  await expect(page.getByText('NDIS-100')).toBeVisible()
})

test('adjusts target data to the selected calendar period', async ({ page }) => {
  await page.getByRole('button', { name: 'Show table for OTC $/Customer' }).click()
  const rows = page.locator('.desktop-target-card').filter({ hasText: 'OTC $/Customer' }).locator('tbody tr')
  await expect(rows).toHaveCount(7)
  await page.getByRole('tab', { name: /^MTD/ }).click()
  await expect(rows).toHaveCount(new Date(new Date().getFullYear(), new Date().getMonth() + 1, 0).getDate())
  await page.getByRole('tab', { name: /^YTD/ }).click()
  const year = new Date().getFullYear()
  await expect(rows).toHaveCount((new Date(year, 1, 29).getMonth() === 1 ? 366 : 365))
})

test('matches the JavaFX shell contract', async ({ page }, testInfo) => {
  await page.addStyleTag({ content: '*,*::before,*::after{animation-duration:0s!important;transition-duration:0s!important}' })
  await expect(page).toHaveScreenshot(`targets-${testInfo.project.name}.png`, { animations: 'disabled', maxDiffPixelRatio: 0.01 })
})

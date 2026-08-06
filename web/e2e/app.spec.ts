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

test('scrolls side-panel forms on short screens', async ({ page }) => {
  await page.setViewportSize({ width: 900, height: 500 })
  await page.getByRole('link', { name: 'EOD Data Entry' }).click()
  await page.locator('.eod-table-card tbody tr').first().dblclick()

  const panelBody = page.locator('.side-panel__body')
  await expect(panelBody).toBeVisible()
  await expect.poll(() => panelBody.evaluate((element) => element.scrollHeight > element.clientHeight)).toBe(true)
  await panelBody.evaluate((element) => element.scrollTo({ top: element.scrollHeight }))
  await expect.poll(() => panelBody.evaluate((element) => element.scrollTop)).toBeGreaterThan(0)
  await expect(page.getByRole('button', { name: 'Save' })).toBeVisible()
})

test('keeps table column headers visible while rows scroll', async ({ page }) => {
  await page.setViewportSize({ width: 900, height: 500 })
  await page.getByRole('link', { name: 'EOD Data Entry' }).click()

  const tableScroll = page.locator('.eod-table-card .table-scroll')
  const firstHeader = tableScroll.locator('thead th').first()
  const initialHeader = await firstHeader.boundingBox()
  expect(initialHeader).not.toBeNull()

  await tableScroll.evaluate((element) => element.scrollTo({ top: element.scrollHeight }))
  await expect.poll(() => tableScroll.evaluate((element) => element.scrollTop)).toBeGreaterThan(0)

  const scrolledHeader = await firstHeader.boundingBox()
  expect(scrolledHeader).not.toBeNull()
  expect(Math.abs(scrolledHeader!.y - initialHeader!.y)).toBeLessThanOrEqual(1)
  await expect(firstHeader).toBeVisible()
})

test('slides side panels out before removing them', async ({ page }) => {
  await page.getByRole('link', { name: 'Account Payments' }).click()
  await page.getByRole('button', { name: 'Add payment' }).click()

  const panel = page.locator('.side-panel')
  await expect(panel).toBeVisible()
  await page.getByRole('button', { name: 'Close' }).click()

  await expect.poll(() => panel.evaluate((element) => getComputedStyle(element).animationName)).toBe('slide-out')
  await expect(panel).toHaveCount(0)
})

test('matches the JavaFX shell contract', async ({ page }, testInfo) => {
  await page.addStyleTag({ content: '*,*::before,*::after{animation-duration:0s!important;transition-duration:0s!important}' })
  await expect(page).toHaveScreenshot(`targets-${testInfo.project.name}.png`, { animations: 'disabled', maxDiffPixelRatio: 0.01 })
})

import type { AccountPayment, AuthSession, Credit, EODDataPoint, Invoice, MonthlySummary, Shift, Store, Target, User } from '../types'

export const stores: Store[] = [
  { storeID: 1, storeName: 'Rainbow Health Pharmacy', storeHours: 10 },
  { storeID: 2, storeName: 'Caresaver Discount Chemist', storeHours: 11 }
]

export const permissions = [
  'Staff Targets', 'EOD Data Entry', 'Account Payments', 'Staff Roster', 'Manage Employees',
  'Invoice Tracking', 'BAS Checker', 'Budget & Expenses', 'Monthly Summary', 'Settings'
].map((permissionName, index) => ({ permissionID: index + 1, permissionName }))

export const user: User = {
  userID: 10, username: '10', first_name: 'Aaron', last_name: 'Fisher', nickname: 'Aaron', role: 'I.T. Admin',
  bgColour: '#f7bcc9', textColour: '#ff003c', inactiveDate: null,
  employments: stores.map((store, index) => ({ employmentID: index + 1, userID: 1, storeID: store.storeID })), permissions
}

export const mockSession = (firstLogin = false): AuthSession => ({ user, stores, permissions, firstLogin })

export const eodRows: EODDataPoint[] = [
  ['2026-07-13', 1094.35, 8450.61, 184.2, 96.4, 0, 2, 142903, 343, 521, 5.56, 'Balanced after recount'],
  ['2026-07-14', 978.1, 9204.48, 310.5, 140.75, 25, 3, 143120, 348, 526, -12.25, ''],
  ['2026-07-15', 1120, 8890.15, 260, 118.9, 0, 1, 143998, 352, 530, 3.4, '']
].map(([date, cashAmount, eftposAmount, amexAmount, googleSquareAmount, chequeAmount, medschecks, stockOnHandAmount, scriptsOnFile, smsPatients, tillBalance, notes], index) => ({
  existsInDB: true, date: String(date) as EODDataPoint['date'], storeID: 1, cashAmount: Number(cashAmount), eftposAmount: Number(eftposAmount),
  amexAmount: Number(amexAmount), googleSquareAmount: Number(googleSquareAmount), chequeAmount: Number(chequeAmount), medschecks: Number(medschecks),
  stockOnHandAmount: Number(stockOnHandAmount), scriptsOnFile: Number(scriptsOnFile), smsPatients: Number(smsPatients), tillBalance: Number(tillBalance),
  runningTillBalance: [42.18, 29.93, 33.33][index], notes: String(notes)
}))

export const targets: Target[] = [
  ['Number of Scripts', 7.5, 1312, 12, 1368], ['OTC $/Customer', 5, 18.72, 9, 19.43], ['GP ($)', 8, 19540, 12, 20264],
  ['Scripts on File', 4, 352, 8, 365], ['Medschecks', 5, 18, 10, 19], ['SMS Patients', 5, 530, 10, 555]
].map(([targetName, target1Growth, target1Actual, target2Growth, target2Actual]) => ({
  date: '2026-07-01', storeID: 1, targetName: String(targetName), target1Growth: Number(target1Growth), target1Actual: Number(target1Actual),
  useTarget1Growth: true, target2Growth: Number(target2Growth), target2Actual: Number(target2Actual), useTarget2Growth: true
}))

export const payments: AccountPayment[] = [
  { contactName: 'Medicare Australia', contactID: 1, storeID: 1, invoiceNumber: 'JUL-2601', invDate: '2026-07-04', dueDate: '2026-07-18', description: 'Services payment', quantity: 1, unitAmount: 4320.5, accountAdjusted: true, accountCode: '200', taxRate: 'GST Free Income' },
  { contactName: 'TAC Victoria', contactID: 2, storeID: 1, invoiceNumber: 'TAC-1842', invDate: '2026-07-11', dueDate: '2026-07-25', description: 'Claims payment', quantity: 1, unitAmount: 1280, accountAdjusted: false, accountCode: '200', taxRate: 'GST Free Income' }
]

export const invoices: Invoice[] = [
  { supplierName: 'Sigma Healthcare', storeID: 1, supplierID: 1, invoiceNo: 'SIG-70241', invoiceDate: '2026-07-07', dueDate: '2026-08-06', description: 'Pharmacy stock', quantity: 1, unitAmount: 18240.2, importedInvoiceAmount: 18235.2, variance: 5, credits: 280, totalAfterCredits: 17960.2, notes: '', invoiceExists: true, importExists: true, creditExists: true },
  { supplierName: 'API', storeID: 1, supplierID: 2, invoiceNo: 'API-48132', invoiceDate: '2026-07-12', dueDate: '2026-08-11', description: 'Pharmacy stock', quantity: 1, unitAmount: 11402.75, importedInvoiceAmount: 11402.75, variance: 0, credits: 0, totalAfterCredits: 11402.75, notes: '', invoiceExists: true, importExists: true, creditExists: false }
]

export const credits: Credit[] = [{ creditID: 1, supplierName: 'Sigma Healthcare', supplierID: 1, storeID: 1, creditNo: 'CR-2251', creditDate: '2026-07-10', referenceInvoiceNo: 'SIG-70241', creditAmount: 280, notes: 'Returned stock' }]

export const shifts: Shift[] = [
  ['2026-07-13', 1, 'Aaron', 'Fisher', 'Pharmacist', '08:30', '17:30', '#f7bcc9', '#ff003c'],
  ['2026-07-13', 2, 'Mia', 'Nguyen', 'Pharmacy Assistant', '09:00', '17:00', '#c7e8ff', '#1473e6'],
  ['2026-07-14', 3, 'Oliver', 'Smith', 'Pharmacist', '08:30', '18:00', '#d8f2d2', '#2d7a22'],
  ['2026-07-15', 2, 'Mia', 'Nguyen', 'Pharmacy Assistant', '10:00', '18:00', '#c7e8ff', '#1473e6'],
  ['2026-07-16', 1, 'Aaron', 'Fisher', 'Pharmacist', '08:30', '17:30', '#f7bcc9', '#ff003c'],
  ['2026-07-17', 4, 'Sophie', 'Jones', 'Retail Manager', '09:00', '17:00', '#f1dbff', '#8838af']
].map(([shiftStartDate, userID, first_name, last_name, role, shiftStartTime, shiftEndTime, profileBG, profileText], index) => ({
  shiftID: index + 1, storeID: 1, userID: Number(userID), shiftStartTime: String(shiftStartTime), shiftEndTime: String(shiftEndTime),
  shiftStartDate: String(shiftStartDate) as Shift['shiftStartDate'], shiftEndDate: String(shiftStartDate) as Shift['shiftEndDate'], thirtyMinBreaks: 1,
  tenMinBreaks: 1, repeating: index === 0, daysPerRepeat: 7, first_name: String(first_name), last_name: String(last_name), nickname: '',
  role: String(role), profileBG: String(profileBG), profileText: String(profileText), originalDate: null
}))

export const summaryRows: MonthlySummary[] = eodRows.map((row, index) => {
  const customers = [410, 452, 438][index]
  const scripts = [236, 271, 259][index]
  const items = [921, 1008, 986][index]
  const otcItems = [443, 490, 468][index]
  const totalIncome = row.cashAmount + row.eftposAmount + row.amexAmount + row.googleSquareAmount + row.chequeAmount
  const gpDollars = totalIncome * 0.327
  const zReportProfit = gpDollars - 1260 - 2320 - 840
  return { date: row.date, dayDuration: 1, noOfScripts: scripts, noOfCustomers: customers, noOfItems: items, noOfOTCItems: otcItems,
    itemsPerCustomer: items / customers, otcPerCustomer: otcItems / customers, dollarPerCustomer: totalIncome / customers,
    otcDollarPerCustomer: (totalIncome * .43) / customers, totalIncome, gpDollars, gpPercentage: 32.7, rentAndOutgoings: 1260,
    wages: 2320, outgoings: 840, zReportProfit, runningZProfit: zReportProfit * (index + 1), tillBalance: row.tillBalance,
    runningTillBalance: row.runningTillBalance, grossProfitDollars: gpDollars, govtRecovery: 0, totalGovtContribution: 0 }
})

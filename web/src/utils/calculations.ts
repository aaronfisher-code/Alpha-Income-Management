import type { EODDataPoint, MonthlySummary, Shift, Target } from '../types'

export const roundMoney = (value: number) => Math.round((value + Number.EPSILON) * 100) / 100

export function calculateTillBalances(
  values: Pick<EODDataPoint, 'cashAmount' | 'eftposAmount' | 'amexAmount' | 'googleSquareAmount' | 'chequeAmount'>,
  totalTakings: number,
  previousRunningTillBalance: number
) {
  const tillBalance = roundMoney(values.cashAmount + values.eftposAmount + values.amexAmount + values.googleSquareAmount + values.chequeAmount - totalTakings)
  return { tillBalance, runningTillBalance: roundMoney(previousRunningTillBalance + tillBalance) }
}

export const invoiceVariance = (invoiceAmount: number, importedAmount: number) => roundMoney(invoiceAmount - importedAmount)
export const paymentTotal = (quantity: number, unitAmount: number) => roundMoney(quantity * unitAmount)
export const targetValue = (lastYear: number, growthPercent: number) => roundMoney(lastYear * (1 + growthPercent / 100))
export const targetVariance = (actual: number, target: number) => roundMoney(actual - target)
export const targetProgress = (actual: number, target: number) => target <= 0 ? 0 : Math.max(0, Math.min(actual / target, 1.25))

export function dailyBudget(monthly: number, openDays: number, partialDays = 0) {
  const weightedDays = openDays + partialDays * 0.5
  return weightedDays <= 0 ? 0 : roundMoney(monthly / weightedDays)
}

export function summarise(rows: MonthlySummary[]): MonthlySummary {
  const sum = (key: keyof MonthlySummary) => rows.reduce((total, row) => total + Number(row[key]), 0)
  const customers = sum('noOfCustomers')
  const income = sum('totalIncome')
  const items = sum('noOfItems')
  const otcItems = sum('noOfOTCItems')
  const gp = sum('gpDollars')
  const last = rows.at(-1)
  return {
    date: last?.date ?? '1970-01-01', dayDuration: sum('dayDuration'), noOfScripts: sum('noOfScripts'), noOfCustomers: customers,
    noOfItems: items, noOfOTCItems: otcItems, itemsPerCustomer: customers ? items / customers : 0, otcPerCustomer: customers ? otcItems / customers : 0,
    dollarPerCustomer: customers ? income / customers : 0, otcDollarPerCustomer: customers ? rows.reduce((n, row) => n + row.otcDollarPerCustomer * row.noOfCustomers, 0) / customers : 0,
    totalIncome: income, gpDollars: gp, gpPercentage: income ? gp / income * 100 : 0, rentAndOutgoings: sum('rentAndOutgoings'), wages: sum('wages'),
    outgoings: sum('outgoings'), zReportProfit: sum('zReportProfit'), runningZProfit: last?.runningZProfit ?? 0, tillBalance: sum('tillBalance'),
    runningTillBalance: last?.runningTillBalance ?? 0, grossProfitDollars: sum('grossProfitDollars'), govtRecovery: sum('govtRecovery'),
    totalGovtContribution: sum('totalGovtContribution')
  }
}

const minutes = (time: string) => { const [h, m] = time.split(':').map(Number); return h * 60 + m }
export function paidShiftHours(shift: Pick<Shift, 'shiftStartTime' | 'shiftEndTime' | 'thirtyMinBreaks'>) {
  let duration = minutes(shift.shiftEndTime) - minutes(shift.shiftStartTime)
  if (duration < 0) duration += 24 * 60
  return Math.max(0, duration - shift.thirtyMinBreaks * 30) / 60
}

export function occursOn(shift: Shift, date: string) {
  if (!shift.repeating) return shift.shiftStartDate === date
  const start = Date.parse(`${shift.shiftStartDate}T00:00:00Z`)
  const current = Date.parse(`${date}T00:00:00Z`)
  const end = Date.parse(`${shift.shiftEndDate}T00:00:00Z`)
  const days = Math.round((current - start) / 86_400_000)
  return current >= start && current <= end && days % shift.daysPerRepeat === 0
}

export const targetLabel = (target: Target) => target.targetName

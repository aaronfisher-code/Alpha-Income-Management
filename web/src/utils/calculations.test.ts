import { calculateTillBalances, dailyBudget, invoiceVariance, occursOn, paidShiftHours, paymentTotal, summarise, targetValue } from './calculations'
import { summaryRows, shifts } from '../api/mockData'

describe('financial calculations', () => {
  it('matches the Java EOD till balance calculation', () => {
    expect(calculateTillBalances({ cashAmount: 100, eftposAmount: 200, amexAmount: 30, googleSquareAmount: 20, chequeAmount: 10 }, 355.25, 12.5)).toEqual({ tillBalance: 4.75, runningTillBalance: 17.25 })
  })
  it('rounds invoice, payment, target and budget calculations to cents', () => {
    expect(invoiceVariance(125.555, 100)).toBe(25.56)
    expect(paymentTotal(3, 19.999)).toBe(60)
    expect(targetValue(1000, 7.5)).toBe(1075)
    expect(dailyBudget(3100, 30, 2)).toBe(100)
  })
  it('uses weighted aggregate ratios for monthly totals', () => {
    const total = summarise(summaryRows)
    expect(total.totalIncome).toBeCloseTo(summaryRows.reduce((n, row) => n + row.totalIncome, 0))
    expect(total.itemsPerCustomer).toBeCloseTo(total.noOfItems / total.noOfCustomers)
    expect(total.runningTillBalance).toBe(summaryRows.at(-1)?.runningTillBalance)
  })
})

describe('roster calculations', () => {
  it('subtracts unpaid breaks and supports overnight shifts', () => {
    expect(paidShiftHours({ shiftStartTime: '09:00', shiftEndTime: '17:30', thirtyMinBreaks: 1 })).toBe(8)
    expect(paidShiftHours({ shiftStartTime: '22:00', shiftEndTime: '06:00', thirtyMinBreaks: 2 })).toBe(7)
  })
  it('expands recurring shifts only at the configured interval', () => {
    const recurring = { ...shifts[0], shiftEndDate: '2026-08-31' as const }
    expect(occursOn(recurring, '2026-07-20')).toBe(true)
    expect(occursOn(recurring, '2026-07-21')).toBe(false)
  })
})

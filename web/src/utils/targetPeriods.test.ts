import { describe, expect, it } from 'vitest'
import { targetPeriodRange } from './targetPeriods'

const iso = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`

describe('targetPeriodRange', () => {
  const today = new Date(2026, 7, 6)

  it('uses Monday through Sunday for WTD', () => {
    const range = targetPeriodRange('WTD', today)
    expect([iso(range.start), iso(range.end), range.dates.length]).toEqual(['2026-08-03', '2026-08-09', 7])
  })

  it('uses the current calendar month for MTD', () => {
    const range = targetPeriodRange('MTD', today)
    expect([iso(range.start), iso(range.end), range.dates.length]).toEqual(['2026-08-01', '2026-08-31', 31])
  })

  it('uses the current calendar year for YTD', () => {
    const range = targetPeriodRange('YTD', today)
    expect([iso(range.start), iso(range.end), range.dates.length]).toEqual(['2026-01-01', '2026-12-31', 365])
  })
})

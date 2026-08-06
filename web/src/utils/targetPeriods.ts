export type TargetPeriod = 'WTD' | 'MTD' | 'YTD'

export interface TargetPeriodRange {
  start: Date
  end: Date
  today: Date
  dates: Date[]
}

const atMidnight = (date: Date) => new Date(date.getFullYear(), date.getMonth(), date.getDate())
const addDays = (date: Date, days: number) => new Date(date.getFullYear(), date.getMonth(), date.getDate() + days)

export function melbourneToday(now = new Date()) {
  const parts = new Intl.DateTimeFormat('en-AU', { timeZone: 'Australia/Melbourne', year: 'numeric', month: 'numeric', day: 'numeric' }).formatToParts(now)
  const value = (type: Intl.DateTimeFormatPartTypes) => Number(parts.find((part) => part.type === type)?.value)
  return new Date(value('year'), value('month') - 1, value('day'))
}

export function targetPeriodRange(period: TargetPeriod, today = melbourneToday()): TargetPeriodRange {
  const current = atMidnight(today)
  let start: Date
  let end: Date
  if (period === 'WTD') {
    const daysSinceMonday = (current.getDay() + 6) % 7
    start = addDays(current, -daysSinceMonday)
    end = addDays(start, 6)
  } else if (period === 'MTD') {
    start = new Date(current.getFullYear(), current.getMonth(), 1)
    end = new Date(current.getFullYear(), current.getMonth() + 1, 0)
  } else {
    start = new Date(current.getFullYear(), 0, 1)
    end = new Date(current.getFullYear(), 11, 31)
  }
  const dates: Date[] = []
  for (let date = start; date <= end; date = addDays(date, 1)) dates.push(date)
  return { start, end, today: current, dates }
}

export function targetPeriodLabel(range: TargetPeriodRange) {
  const sameMonth = range.start.getMonth() === range.end.getMonth()
  const start = new Intl.DateTimeFormat('en-AU', { day: 'numeric', month: 'short', year: sameMonth ? undefined : 'numeric' }).format(range.start)
  const end = new Intl.DateTimeFormat('en-AU', { day: 'numeric', month: 'short', year: 'numeric' }).format(range.end)
  return `${start} – ${end}`
}

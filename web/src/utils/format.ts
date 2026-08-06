export const currency = new Intl.NumberFormat('en-AU', { style: 'currency', currency: 'AUD' })
export const number = new Intl.NumberFormat('en-AU', { maximumFractionDigits: 2 })
export const percent = new Intl.NumberFormat('en-AU', { maximumFractionDigits: 1, style: 'percent' })

export function displayDate(iso: string) {
  if (!iso) return ''
  const [year, month, day] = iso.slice(0, 10).split('-').map(Number)
  return new Intl.DateTimeFormat('en-AU', { day: '2-digit', month: '2-digit', year: 'numeric' }).format(new Date(year, month - 1, day))
}

export function displayMonth(month: string) {
  const [year, monthIndex] = month.split('-').map(Number)
  return new Intl.DateTimeFormat('en-AU', { month: 'long', year: 'numeric' }).format(new Date(year, monthIndex - 1, 1))
}

export function initials(first: string, last = '') { return `${first[0] ?? ''}${last[0] ?? ''}`.toUpperCase() }

export const currentMonth = () => new Date().toISOString().slice(0, 7)

export function changeMonth(month: string, delta: number) {
  const [year, monthIndex] = month.split('-').map(Number)
  const value = new Date(year, monthIndex - 1 + delta, 1)
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}`
}

export function toCsv<T extends object>(rows: T[]) {
  if (!rows.length) return ''
  const headers = Object.keys(rows[0]) as Array<keyof T>
  const escape = (value: unknown) => `"${String(value ?? '').replaceAll('"', '""')}"`
  return [headers.map((key) => escape(String(key))).join(','), ...rows.map((row) => headers.map((key) => escape(row[key])).join(','))].join('\r\n')
}

export function downloadText(filename: string, contents: string, type = 'text/csv;charset=utf-8') {
  const link = document.createElement('a')
  link.href = URL.createObjectURL(new Blob([contents], { type }))
  link.download = filename
  link.click()
  URL.revokeObjectURL(link.href)
}

import { useState } from 'react'
import { Button, Card, PageHeader, Notice } from '../components/UI'
import { currentMonth, currency } from '../utils/format'

type CheckRow = { key: string; label: string; spreadsheet: number; adjustment: number; correct: boolean; help?: string }
const initial: CheckRow[] = [
  { key: 'cash', label: 'Cash Income', spreadsheet: 32410.5, adjustment: 0, correct: true }, { key: 'eftpos', label: 'Eftpos Income', spreadsheet: 226530.12, adjustment: -42, correct: false },
  { key: 'amex', label: 'Amex Income', spreadsheet: 7240.3, adjustment: 0, correct: true }, { key: 'square', label: 'Google Square Income', spreadsheet: 2880.5, adjustment: 0, correct: true },
  { key: 'cheque', label: 'Cheques Income', spreadsheet: 925, adjustment: 0, correct: true }, { key: 'medicare', label: 'Medicare Income (excluding GST)', spreadsheet: 78420.41, adjustment: 120.15, correct: false, help: 'Medicare income entered into Xero as excluding GST' },
  { key: 'total', label: 'Total income from spreadsheet into Xero', spreadsheet: 348406.83, adjustment: 78.15, correct: false }, { key: 'gst', label: 'GST we owe ATO', spreadsheet: 16218.44, adjustment: 0, correct: true }
]

export function BASPage() {
  const [month, setMonth] = useState(currentMonth())
  const [rows, setRows] = useState(initial)
  const [saved, setSaved] = useState(false)
  return <div className="page"><PageHeader title="BAS Checker" subtitle="Reconcile spreadsheet figures before BAS submission" month={month} setMonth={setMonth} actions={<Button onClick={() => setSaved(true)}>Save</Button>} />{saved && <Notice kind="success">BAS checks saved for {month}.</Notice>}<Card className="bas-card"><div className="bas-table"><div className="bas-head"><span>Description</span><span>Figures as they appear</span><span>End of month adjustments</span><span>Total after adjustment</span><span>Correct?</span></div>{rows.map((row, index) => <div className="bas-row" key={row.key}><span><b>{row.label}</b>{row.help && <small>{row.help}</small>}</span><span>{currency.format(row.spreadsheet)}</span><label><span className="sr-only">{row.label} adjustment</span><input type="number" step="0.01" value={row.adjustment} onChange={(e) => setRows(rows.map((item, i) => i === index ? { ...item, adjustment: Number(e.target.value), correct: false } : item))} /></label><strong>{currency.format(row.spreadsheet + row.adjustment)}</strong><label className="check"><input type="checkbox" checked={row.correct} onChange={(e) => setRows(rows.map((item, i) => i === index ? { ...item, correct: e.target.checked } : item))} /><span>{row.correct ? '✓' : ''}</span></label></div>)}</div></Card><div className="two-column"><Card><h2>Medicare check</h2><dl className="check-summary"><div><dt>From Spreadsheet</dt><dd>{currency.format(78420.41)}</dd></div><div><dt>From BAS-time “Daily script totals”</dt><dd>{currency.format(78540.56)}</dd></div><div><dt>Adjustment Needed</dt><dd className="negative">{currency.format(120.15)}</dd></div></dl></Card><Card><h2>Spreadsheet / COGs check</h2><dl className="check-summary"><div><dt>Spreadsheet Check</dt><dd className="positive">Balanced</dd></div><div><dt>COGs Check</dt><dd className="positive">Balanced</dd></div><div><dt>Till Balance</dt><dd>{currency.format(33.33)}</dd></div></dl></Card></div></div>
}

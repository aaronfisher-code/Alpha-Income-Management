import { useMemo, useState, type FormEvent } from 'react'
import { eodRows as seededRows } from '../api/mockData'
import { Button, Card, DataTable, Field, MonthToolbar, SidePanel, TextArea } from '../components/UI'
import type { EODDataPoint } from '../types'
import { calculateTillBalances } from '../utils/calculations'
import { currency, displayDate, downloadText, toCsv } from '../utils/format'

const emptyRow = (date: EODDataPoint['date']): EODDataPoint => ({ existsInDB: false, date, storeID: 1, cashAmount: 0, eftposAmount: 0, amexAmount: 0, googleSquareAmount: 0, chequeAmount: 0, medschecks: 0, stockOnHandAmount: 0, scriptsOnFile: 0, smsPatients: 0, tillBalance: 0, runningTillBalance: 0, notes: '' })
const sample = [
  [140, 3459.55, 53.95, 7.55, -7.55, ''], [390, 2967.74, 80.94, 9.65, 2.1, ''], [465, 2768.83, 97.99, 2.95, 5.05, ''], [295, 3928.73, 41.67, -1.95, 3.1, ''],
  [0, 0, 0, 0, 3.1, ''], [535, 2816.24, 24.99, 1.55, 4.65, ''], [335, 4799.17, 77.99, -3.06, 1.59, ''], [245, 4575.11, 0, -39.95, -38.36, 'Change is down by $45 so added $45 to change.'],
  [350, 3099.34, 49.47, -1.51, -39.87, ''], [685, 3606.75, 0, 46.85, 6.98, 'extra $45 from change - put with takings'], [445, 3010.30, 0, -10.55, -3.57, '']
]
const initialRows: EODDataPoint[] = Array.from({ length: 31 }, (_, index) => {
  const day = index + 1
  const date = `2026-07-${String(day).padStart(2, '0')}` as EODDataPoint['date']
  const values = sample[index]
  if (!values) return seededRows.find((row) => row.date === date) ?? emptyRow(date)
  const [cashAmount, eftposAmount, amexAmount, tillBalance, runningTillBalance, notes] = values
  return { ...emptyRow(date), existsInDB: true, cashAmount: Number(cashAmount), eftposAmount: Number(eftposAmount), amexAmount: Number(amexAmount), tillBalance: Number(tillBalance), runningTillBalance: Number(runningTillBalance), notes: String(notes) }
})

export function EODPage() {
  const [month, setMonth] = useState('2026-07')
  const [rows, setRows] = useState(initialRows)
  const [editing, setEditing] = useState<EODDataPoint | null>(null)
  const [form, setForm] = useState<EODDataPoint>(initialRows.at(-1)!)
  const open = (row: EODDataPoint) => { setEditing(row); setForm(row) }
  const totalTakings = useMemo(() => form.cashAmount + form.eftposAmount + form.amexAmount + form.googleSquareAmount + form.chequeAmount - form.tillBalance, [form])
  const save = (event: FormEvent) => { event.preventDefault(); const previous = rows.filter((row) => row.date < form.date).at(-1)?.runningTillBalance ?? 0; const result = { ...form, ...calculateTillBalances(form, totalTakings, previous), existsInDB: true }; setRows((current) => current.map((row) => row.date === result.date ? result : row)); setEditing(null) }
  const moneyField = (key: keyof EODDataPoint, label: string) => <Field className="money-input" label={label} type="number" step="0.01" value={String(form[key])} onChange={(event) => setForm({ ...form, [key]: Number(event.target.value) })} />
  const numberField = (key: keyof EODDataPoint, label: string) => <Field label={label} type="number" value={String(form[key])} onChange={(event) => setForm({ ...form, [key]: Number(event.target.value) })} />
  const displayMoney = (value: number) => value === 0 ? '' : currency.format(value)
  const columns = [
    { key: 'date', label: 'DATE', render: (r: EODDataPoint) => displayDate(r.date) }, { key: 'cashAmount', label: 'CASH', render: (r: EODDataPoint) => displayMoney(r.cashAmount) }, { key: 'eftposAmount', label: 'EFTPOS', render: (r: EODDataPoint) => displayMoney(r.eftposAmount) },
    { key: 'amexAmount', label: 'AMEX', render: (r: EODDataPoint) => displayMoney(r.amexAmount) }, { key: 'googleSquareAmount', label: <>GOOGLE<br />SQUARE</>, render: (r: EODDataPoint) => displayMoney(r.googleSquareAmount) }, { key: 'chequeAmount', label: 'CHEQUE', render: (r: EODDataPoint) => displayMoney(r.chequeAmount) },
    { key: 'medschecks', label: 'MEDSCHECKS', render: (r: EODDataPoint) => r.medschecks || '' }, { key: 'stockOnHandAmount', label: <>STOCK ON<br />HAND</>, render: (r: EODDataPoint) => displayMoney(r.stockOnHandAmount) }, { key: 'scriptsOnFile', label: <>SCRIPTS ON<br />FILE</>, render: (r: EODDataPoint) => r.scriptsOnFile || '' },
    { key: 'smsPatients', label: 'SMS PATIENTS', render: (r: EODDataPoint) => r.smsPatients || '' }, { key: 'tillBalance', label: 'TILL BALANCE', render: (r: EODDataPoint) => <span className={r.tillBalance < 0 ? 'negative' : ''}>{r.existsInDB ? currency.format(r.tillBalance) : ''}</span> }, { key: 'runningTillBalance', label: <>RUNNING TILL<br />BALANCE</>, render: (r: EODDataPoint) => <span className={r.runningTillBalance < 0 ? 'negative' : ''}>{r.existsInDB ? currency.format(r.runningTillBalance) : ''}</span> }, { key: 'notes', label: 'NOTES' }
  ]
  return <div className="page desktop-data-page"><MonthToolbar value={month} onChange={setMonth} actions={<Button variant="secondary" className="xero-button" onClick={() => downloadText(`eod-${month}-xero.csv`, toCsv(rows))}><span className="xero-mark">xero</span>Export in Xero Format</Button>} /><Card className="desktop-table-card eod-table-card"><header><h1>End of Day Values:</h1><p>Double click on a row to edit</p></header><DataTable columns={columns} rows={rows.filter((row) => row.date.startsWith(month))} onRowClick={open} rowClassName={(row) => new Date(`${row.date}T00:00:00`).getDay() === 0 ? 'closed-row' : ''} /></Card>
    <SidePanel title={`Modify EOD Values for ${displayDate(form.date)}`} open={Boolean(editing)} onClose={() => setEditing(null)}><form className="form-stack desktop-side-form" onSubmit={save}><label className="button button--secondary file-button"><input type="file" multiple accept=".xlsx,.xls,.csv" /><span>⇧&nbsp; Import EOD files</span></label><h3>Till Balancing</h3>{moneyField('cashAmount', 'Cash')}{moneyField('eftposAmount', 'EFTPOS')}{moneyField('amexAmount', 'AMEX')}{moneyField('googleSquareAmount', 'Google Square')}{moneyField('chequeAmount', 'Cheque')}<div className="eod-balances"><span>Till Balance<b>{currency.format(form.tillBalance)}</b></span><span>Running Till Balance<b>{currency.format(form.runningTillBalance)}</b></span></div><h3>Store Tracking</h3>{numberField('medschecks', 'Medschecks')}{moneyField('stockOnHandAmount', 'Stock on hand')}{numberField('scriptsOnFile', 'Scripts on File Count')}{numberField('smsPatients', 'SMS patients')}<TextArea label="" aria-label="Additional notes" placeholder="Add any additional notes here..." rows={5} value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} /><Button type="submit">Save</Button></form></SidePanel>
  </div>
}

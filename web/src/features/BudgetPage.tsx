import { useState } from 'react'
import { targets } from '../api/mockData'
import { Button, Card, DataTable, Field, Notice, PageHeader } from '../components/UI'
import { dailyBudget, targetValue } from '../utils/calculations'
import { currentMonth, currency, number } from '../utils/format'

export function BudgetPage() {
  const [month, setMonth] = useState(currentMonth())
  const [saved, setSaved] = useState(false)
  const [values, setValues] = useState({ days: 31, open: 26, partial: 1, rent: 32500, outgoings: 420, building: 4800, loan: 12500, wages: 62800, cpa: 1400, lantern: 780, other: 2200, ato: 8240 })
  const change = (key: keyof typeof values, value: number) => setValues({ ...values, [key]: value })
  const monthlyFixed = values.rent + values.building + values.loan + values.wages
  const targetRows = targets.map((target) => ({ ...target, lastYear: target.target1Actual / (1 + target.target1Growth / 100) }))

  return <div className="page">
    <PageHeader title="Budget & Expenses" subtitle="Daily costs, end-of-month figures and targets" month={month} setMonth={setMonth} actions={<Button onClick={() => setSaved(true)}>Save</Button>} />
    {saved && <Notice kind="success">Budget and target values saved.</Notice>}
    <div className="budget-grid">
      <Card><h2>Daily expenses calculator</h2><div className="form-grid"><Field label="No. days this month" type="number" value={values.days} onChange={(e) => change('days', Number(e.target.value))} /><Field label="No. Open Days" type="number" value={values.open} onChange={(e) => change('open', Number(e.target.value))} /><Field label="No. Partial Days" type="number" value={values.partial} onChange={(e) => change('partial', Number(e.target.value))} /><Field label="Monthly Rent (ex GST)" type="number" value={values.rent} onChange={(e) => change('rent', Number(e.target.value))} /><Field label="Daily outgoings" type="number" value={values.outgoings} onChange={(e) => change('outgoings', Number(e.target.value))} /><Field label="Monthly Rental outgoings" type="number" value={values.building} onChange={(e) => change('building', Number(e.target.value))} /><Field label="Monthly loan expense" type="number" value={values.loan} onChange={(e) => change('loan', Number(e.target.value))} /><Field label="Monthly Wages" type="number" value={values.wages} onChange={(e) => change('wages', Number(e.target.value))} /></div><div className="budget-summary"><span>Daily rent <b>{currency.format(dailyBudget(values.rent, values.open, values.partial))}</b></span><span>Total daily average rent and expenses <b>{currency.format(dailyBudget(monthlyFixed, values.open, values.partial) + values.outgoings)}</b></span></div></Card>
      <Card><h2>End of month figures</h2>{([['cpa', '6CPA Income', 1550], ['lantern', 'Lantern Pay Income', 810], ['other', 'Other direct debit income', 2120], ['ato', 'ATO – GST BAS refund', 8240]] as const).map(([key, label, spreadsheet]) => <div className="variance-row" key={key}><Field label={`${label} — Income from Xero`} type="number" value={values[key]} onChange={(e) => change(key, Number(e.target.value))} /><span>From Spreadsheet <b>{currency.format(spreadsheet)}</b></span><span>Variance <b className={values[key] === spreadsheet ? 'positive' : 'negative'}>{currency.format(values[key] - spreadsheet)}</b></span></div>)}</Card>
    </div>
    <Card><h2>Targets</h2><DataTable rows={targetRows} columns={[{ key: 'targetName', label: 'Target', grow: true }, { key: 'lastYear', label: 'This month last year', minWidth: 130, render: (target) => number.format(target.lastYear) }, { key: 'target1Growth', label: 'Target 1 Growth (%)', minWidth: 125, render: (target) => `${target.target1Growth}%` }, { key: 'target1Actual', label: 'Target 1', render: (target) => number.format(targetValue(target.lastYear, target.target1Growth)) }, { key: 'target2Growth', label: 'Target 2 Growth (%)', minWidth: 125, render: (target) => `${target.target2Growth}%` }, { key: 'target2Actual', label: 'Target 2', render: (target) => number.format(targetValue(target.lastYear, target.target2Growth)) }]} /></Card>
  </div>
}

import { useState } from 'react'
import { summaryRows } from '../api/mockData'
import { Button, Card, DataTable, PageHeader } from '../components/UI'
import { summarise } from '../utils/calculations'
import { currentMonth, currency, displayDate, downloadText, number, toCsv } from '../utils/format'

export function SummaryPage() {
  const [month, setMonth] = useState(currentMonth())
  const rows = summaryRows.filter((row) => row.date.startsWith(month))
  const total = summarise(rows)
  const columns = [
    { key: 'date', label: 'Date', render: (r: any) => r.total ? 'TOTALS' : displayDate(r.date) }, { key: 'dayDuration', label: <>Duration of<br/>day (0–1)</>, render: (r: any) => number.format(r.dayDuration) }, { key: 'noOfScripts', label: <>No. of<br/>scripts</>, render: (r: any) => number.format(r.noOfScripts) },
    { key: 'noOfCustomers', label: <>No. of<br/>Customers</>, render: (r: any) => number.format(r.noOfCustomers) }, { key: 'noOfItems', label: <>No. of<br/>items</>, render: (r: any) => number.format(r.noOfItems) }, { key: 'noOfOTCItems', label: <>No. of OTC<br/>items</>, render: (r: any) => number.format(r.noOfOTCItems) },
    { key: 'itemsPerCustomer', label: <>ITEMS /<br/>CUSTOMER</>, render: (r: any) => number.format(r.itemsPerCustomer) }, { key: 'otcPerCustomer', label: <>OTC /<br/>CUSTOMER</>, render: (r: any) => number.format(r.otcPerCustomer) }, { key: 'dollarPerCustomer', label: <>$ /<br/>CUSTOMER</>, render: (r: any) => currency.format(r.dollarPerCustomer) },
    { key: 'otcDollarPerCustomer', label: <>OTC $ /<br/>CUSTOMER</>, render: (r: any) => currency.format(r.otcDollarPerCustomer) }, { key: 'totalIncome', label: <>TOTAL INCOME<br/>(IN-STORE SALES ONLY)</>, render: (r: any) => currency.format(r.totalIncome) }, { key: 'gpDollars', label: 'GP ($)', render: (r: any) => currency.format(r.gpDollars) },
    { key: 'gpPercentage', label: 'GP %', render: (r: any) => `${number.format(r.gpPercentage)}%` }, { key: 'rentAndOutgoings', label: <>RENT<br/>(INCL OUTGOINGS)</>, render: (r: any) => currency.format(r.rentAndOutgoings) }, { key: 'wages', label: <>WAGES<br/>(EXCL SUPER)</>, render: (r: any) => currency.format(r.wages) },
    { key: 'outgoings', label: 'OUTGOINGS', render: (r: any) => currency.format(r.outgoings) }, { key: 'zReportProfit', label: <>Z REPORT<br/>PROFIT</>, render: (r: any) => currency.format(r.zReportProfit) }, { key: 'runningZProfit', label: <>RUNNING<br/>Z PROFIT</>, render: (r: any) => currency.format(r.runningZProfit) },
    { key: 'tillBalance', label: <>TILL<br/>BALANCE</>, render: (r: any) => currency.format(r.tillBalance) }, { key: 'runningTillBalance', label: <>RUNNING TILL<br/>BALANCE</>, render: (r: any) => currency.format(r.runningTillBalance) }
  ]
  const exportRows = rows.map(({ grossProfitDollars: _, govtRecovery: __, totalGovtContribution: ___, ...row }) => row)
  return <div className="page summary-page"><PageHeader title="Monthly Summary" subtitle="Income, performance and operating totals" month={month} setMonth={setMonth} actions={<><Button variant="secondary" onClick={() => void navigator.clipboard.writeText(toCsv([{ ...total }]))}>One-line export</Button><Button icon="download" onClick={() => downloadText(`monthly-summary-${month}.csv`, toCsv(exportRows))}>Full table export</Button></>} /><Card className="summary-table"><DataTable columns={columns} rows={[...rows, { ...total, total: true }]} /></Card></div>
}

import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { payments as seededPayments } from '../api/mockData'
import { Button, Card, DataTable, Field, FloatingButton, MonthToolbar, SelectField, SidePanel, TextArea, Toggle } from '../components/UI'
import type { AccountPayment } from '../types'
import { paymentTotal } from '../utils/calculations'
import { currency, displayDate, downloadText, toCsv } from '../utils/format'

const contacts = ['6CPA PPA Online', '7CPA PPA Online', 'DCO Cobblebank', 'In-store account', 'TAC', 'Workcover', 'NDIS']
const names = ['Jean Chapman', 'Robert Grimaldi', 'Carlos Duran', 'Michael Plonsker', 'Joanne Theodore', 'Ricardo Vedar', 'Maria Krajina', 'Mary Berens', 'Peter Gatt', 'Hong Ngan Duong']
const emptyPayment: AccountPayment = { contactName: '', contactID: 0, storeID: 1, invoiceNumber: '', invDate: '2026-07-15', dueDate: '2026-07-31', description: '', quantity: 1, unitAmount: 0, accountAdjusted: false, accountCode: '200', taxRate: 'Gst Free Income' }
const referencePayments: AccountPayment[] = Array.from({ length: 42 }, (_, index) => ({ ...emptyPayment, contactName: index % 7 === 2 ? 'TAC' : index % 11 === 0 ? 'In-store account' : 'Workcover', contactID: index % 3 + 1, invoiceNumber: `2026070${String(1483 + index * 151).slice(-4)}`, invDate: `2026-07-${String(index % 28 + 1).padStart(2, '0')}` as AccountPayment['invDate'], dueDate: '2026-07-31', description: names[index % names.length], unitAmount: [94.19, 37.97, 19.74, 25, 293.18, 80.95, 167.23, 200][index % 8], accountAdjusted: true }))

export function PaymentsPage() {
  const [month, setMonth] = useState('2026-07')
  const [rows, setRows] = useState([...referencePayments, ...seededPayments])
  const [panel, setPanel] = useState(false)
  const [original, setOriginal] = useState('')
  const [form, setForm] = useState(emptyPayment)
  const [contactOpen, setContactOpen] = useState(false)
  const [contactSearch, setContactSearch] = useState('')
  const contactRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    if (!contactOpen) return
    const close = (event: MouseEvent) => !contactRef.current?.contains(event.target as Node) && setContactOpen(false)
    const escape = (event: KeyboardEvent) => event.key === 'Escape' && setContactOpen(false)
    document.addEventListener('mousedown', close)
    document.addEventListener('keydown', escape)
    return () => { document.removeEventListener('mousedown', close); document.removeEventListener('keydown', escape) }
  }, [contactOpen])
  const open = (value?: AccountPayment) => { setForm(value ?? { ...emptyPayment, invDate: `${month}-01` as AccountPayment['invDate'] }); setOriginal(value?.invoiceNumber ?? ''); setPanel(true) }
  const save = (event: FormEvent) => { event.preventDefault(); setRows((current) => original ? current.map((row) => row.invoiceNumber === original ? form : row) : [...current, form]); setPanel(false) }
  const remove = () => { setRows((current) => current.filter((row) => row.invoiceNumber !== original)); setPanel(false) }
  const filteredRows = rows.filter((row) => row.invDate.startsWith(month))
  const totals = useMemo(() => filteredRows.reduce<Record<string, number>>((result, row) => ({ ...result, [row.contactName]: (result[row.contactName] ?? 0) + paymentTotal(row.quantity, row.unitAmount) }), {}), [filteredRows])
  const total = Object.values(totals).reduce((sum, value) => sum + value, 0)
  return <div className="page desktop-data-page payments-page"><MonthToolbar value={month} onChange={setMonth} actions={<Button variant="secondary" className="xero-button" onClick={() => downloadText(`account-payments-${month}-xero.csv`, toCsv(rows))}><span className="xero-mark">xero</span>Export in Xero Format</Button>} /><div className="payments-layout"><Card className="desktop-table-card totals-card"><header><h1>Totals:</h1></header><DataTable rows={Object.entries(totals).map(([contactName, amount]) => ({ contactName, amount }))} columns={[{ key: 'contactName', label: 'CONTACT' }, { key: 'amount', label: 'TOTAL', render: (row) => currency.format(row.amount) }]} /><footer><span>All Suppliers:</span><b>{currency.format(total)}</b></footer></Card><Card className="desktop-table-card payments-table-card"><header><h1>Payments List:</h1></header><DataTable rows={filteredRows} onRowClick={open} columns={[{ key: 'contactName', label: 'CONTACT NAME' }, { key: 'invoiceNumber', label: 'INVOICE NUMBER' }, { key: 'invDate', label: 'INVOICE DATE', render: (row: AccountPayment) => displayDate(row.invDate) }, { key: 'dueDate', label: 'DUE DATE', render: (row: AccountPayment) => displayDate(row.dueDate) }, { key: 'description', label: 'DESCRIPTION' }, { key: 'unitAmount', label: 'UNIT AMOUNT', render: (row: AccountPayment) => currency.format(paymentTotal(row.quantity, row.unitAmount)) }, { key: 'accountAdjusted', label: <>ACCOUNT<br />ADJUSTED?</>, render: (row: AccountPayment) => row.accountAdjusted ? 'Y' : 'N' }]} /></Card></div><FloatingButton label="Add payment" onClick={() => open()} />
    <SidePanel title={`${original ? 'Edit' : 'Add'} account payment`} open={panel} onClose={() => setPanel(false)} onDelete={original ? remove : undefined}><form className="form-stack desktop-side-form" onSubmit={save}><div className={`contact-combo ${contactOpen ? 'contact-combo--open' : ''}`} ref={contactRef}><label className="field"><span>Contact name</span><button type="button" className="contact-trigger" role="combobox" aria-label="Supplier" aria-haspopup="listbox" aria-expanded={contactOpen} onClick={() => { setContactSearch(''); setContactOpen((open) => !open) }}><span>{form.contactName || 'Select contact'}</span><span aria-hidden="true">⌄</span></button></label>{contactOpen && <div className="contact-popup"><input autoFocus aria-label="Search contacts" placeholder="Search..." value={contactSearch} onChange={(event) => setContactSearch(event.target.value)} /><div role="listbox" aria-label="Contacts">{contacts.filter((contact) => contact.toLowerCase().includes(contactSearch.toLowerCase())).map((contact) => <button type="button" role="option" aria-selected={form.contactName === contact} key={contact} onClick={() => { setForm({ ...form, contactName: contact, contactID: contacts.indexOf(contact) + 1 }); setContactOpen(false) }}>{contact}</button>)}</div><footer><button type="button">Create New</button><button type="button">Manage Contacts</button></footer></div>}</div><Field label="Invoice Number" value={form.invoiceNumber} onChange={(event) => setForm({ ...form, invoiceNumber: event.target.value })} required /><Field label="Invoice Date" type="date" value={form.invDate} onChange={(event) => setForm({ ...form, invDate: event.target.value as AccountPayment['invDate'] })} required /><Field label="Due Date" type="date" value={form.dueDate} onChange={(event) => setForm({ ...form, dueDate: event.target.value as AccountPayment['dueDate'] })} required /><TextArea label="Description" rows={1} value={form.description} onChange={(event) => setForm({ ...form, description: event.target.value })} /><Field label="Unit Amount ($)" type="number" min="0" step="0.01" value={form.unitAmount} onChange={(event) => setForm({ ...form, unitAmount: Number(event.target.value) })} /><Toggle label="Account Adjusted?" checked={form.accountAdjusted} onChange={(accountAdjusted) => setForm({ ...form, accountAdjusted })} /><SelectField label="Tax Rate" value={form.taxRate} onChange={(event) => setForm({ ...form, taxRate: event.target.value })}><option>Gst Free Income</option><option>GST on Income</option></SelectField><Button type="submit">Save</Button></form></SidePanel>
  </div>
}

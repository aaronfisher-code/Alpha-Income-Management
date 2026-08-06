import { useEffect, useId, useLayoutEffect, useRef, useState, type ButtonHTMLAttributes, type InputHTMLAttributes, type ReactNode, type SelectHTMLAttributes } from 'react'
import { changeMonth, displayMonth } from '../utils/format'
import { Icon } from './Icon'

export function Button({ variant = 'primary', icon, children, className = '', ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'primary' | 'secondary' | 'danger' | 'ghost'; icon?: string }) {
  return <button className={`button button--${variant} ${className}`} {...props}>{icon && <Icon name={icon} />}{children}</button>
}

export function Field({ label, error, ...props }: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string }) {
  const id = useId()
  return <label className={`field ${error ? 'field--error' : ''}`} htmlFor={id}><span>{label}</span><input id={id} {...props} />{error && <small>{error}</small>}</label>
}

export function SelectField({ label, children, ...props }: SelectHTMLAttributes<HTMLSelectElement> & { label: string; children: ReactNode }) {
  const id = useId()
  return <label className="field" htmlFor={id}><span>{label}</span><select id={id} {...props}>{children}</select></label>
}

export function TextArea({ label, ...props }: React.TextareaHTMLAttributes<HTMLTextAreaElement> & { label: string }) {
  const id = useId()
  return <label className="field" htmlFor={id}><span>{label}</span><textarea id={id} {...props} /></label>
}

export function Card({ children, className = '' }: { children: ReactNode; className?: string }) { return <section className={`card ${className}`}>{children}</section> }

export function MonthSelector({ value, onChange, busy = false }: { value: string; onChange: (value: string) => void; busy?: boolean }) {
  const [open, setOpen] = useState(false)
  const rootRef = useRef<HTMLDivElement>(null)
  const [year, month] = value.split('-').map(Number)
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
  useEffect(() => {
    if (!open) return
    const close = (event: MouseEvent) => !rootRef.current?.contains(event.target as Node) && setOpen(false)
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [open])
  return <div className="month-selector" ref={rootRef}><span>NOW SHOWING THE MONTH OF:</span><div className="month-picker-wrap"><button className="month-picker-trigger" aria-label="Select month" aria-expanded={open} onClick={() => setOpen((visible) => !visible)}><b>{displayMonth(value)}</b><Icon name="calendar" /></button>{open && <div className="month-popover"><header><button aria-label="Previous year" onClick={() => onChange(`${year - 1}-${String(month).padStart(2, '0')}`)}>‹</button><span>{year}</span><button aria-label="Next year" onClick={() => onChange(`${year + 1}-${String(month).padStart(2, '0')}`)}>›</button></header><div>{months.map((name, index) => <button className={month === index + 1 ? 'active' : ''} key={name} onClick={() => { onChange(`${year}-${String(index + 1).padStart(2, '0')}`); setOpen(false) }}>{name}</button>)}</div></div>}</div><button className="month-step" aria-label="Previous month" onClick={() => onChange(changeMonth(value, -1))}>‹</button><button className="month-step" aria-label="Next month" onClick={() => onChange(changeMonth(value, 1))}>›</button>{busy && <span className="spinner" aria-label="Loading" />}</div>
}

export function MonthToolbar({ value, onChange, actions }: { value: string; onChange: (value: string) => void; actions?: ReactNode }) {
  return <div className="desktop-toolbar"><MonthSelector value={value} onChange={onChange} /><div className="desktop-toolbar__actions">{actions}</div></div>
}

export function PageHeader({ title, subtitle, month, setMonth, actions }: { title: string; subtitle?: string; month?: string; setMonth?: (value: string) => void; actions?: ReactNode }) {
  return <header className="page-header"><div><h1>{title}</h1>{subtitle && <p>{subtitle}</p>}</div>{month && setMonth && <MonthSelector value={month} onChange={setMonth} />}<div className="page-actions">{actions}</div></header>
}

type DataTableColumn = { key: string; label: ReactNode; render?: (row: any) => ReactNode; className?: string; minWidth?: number; grow?: boolean }

function minimumColumnWidth(column: DataTableColumn) {
  if (column.minWidth) return column.minWidth
  const key = column.key.toLowerCase()
  if (/notes?|description|reason/.test(key)) return 160
  if (/date/.test(key)) return 88
  if (/name|supplier|contact|role/.test(key)) return 112
  if (/invoice|reference|number|(^|_)id$/.test(key)) return 100
  if (/amount|balance|income|profit|wages|outgoings|credits|percentage|customer|items|scripts|meds|cash|eftpos|amex|cheque|stock|sms|duration|total/.test(key)) return 82
  return 88
}

function flexibleColumnIndex(columns: DataTableColumn[]) {
  const explicit = columns.findIndex((column) => column.grow)
  if (explicit >= 0) return explicit
  const notes = columns.findIndex((column) => /notes?|description|reason/i.test(column.key))
  if (notes >= 0) return notes
  const text = columns.findIndex((column) => /name|supplier|contact|role/i.test(column.key))
  return text >= 0 ? text : Math.max(columns.length - 1, 0)
}

function defaultColumnWidths(columns: DataTableColumn[], availableWidth: number) {
  const widths = columns.map(minimumColumnWidth)
  const spareWidth = Math.max(0, availableWidth - widths.reduce((total, width) => total + width, 0))
  if (widths.length) widths[flexibleColumnIndex(columns)] += spareWidth
  return widths
}

function readableColumnName(key: string) {
  return key.replace(/([a-z])([A-Z])/g, '$1 $2').replace(/[_-]+/g, ' ').toLowerCase()
}

export function DataTable({ columns, rows, onRowClick, rowClassName, empty = 'No records found', wrapperClassName = '', tableClassName = '' }: { columns: DataTableColumn[]; rows: any[]; onRowClick?: (row: any) => void; rowClassName?: (row: any) => string; empty?: string; wrapperClassName?: string; tableClassName?: string }) {
  const scrollRef = useRef<HTMLDivElement>(null)
  const userSized = useRef(false)
  const signature = columns.map((column) => `${column.key}:${column.minWidth ?? ''}:${column.grow ?? ''}`).join('|')
  const [widths, setWidths] = useState<number[]>([])

  useLayoutEffect(() => {
    userSized.current = false
    const sizeColumns = () => {
      if (userSized.current || !scrollRef.current) return
      const next = defaultColumnWidths(columns, scrollRef.current.clientWidth)
      setWidths((current) => current.length === next.length && current.every((width, index) => width === next[index]) ? current : next)
    }
    sizeColumns()
    if (typeof ResizeObserver === 'undefined' || !scrollRef.current) return
    const observer = new ResizeObserver(sizeColumns)
    observer.observe(scrollRef.current)
    return () => observer.disconnect()
  }, [signature])

  const appliedWidths = widths.length === columns.length ? widths : defaultColumnWidths(columns, 0)
  const resizeColumn = (index: number, width: number) => {
    userSized.current = true
    setWidths((current) => {
      const next = current.length === columns.length ? [...current] : [...appliedWidths]
      next[index] = Math.max(minimumColumnWidth(columns[index]), Math.round(width))
      return next
    })
  }
  const beginResize = (event: React.PointerEvent<HTMLButtonElement>, index: number) => {
    event.preventDefault()
    event.stopPropagation()
    const startX = event.clientX
    const startWidth = appliedWidths[index]
    document.body.classList.add('table-column-resizing')
    const move = (pointerEvent: PointerEvent) => resizeColumn(index, startWidth + pointerEvent.clientX - startX)
    const stop = () => {
      document.body.classList.remove('table-column-resizing')
      window.removeEventListener('pointermove', move)
      window.removeEventListener('pointerup', stop)
      window.removeEventListener('pointercancel', stop)
    }
    window.addEventListener('pointermove', move)
    window.addEventListener('pointerup', stop)
    window.addEventListener('pointercancel', stop)
  }
  const resizeWithKeyboard = (event: React.KeyboardEvent<HTMLButtonElement>, index: number) => {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
    event.preventDefault()
    const direction = event.key === 'ArrowRight' ? 1 : -1
    resizeColumn(index, appliedWidths[index] + direction * (event.shiftKey ? 25 : 10))
  }
  const tableWidth = appliedWidths.reduce((total, width) => total + width, 0)

  return <div className={`table-scroll ${wrapperClassName}`} ref={scrollRef}><table className={`resizable-table ${tableClassName}`} style={{ width: tableWidth, minWidth: '100%' }}><colgroup>{columns.map((column, index) => <col key={column.key} style={{ width: appliedWidths[index] }} />)}</colgroup><thead><tr>{columns.map((column, index) => <th key={column.key} className={column.className}><span className="column-label">{column.label}</span><button type="button" className="column-resizer" aria-label={`Resize ${readableColumnName(column.key)} column`} onPointerDown={(event) => beginResize(event, index)} onKeyDown={(event) => resizeWithKeyboard(event, index)} /></th>)}</tr></thead><tbody>{rows.length ? rows.map((row, index) => <tr className={rowClassName?.(row)} key={row.id ?? row.userID ?? row.storeID ?? row.shiftID ?? row.invoiceNo ?? row.invoiceNumber ?? row.date ?? index} onDoubleClick={() => onRowClick?.(row)} tabIndex={onRowClick ? 0 : undefined} onKeyDown={(event) => event.key === 'Enter' && onRowClick?.(row)}>{columns.map((column) => <td key={column.key} className={column.className}>{column.render ? column.render(row) : row[column.key]}</td>)}</tr>) : <tr><td colSpan={columns.length} className="empty-cell">{empty}</td></tr>}</tbody></table></div>
}

export function Modal({ title, open, onClose, children, width = 520 }: { title: string; open: boolean; onClose: () => void; children: ReactNode; width?: number }) {
  const closeRef = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    if (!open) return
    closeRef.current?.focus()
    const key = (event: KeyboardEvent) => event.key === 'Escape' && onClose()
    document.addEventListener('keydown', key)
    return () => document.removeEventListener('keydown', key)
  }, [open, onClose])
  if (!open) return null
  return <div className="overlay" role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><section className="modal" role="dialog" aria-modal="true" aria-label={title} style={{ maxWidth: width }}><header><h2>{title}</h2><button ref={closeRef} className="icon-button" aria-label="Close" onClick={onClose}><Icon name="close" /></button></header>{children}</section></div>
}

export function SidePanel({ title, open, onClose, onDelete, onBack, children }: { title: string; open: boolean; onClose: () => void; onDelete?: () => void; onBack?: () => void; children: ReactNode }) {
  const [mounted, setMounted] = useState(open)
  const [closing, setClosing] = useState(false)

  useEffect(() => {
    if (open) {
      setMounted(true)
      setClosing(false)
      return
    }
    if (!mounted) return
    setClosing(true)
    const timeout = window.setTimeout(() => {
      setMounted(false)
      setClosing(false)
    }, 170)
    return () => window.clearTimeout(timeout)
  }, [open, mounted])

  if (!mounted) return null
  return <div className={`overlay panel-overlay ${closing ? 'panel-overlay--closing' : ''}`} role="presentation" onMouseDown={(event) => event.target === event.currentTarget && onClose()}><aside className="side-panel" role="dialog" aria-modal="true" aria-label={title}><header>{onBack && <button className="icon-button panel-back" aria-label="Back" onClick={onBack}>‹</button>}<h2>{title}</h2><span className="panel-header-spacer" />{onDelete && <button className="icon-button" aria-label="Delete" onClick={onDelete}><Icon name="trash" /></button>}<button className="icon-button" aria-label="Close" onClick={onClose}><Icon name="close" /></button></header><div className="side-panel__body">{children}</div></aside></div>
}

export function FloatingButton({ label, onClick }: { label: string; onClick: () => void }) { return <button className="floating-button" aria-label={label} onClick={onClick}><Icon name="plus" /></button> }

export function Notice({ kind = 'info', children }: { kind?: 'info' | 'error' | 'success'; children: ReactNode }) { return <div className={`notice notice--${kind}`} role={kind === 'error' ? 'alert' : 'status'}>{children}</div> }

export function Toggle({ label, checked, onChange }: { label: string; checked: boolean; onChange: (checked: boolean) => void }) { return <label className="toggle"><input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} /><span />{label}</label> }

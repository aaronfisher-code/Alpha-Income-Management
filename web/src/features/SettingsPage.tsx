import { useState, type FormEvent } from 'react'
import { api } from '../api/client'
import { Button, Card, Notice, PageHeader } from '../components/UI'

export function SettingsPage() {
  const [files, setFiles] = useState<File[]>([])
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (!files.length) return setMessage('Select at least one spreadsheet.')
    setBusy(true)
    if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCKS !== 'false') { await new Promise((resolve) => setTimeout(resolve, 500)); setMessage(`${files.length} spreadsheet(s) validated and ready for server import.`) }
    else { try { const result = await api.uploadLegacy(files); setMessage(`Imported ${result.imported} records${result.warnings.length ? ` with ${result.warnings.length} warning(s)` : ''}.`) } catch (error) { setMessage(error instanceof Error ? error.message : 'Import failed') } }
    setBusy(false)
  }
  return <div className="page"><PageHeader title="Settings & Import Tools" subtitle="Administrative tools for authenticated users" /><Card className="settings-card"><div><h2>Legacy spreadsheet import</h2><p>Upload legacy Alpha Income spreadsheets through the secured server importer. Files are validated before any records are changed.</p><Notice>Direct database access has been removed from the client. Production imports require the authenticated <code>POST /legacy-import</code> endpoint.</Notice></div><form className="upload-box" onSubmit={submit}><label><input type="file" multiple accept=".xlsx,.xls,.csv" onChange={(e) => setFiles(Array.from(e.target.files ?? []))} /><span>Drop spreadsheets here or choose files</span><small>{files.length ? files.map((file) => file.name).join(', ') : 'XLSX, XLS or CSV'}</small></label><Button type="submit" icon="upload" disabled={busy}>{busy ? 'Importing…' : 'Start secure import'}</Button>{message && <Notice kind={message.includes('failed') || message.startsWith('Select') ? 'error' : 'success'}>{message}</Notice>}</form></Card></div>
}

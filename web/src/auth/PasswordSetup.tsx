import { useState, type FormEvent } from 'react'
import { Button, Field, Modal, Notice } from '../components/UI'
import { useAuth } from './AuthContext'

export function PasswordSetup() {
  const { session, changePassword } = useAuth()
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (event: FormEvent) => {
    event.preventDefault()
    if (password.length < 10) return setError('Use at least 10 characters')
    if (password !== confirm) return setError('Passwords do not match')
    setBusy(true); setError('')
    try { await changePassword(password) } catch { setError('Password could not be changed') } finally { setBusy(false) }
  }
  return <Modal title="Create a new password" open={Boolean(session?.firstLogin)} onClose={() => {}}>
    <form className="form-stack" onSubmit={submit}>
      <p>This is your first sign in. Set a private password before continuing.</p>
      {error && <Notice kind="error">{error}</Notice>}
      <Field label="New password" type="password" autoComplete="new-password" value={password} onChange={(e) => setPassword(e.target.value)} />
      <Field label="Confirm password" type="password" autoComplete="new-password" value={confirm} onChange={(e) => setConfirm(e.target.value)} />
      <Button type="submit" disabled={busy}>Save password</Button>
    </form>
  </Modal>
}

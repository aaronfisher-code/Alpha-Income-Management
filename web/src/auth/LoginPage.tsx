import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { Button, Field, Notice } from '../components/UI'
import { useAuth } from './AuthContext'

export function LoginPage() {
  const { session, login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  if (session) return <Navigate to="/targets" replace />

  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError(''); setBusy(true)
    try { await login(username, password) } catch (reason) { setError(reason instanceof Error ? reason.message : 'Login failed') } finally { setBusy(false) }
  }

  return <main className="login-page">
    <div className="login-art" aria-hidden="true"><div className="login-brand"><img src="/images/alpha-logo.png" alt="" /><span>Alpha</span></div></div>
    <section className="login-form-wrap">
      <form className="login-form" onSubmit={submit}>
        <h1>Welcome to Alpha! <span aria-hidden="true">👋</span></h1>
        <p>Enter your details below to sign in</p>
        {error && <Notice kind="error">{error}</Notice>}
        <Field label="Username" placeholder="Enter your username" autoComplete="username" value={username} onChange={(event) => setUsername(event.target.value)} required />
        <Field label="Password" type="password" placeholder="Enter your password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
        <Button type="submit" disabled={busy}>{busy ? <><span className="spinner spinner--light" /> Signing in…</> : 'Sign in'}</Button>
      </form>
    </section>
  </main>
}

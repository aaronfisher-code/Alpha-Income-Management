import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api } from '../api/client'
import { mockSession } from '../api/mockData'
import type { AuthSession } from '../types'

const mocksEnabled = import.meta.env.DEV ? import.meta.env.VITE_USE_MOCKS !== 'false' : import.meta.env.VITE_USE_MOCKS === 'true'

interface AuthValue {
  session: AuthSession | null
  loading: boolean
  login: (username: string, password: string) => Promise<void>
  logout: () => Promise<void>
  changePassword: (password: string) => Promise<void>
}

const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (mocksEnabled) {
      if (sessionStorage.getItem('alpha-demo-session')) setSession(mockSession(false))
      setLoading(false)
      return
    }
    api.auth.session().then(setSession).catch(() => setSession(null)).finally(() => setLoading(false))
  }, [])

  const login = useCallback(async (username: string, password: string) => {
    if (mocksEnabled) {
      await new Promise((resolve) => window.setTimeout(resolve, 350))
      if (!username.trim() || !password) throw new Error('Enter your username and password')
      const next = mockSession(username.toLowerCase() === 'firstlogin')
      sessionStorage.setItem('alpha-demo-session', '1')
      setSession(next)
    } else setSession(await api.auth.login(username, password))
  }, [])

  const logout = useCallback(async () => {
    if (!mocksEnabled) await api.auth.logout()
    sessionStorage.removeItem('alpha-demo-session')
    setSession(null)
  }, [])

  const changePassword = useCallback(async (password: string) => {
    if (!mocksEnabled) await api.auth.changePassword(password)
    setSession((current) => current ? { ...current, firstLogin: false } : current)
  }, [])

  const value = useMemo(() => ({ session, loading, login, logout, changePassword }), [session, loading, login, logout, changePassword])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used within AuthProvider')
  return value
}

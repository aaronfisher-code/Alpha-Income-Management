import { createContext, useContext, useMemo, useState } from 'react'
import { NavLink, Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { PasswordSetup } from '../auth/PasswordSetup'
import type { Store } from '../types'
import { initials } from '../utils/format'
import { Icon } from './Icon'

const navigation = [
  ['targets', 'Staff Targets', 'targets'], ['eod', 'EOD Data Entry', 'eod'], ['payments', 'Account Payments', 'payments'],
  ['roster', 'Staff Roster', 'roster'], ['employees', 'Manage Employees', 'employees'], ['invoices', 'Invoice Tracking', 'invoices'],
  ['bas', 'BAS Checker', 'bas'], ['budget', 'Budget & Expenses', 'budget'], ['summary', 'Monthly Summary', 'summary'], ['settings', 'Settings', 'settings']
] as const

interface AppState { store: Store; setStore: (store: Store) => void }
const AppStateContext = createContext<AppState | null>(null)
export const useAppState = () => {
  const state = useContext(AppStateContext)
  if (!state) throw new Error('useAppState must be used in AppShell')
  return state
}

export function AppShell() {
  const { session, loading, logout } = useAuth()
  const [store, setStore] = useState<Store | null>(session?.stores[0] ?? null)
  const [menu, setMenu] = useState(false)
  const [navExpanded, setNavExpanded] = useState(false)
  const [profile, setProfile] = useState(false)
  const value = useMemo(() => store ? { store, setStore } : null, [store])
  if (loading) return <div className="app-loader"><img src="/images/alpha-logo.png" alt="Alpha Income" /><span className="spinner" /></div>
  if (!session) return <Navigate to="/login" replace />
  if (!store || !value) return null
  const permissionSet = new Set(session.permissions.map((item) => item.permissionName))

  return <AppStateContext.Provider value={value}>
    <div className={`app-shell ${menu ? 'app-shell--menu' : ''} ${navExpanded ? 'app-shell--nav-expanded' : ''}`}>
      <header className="topbar">
        <button className="mobile-menu" onClick={() => setMenu((open) => !open)} aria-label="Toggle navigation"><Icon name="menu" /></button>
        <label className="store-select"><span className="sr-only">Current store</span><select value={store.storeID} onChange={(event) => setStore(session.stores.find((item) => item.storeID === Number(event.target.value)) ?? store)}>{session.stores.map((item) => <option key={item.storeID} value={item.storeID}>{item.storeName}</option>)}</select></label>
        <div className="profile-wrap">
          <button className="profile-button" onClick={() => setProfile((open) => !open)} aria-expanded={profile}>
            <span className="avatar" style={{ backgroundColor: session.user.bgColour, color: session.user.textColour }}>{initials(session.user.first_name, session.user.last_name).slice(0, 1)}</span>
            <span><b>{session.user.first_name} {session.user.last_name}</b><small>{session.user.role}</small></span><span aria-hidden="true">⌄</span>
          </button>
          {profile && <div className="profile-menu"><button onClick={() => void logout()}><Icon name="logout" /> Logout</button></div>}
        </div>
      </header>
      {(menu || navExpanded) && <button className="nav-backdrop" aria-label="Close navigation" onClick={() => { setMenu(false); setNavExpanded(false) }} />}
      <aside className="sidebar" onMouseEnter={() => setNavExpanded(true)} onMouseLeave={() => { setMenu(false); setNavExpanded(false) }}>
        <div className="brand"><img src="/images/alpha-logo.png" alt="" /><span>Alpha</span></div>
        <nav aria-label="Primary navigation">{navigation.filter(([, label]) => permissionSet.has(label)).map(([path, label, icon]) => <NavLink key={path} to={`/${path}`} onClick={() => { setMenu(false); setNavExpanded(false) }}><Icon name={icon} /><span>{label}</span></NavLink>)}</nav>
      </aside>
      <main className="page-content"><Outlet /></main>
      <PasswordSetup />
    </div>
  </AppStateContext.Provider>
}

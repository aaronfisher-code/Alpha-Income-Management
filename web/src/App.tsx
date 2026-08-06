import { Navigate, Route, Routes } from 'react-router-dom'
import { LoginPage } from './auth/LoginPage'
import { AppShell } from './components/AppShell'
import { BASPage } from './features/BASPage'
import { BudgetPage } from './features/BudgetPage'
import { EmployeesPage } from './features/EmployeesPage'
import { EODPage } from './features/EODPage'
import { InvoicesPage } from './features/InvoicesPage'
import { PaymentsPage } from './features/PaymentsPage'
import { RosterPage } from './features/RosterPage'
import { SettingsPage } from './features/SettingsPage'
import { SummaryPage } from './features/SummaryPage'
import { TargetsPage } from './features/TargetsPage'

export default function App() {
  return <Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route element={<AppShell />}>
      <Route index element={<Navigate to="/targets" replace />} />
      <Route path="targets" element={<TargetsPage />} />
      <Route path="eod" element={<EODPage />} />
      <Route path="payments" element={<PaymentsPage />} />
      <Route path="roster" element={<RosterPage />} />
      <Route path="employees" element={<EmployeesPage />} />
      <Route path="invoices" element={<InvoicesPage />} />
      <Route path="bas" element={<BASPage />} />
      <Route path="budget" element={<BudgetPage />} />
      <Route path="summary" element={<SummaryPage />} />
      <Route path="settings" element={<SettingsPage />} />
    </Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
}

import { api } from './client'
import type { AccountPayment, AccountPaymentContact, BASCheck, BudgetExpenses, Credit, EODDataPoint, Employment, Invoice, InvoiceSupplier, LeaveRequest, Permission, Shift, SpecialDate, Store, Target, TillReportDataPoint, User, UserPermissionDTO } from '../types'

const encoded = (value: string | number) => encodeURIComponent(String(value))
type Range = { storeId: number; startDate: string; endDate: string }
type Month = { storeId: number; yearMonth: string }

export const resources = {
  stores: {
    all: () => api.get<Store[]>('/stores'), create: (body: Store) => api.post<Store>('/stores', body),
    update: (body: Store) => api.put<void>(`/stores/${body.storeID}`, body), remove: (id: number) => api.delete<void>(`/stores/${id}`)
  },
  users: {
    all: () => api.get<User[]>('/users'), byId: (id: number) => api.get<User>(`/users/by-id/${id}`), byUsername: (username: string) => api.get<User>(`/users/by-username/${encoded(username)}`),
    byStore: (storeId: number) => api.get<User[]>(`/users/store/${storeId}`), create: (body: User) => api.post<User>('/users', body),
    update: (body: User) => api.put<void>(`/users/${encoded(body.username)}`, body), remove: (id: number) => api.delete<void>(`/users/${id}`),
    resetPassword: (id: number) => api.post<void>(`/users/${id}/reset-password`, {}), permissions: (id: number) => api.get<Permission[]>(`/users/${id}/permissions`),
    stores: (id: number) => api.get<Store[]>(`/users/${id}/stores`), employments: (id: number) => api.get<Employment[]>(`/users/${id}/employments`)
  },
  permissions: { all: () => api.get<Permission[]>('/permissions'), replace: async (userId: number, values: UserPermissionDTO[]) => { await api.delete<void>(`/user-permissions/${userId}`); return api.post<void>('/user-permissions', values) } },
  employments: { create: (values: Employment[]) => api.post<void>('/employments', values), removeUser: (userId: number) => api.delete<void>(`/employments/${userId}`) },
  eod: { list: (range: Range) => api.get<EODDataPoint[]>('/eod', range), create: (body: EODDataPoint) => api.post<EODDataPoint>('/eod', body), update: (body: EODDataPoint) => api.put<void>('/eod', body) },
  tillReports: { list: (range: Range) => api.get<TillReportDataPoint[]>('/till-report', range), byKey: (range: Range & { key: string }) => api.get<TillReportDataPoint[]>('/till-report/by-key', range), import: (rows: TillReportDataPoint[]) => api.post<void>('/till-report/import', rows) },
  targets: { list: (month: Month) => api.get<Target[]>('/targets', month), byKey: (range: Range & { key: string }) => api.get<Target[]>('/targets/targets-by-key', range), save: (body: Target) => api.post<void>('/targets', body) },
  payments: {
    list: (month: Month) => api.get<AccountPayment[]>('/account-payments', month), create: (body: AccountPayment) => api.post<void>('/account-payments', body),
    update: (original: string, body: AccountPayment) => api.put<void>(`/account-payments/${encoded(original)}`, body),
    remove: (storeId: number, invoiceNo: string) => api.delete<void>(`/account-payments/${storeId}/${encoded(invoiceNo)}`), total: (month: Month & { type: string }) => api.get<number>('/account-payments/total', month)
  },
  paymentContacts: {
    list: (storeId: number) => api.get<AccountPaymentContact[]>('/account-payment-contacts', { storeId }), create: (body: AccountPaymentContact) => api.post<void>('/account-payment-contacts', body),
    update: (body: AccountPaymentContact) => api.put<void>(`/account-payment-contacts/${body.contactID}`, body), remove: (id: number) => api.delete<void>(`/account-payment-contacts/${id}`)
  },
  invoices: {
    list: (month: Month) => api.get<Invoice[]>('/invoices', month), table: (month: Month) => api.get<Invoice[]>('/invoices/table-data', month), total: (month: Month) => api.get<number>('/invoices/total', month),
    create: (body: Invoice) => api.post<void>('/invoices', body), update: (original: string, body: Invoice) => api.put<void>(`/invoices/${encoded(original)}?storeId=${body.storeID}&supplierId=${body.supplierID}`, body),
    remove: (invoiceNo: string, storeId: number, supplierId: number) => api.delete<void>(`/invoices/${encoded(invoiceNo)}`, { storeId, supplierId })
  },
  credits: { list: (month: Month) => api.get<Credit[]>('/credits', month), create: (body: Credit) => api.post<void>('/credits', body), update: (body: Credit) => api.put<void>(`/credits/${body.creditID}`, body), remove: (id: number) => api.delete<void>(`/credits/${id}`) },
  suppliers: { list: (storeId: number) => api.get<InvoiceSupplier[]>('/invoice-suppliers', { storeId }), create: (body: InvoiceSupplier) => api.post<void>('/invoice-suppliers', body), update: (body: InvoiceSupplier) => api.put<void>(`/invoice-suppliers/${body.contactID}`, body), remove: (id: number) => api.delete<void>(`/invoice-suppliers/${id}`) },
  roster: {
    shifts: (range: Range) => api.get<Shift[]>('/roster/shifts', range), modifications: (range: Range) => api.get<Shift[]>('/roster/shift-modifications', range), create: (body: Shift) => api.post<void>('/roster/shifts', body),
    update: (body: Shift) => api.put<void>(`/roster/shifts/${body.shiftID}`, body), remove: (id: number) => api.delete<void>(`/roster/shifts/${id}`), createModification: (body: Shift) => api.post<void>('/roster/shift-modifications', body),
    removeModifications: (id: number, cutoffDate?: string) => cutoffDate ? api.delete<void>(`/roster/shift-modifications/${id}/cutoff`, { cutoffDate }) : api.delete<void>(`/roster/shift-modifications/${id}`),
    updateEndDate: (id: number, endDate: string) => api.put<void>(`/roster/shifts/${id}/end-date?endDate=${encoded(endDate)}`, {}), specialDates: (startDate: string, endDate: string) => api.get<SpecialDate[]>('/roster/special-dates', { startDate, endDate }), saveSpecialDate: (body: SpecialDate) => body.eventID ? api.put<void>('/roster/special-date', body) : api.post<void>('/roster/special-date', body)
  },
  leave: { list: (range: Range) => api.get<LeaveRequest[]>('/leave', range), create: (body: LeaveRequest) => api.post<void>('/leave', body), update: (body: LeaveRequest) => api.put<void>(`/leave/${body.leaveID}`, body), remove: (id: number) => api.delete<void>(`/leave/${id}`) },
  bas: { get: (month: Month) => api.get<BASCheck>('/bas-checker', month), save: (body: BASCheck) => api.post<void>('/bas-checker', body) },
  budget: { get: (month: Month) => api.get<BudgetExpenses>('/budget-expenses', month), save: (body: BudgetExpenses) => api.post<void>('/budget-expenses', body) }
}

import type { AuthSession } from '../types'

const API_BASE = (import.meta.env.VITE_API_BASE_URL || '/api').replace(/\/$/, '')
const prototypeToken = import.meta.env.DEV ? import.meta.env.VITE_API_TOKEN : undefined

export class ApiError extends Error {
  constructor(message: string, public status: number, public details?: unknown) {
    super(message)
    this.name = 'ApiError'
  }
}

const csrfToken = () => document.cookie
  .split('; ')
  .find((entry) => entry.startsWith('XSRF-TOKEN='))
  ?.split('=')[1]

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = init.method?.toUpperCase() || 'GET'
  const headers = new Headers(init.headers)
  if (init.body && !(init.body instanceof FormData)) headers.set('Content-Type', 'application/json')
  if (prototypeToken) headers.set('Authorization', `Bearer ${prototypeToken}`)
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    const token = csrfToken()
    if (token) headers.set('X-XSRF-TOKEN', decodeURIComponent(token))
  }
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers, credentials: 'include' })
  if (response.status === 204) return undefined as T
  const contentType = response.headers.get('content-type') || ''
  const body = contentType.includes('application/json') ? await response.json() : await response.text()
  if (!response.ok) {
    const message = typeof body === 'object' && body && 'message' in body ? String(body.message) : `Request failed (${response.status})`
    throw new ApiError(message, response.status, body)
  }
  return body as T
}

export const query = (params: Record<string, string | number | boolean | null | undefined>) => {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null) search.set(key, String(value))
  })
  const encoded = search.toString()
  return encoded ? `?${encoded}` : ''
}

export const api = {
  auth: {
    login: (username: string, password: string) => request<AuthSession>('/auth/login', { method: 'POST', body: JSON.stringify({ username, password }) }),
    session: () => request<AuthSession>('/auth/session'),
    logout: () => request<void>('/auth/logout', { method: 'POST' }),
    changePassword: (password: string) => request<void>('/auth/password', { method: 'PUT', body: JSON.stringify({ password }) })
  },
  get: <T>(path: string, params: Record<string, string | number | boolean | null | undefined> = {}) => request<T>(`${path}${query(params)}`),
  post: <T>(path: string, body: unknown) => request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) => request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  delete: <T>(path: string, params: Record<string, string | number | boolean | null | undefined> = {}) => request<T>(`${path}${query(params)}`, { method: 'DELETE' }),
  uploadLegacy: (files: File[]) => {
    const form = new FormData()
    files.forEach((file) => form.append('files', file))
    return request<{ imported: number; warnings: string[] }>('/legacy-import', { method: 'POST', body: form })
  }
}

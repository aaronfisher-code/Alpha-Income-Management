import { api, query, request } from './client'
import { resources } from './resources'

describe('typed API client', () => {
  beforeEach(() => vi.restoreAllMocks())
  it('serializes query parameters consistently', () => {
    expect(query({ storeId: 4, yearMonth: '2026-07', ignored: null })).toBe('?storeId=4&yearMonth=2026-07')
  })
  it('uses credentials and maps JSON responses', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify([{ storeID: 1 }]), { status: 200, headers: { 'content-type': 'application/json' } }))
    await expect(request('/stores')).resolves.toEqual([{ storeID: 1 }])
    expect(fetchMock).toHaveBeenCalledWith('/api/stores', expect.objectContaining({ credentials: 'include' }))
  })
  it('sends login using the new session endpoint contract', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(JSON.stringify({ user: {}, stores: [], permissions: [], firstLogin: false }), { status: 200, headers: { 'content-type': 'application/json' } }))
    await api.auth.login('employee', 'secret')
    expect(fetchMock).toHaveBeenCalledWith('/api/auth/login', expect.objectContaining({ method: 'POST', body: JSON.stringify({ username: 'employee', password: 'secret' }) }))
  })
  it('preserves Java service paths and month query semantics', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('[]', { status: 200, headers: { 'content-type': 'application/json' } }))
    await resources.invoices.list({ storeId: 2, yearMonth: '2026-07' })
    expect(fetchMock).toHaveBeenCalledWith('/api/invoices?storeId=2&yearMonth=2026-07', expect.objectContaining({ credentials: 'include' }))
  })
  it('encodes invoice deletion identifiers and authorization scope', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))
    await resources.invoices.remove('INV / 42', 3, 7)
    expect(fetchMock).toHaveBeenCalledWith('/api/invoices/INV%20%2F%2042?storeId=3&supplierId=7', expect.objectContaining({ method: 'DELETE' }))
  })
})

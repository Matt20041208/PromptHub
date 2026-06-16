const BASE = '/api'

async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
  const token = localStorage.getItem('token')
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string> || {}),
  }
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  const res = await fetch(`${BASE}${url}`, { ...options, headers })
  if (!res.ok) {
    if (res.status === 401) {
      if (token) localStorage.removeItem('token')
      throw new Error('NOT_AUTH')
    }
    const err = await res.json().catch(() => ({ message: res.statusText }))
    throw new Error(err.message || `HTTP ${res.status}`)
  }
  return res.json()
}

export const api = {
  get<T>(url: string, params?: Record<string, any>): Promise<T> {
    const query = params ? '?' + new URLSearchParams(
      Object.entries(params).filter(([_, v]) => v !== undefined && v !== null && v !== '').map(([k, v]) => [k, String(v)])
    ).toString() : ''
    return request<T>(`${url}${query}`)
  },
  post<T>(url: string, data?: any): Promise<T> {
    return request<T>(url, { method: 'POST', body: JSON.stringify(data) })
  },
  put<T>(url: string, data?: any): Promise<T> {
    return request<T>(url, { method: 'PUT', body: JSON.stringify(data) })
  },
  delete<T>(url: string): Promise<T> {
    return request<T>(url, { method: 'DELETE' })
  },
}

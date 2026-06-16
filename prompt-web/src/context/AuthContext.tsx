import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'
import { api } from '../api/client'
import type { ApiResponse, UserDTO, LoginResultDTO } from '../types'

interface AuthContextType {
  user: UserDTO | null
  token: string | null
  loading: boolean
  login: (token: string, user: UserDTO) => void
  logout: () => void
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextType>({
  user: null, token: null, loading: true,
  login: () => {}, logout: () => {}, refreshUser: async () => {},
})

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserDTO | null>(null)
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('token'))
  const [loading, setLoading] = useState(true)

  const refreshUser = useCallback(async () => {
    if (!token) { setLoading(false); return }
    try {
      const res = await api.get<ApiResponse<UserDTO>>('/user/info')
      if (res.code === 200) setUser(res.data)
    } catch { logout() }
    setLoading(false)
  }, [token])

  useEffect(() => { refreshUser() }, [refreshUser])

  const login = (t: string, u: UserDTO) => {
    localStorage.setItem('token', t)
    setToken(t)
    setUser(u)
  }

  const logout = () => {
    localStorage.removeItem('token')
    setToken(null)
    setUser(null)
  }

  return <AuthContext.Provider value={{ user, token, loading, login, logout, refreshUser }}>
    {children}
  </AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)

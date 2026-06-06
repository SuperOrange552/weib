import React, { createContext, useContext, useState, useEffect, useCallback } from 'react'
import type { AdminInfo, LoginRequest } from '../types'
import * as authApi from '../api/auth'

interface AuthState {
  user: AdminInfo | null
  loading: boolean
  login: (data: LoginRequest) => Promise<void>
  logout: () => void
  isAuthenticated: boolean
}

const AuthContext = createContext<AuthState>({
  user: null,
  loading: true,
  login: async () => {},
  logout: () => {},
  isAuthenticated: false
})

export const useAuth = () => useContext(AuthContext)

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AdminInfo | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const token = localStorage.getItem('admin_token')
    const savedUser = localStorage.getItem('admin_user')
    if (token && savedUser) {
      try {
        setUser(JSON.parse(savedUser))
      } catch {
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_user')
      }
    }
    setLoading(false)
  }, [])

  const login = useCallback(async (data: LoginRequest) => {
    const res = await authApi.login(data)
    const result = res.data
    if (result.code === 200 && result.data) {
      localStorage.setItem('admin_token', result.data.token)
      localStorage.setItem('admin_user', JSON.stringify(result.data.admin))
      setUser(result.data.admin)
    } else {
      throw new Error(result.msg || '登录失败')
    }
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
    setUser(null)
    authApi.logout().catch(() => {})
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, loading, login, logout, isAuthenticated: !!user }}
    >
      {children}
    </AuthContext.Provider>
  )
}

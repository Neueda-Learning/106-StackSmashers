import { createContext, useContext, useState, useCallback } from 'react'
import { login as loginApi } from '../api/authApi'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('tms_user')) }
    catch { return null }
  })

  const login = useCallback(async (username, password) => {
    const { data } = await loginApi({ username, password })
    localStorage.setItem('tms_token', data.accessToken)
    localStorage.setItem('tms_user', JSON.stringify({ username: data.username, role: data.role }))
    setUser({ username: data.username, role: data.role })
    return data
  }, [])

  const logout = useCallback(() => {
    localStorage.removeItem('tms_token')
    localStorage.removeItem('tms_user')
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be inside AuthProvider')
  return ctx
}

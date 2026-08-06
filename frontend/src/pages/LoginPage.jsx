import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useTheme } from '../context/ThemeContext'
import toast from 'react-hot-toast'
import { Shield, Eye, EyeOff, Lock, User, Moon, Sun } from 'lucide-react'

export default function LoginPage() {
  const [form, setForm] = useState({ username: '', password: '' })
  const [showPass, setShowPass] = useState(false)
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const { isDark, toggleTheme } = useTheme()
  const navigate = useNavigate()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      await login(form.username, form.password)
      toast.success('Welcome back!')
      navigate('/', { replace: true })
    } catch (err) {
      toast.error(err.response?.data?.message || 'Invalid credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-surface-900 flex items-center justify-center p-4 relative overflow-hidden">
      <button
        type="button"
        onClick={toggleTheme}
        className="btn-secondary absolute top-4 right-4 z-10 !px-2.5 !py-1.5"
        aria-label={`Switch to ${isDark ? 'light' : 'dark'} mode`}
        title={`Switch to ${isDark ? 'light' : 'dark'} mode`}
      >
        {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
      </button>
      {/* Background glow */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2
                        w-[600px] h-[600px] rounded-full
                        bg-brand-600/5 blur-[120px]" />
      </div>

      <div className="w-full max-w-sm relative animate-slide-in">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex p-4 bg-brand-600/15 rounded-2xl border border-brand-500/20 mb-4">
            <Shield className="w-10 h-10 text-brand-400" />
          </div>
          <h1 className="text-2xl font-bold text-white">Transaction Monitor</h1>
          <p className="text-sm text-slate-500 mt-1">Secure operator access only</p>
        </div>

        <form onSubmit={handleSubmit} className="card p-6 space-y-4">
          <div>
            <label className="label">Username</label>
            <div className="relative">
              <User className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
              <input
                id="username"
                type="text"
                className="input pl-9"
                placeholder="admin"
                value={form.username}
                onChange={e => setForm(f => ({ ...f, username: e.target.value }))}
                required
                autoFocus
              />
            </div>
          </div>
          <div>
            <label className="label">Password</label>
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
              <input
                id="password"
                type={showPass ? 'text' : 'password'}
                className="input pl-9 pr-10"
                placeholder="••••••••"
                value={form.password}
                onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
                required
              />
              <button
                type="button"
                onClick={() => setShowPass(p => !p)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-500 hover:text-slate-300"
              >
                {showPass ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
              </button>
            </div>
          </div>
          <button
            id="login-btn"
            type="submit"
            disabled={loading}
            className="btn-primary w-full justify-center py-2.5 mt-2"
          >
            {loading ? (
              <svg className="animate-spin w-4 h-4" viewBox="0 0 24 24" fill="none">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"/>
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v8H4z"/>
              </svg>
            ) : 'Sign In'}
          </button>
          <p className="text-center text-xs text-slate-600 pt-2">
            Protected by JWT • TLS encrypted
          </p>
        </form>
      </div>
    </div>
  )
}

import { useEffect, useState } from 'react'
import { Bell, Moon, Sun } from 'lucide-react'
import { getAlertStats } from '../../api/alertApi'
import { useTheme } from '../../context/ThemeContext'

export default function TopBar({ title }) {
  const [openAlerts, setOpenAlerts] = useState(null)
  const { isDark, toggleTheme } = useTheme()

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const { data } = await getAlertStats()
        setOpenAlerts(data.openAlerts)
      } catch { /* silently fail */ }
    }
    fetchStats()
    const interval = setInterval(fetchStats, 10000) // poll every 10s
    return () => clearInterval(interval)
  }, [])

  return (
    <header className="h-14 shrink-0 border-b border-white/5 bg-surface-800/50 backdrop-blur
                       flex items-center justify-between px-6 sticky top-0 z-10">
      <h1 className="text-base font-semibold text-white">{title}</h1>
      <div className="flex items-center gap-3">
        <button
          type="button"
          onClick={toggleTheme}
          className="btn-secondary !px-3 !py-1.5"
          aria-label={`Switch to ${isDark ? 'light' : 'dark'} mode`}
          title={`Switch to ${isDark ? 'light' : 'dark'} mode`}
        >
          {isDark ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
          <span className="text-xs">{isDark ? 'Light' : 'Dark'}</span>
        </button>
        <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-surface-700 border border-white/5">
          <Bell className="w-3.5 h-3.5 text-slate-400" />
          <span className="text-xs text-slate-400">Open Alerts:</span>
          {openAlerts === null ? (
            <span className="text-xs text-slate-500">—</span>
          ) : (
            <span className={`text-xs font-bold ${openAlerts > 0 ? 'text-amber-400' : 'text-emerald-400'}`}>
              {openAlerts}
            </span>
          )}
        </div>
        <div className="text-[10px] text-slate-600">
          {new Date().toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' })}
        </div>
      </div>
    </header>
  )
}

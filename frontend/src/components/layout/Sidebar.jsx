import { NavLink } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import {
  LayoutDashboard, ArrowLeftRight, Bell, Search,
  ShieldAlert, BookOpen, LogOut, Shield, Gavel
} from 'lucide-react'

const navItems = [
  { to: '/',              icon: LayoutDashboard, label: 'Dashboard'       },
  { to: '/transactions',  icon: ArrowLeftRight,  label: 'Transactions'    },
  { to: '/alerts',        icon: Bell,            label: 'Alerts'          },
  { to: '/investigation', icon: Gavel,           label: 'Investigation'   },
  { to: '/rules',         icon: ShieldAlert,     label: 'Monitoring Rules'},
  { to: '/reports',       icon: BookOpen,        label: 'Reports'         },
]

export default function Sidebar() {
  const { user, logout } = useAuth()

  return (
    <aside className="w-60 shrink-0 flex flex-col bg-surface-800 border-r border-white/5 h-screen sticky top-0">
      {/* Logo */}
      <div className="px-5 py-5 border-b border-white/5">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-brand-600/20 rounded-lg border border-brand-500/20">
            <Shield className="w-5 h-5 text-brand-400" />
          </div>
          <div>
            <div className="text-sm font-bold text-white leading-none">TMS</div>
            <div className="text-[10px] text-slate-500 mt-0.5">Transaction Monitor</div>
          </div>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {navItems.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/'}
            className={({ isActive }) =>
              `nav-item ${isActive ? 'active' : ''}`
            }
          >
            <Icon className="w-4 h-4 shrink-0" />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      {/* User */}
      <div className="px-3 py-4 border-t border-white/5">
        <div className="flex items-center gap-3 px-3 py-2.5 rounded-lg bg-surface-700 mb-2">
          <div className="w-7 h-7 rounded-full bg-brand-600/30 flex items-center justify-center text-brand-400 text-xs font-bold">
            {user?.username?.charAt(0).toUpperCase()}
          </div>
          <div className="flex-1 min-w-0">
            <div className="text-xs font-medium text-white truncate">{user?.username}</div>
            <div className="text-[10px] text-slate-500">{user?.role}</div>
          </div>
        </div>
        <button onClick={logout} className="nav-item w-full text-red-400 hover:text-red-300 hover:bg-red-500/10">
          <LogOut className="w-4 h-4" />
          <span>Logout</span>
        </button>
      </div>
    </aside>
  )
}

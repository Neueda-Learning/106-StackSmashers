import { Outlet, useLocation } from 'react-router-dom'
import Sidebar from './Sidebar'
import TopBar from './TopBar'

const titles = {
  '/':             'Dashboard',
  '/transactions': 'Transactions',
  '/alerts':       'Alerts',
  '/investigation':'Investigation Queue',
  '/rules':        'Monitoring Rules',
  '/reports':      'Reports & Audit',
}

export default function Layout() {
  const { pathname } = useLocation()
  const title = Object.entries(titles)
    .reverse()
    .find(([path]) => pathname.startsWith(path))?.[1] ?? 'TMS'
  const year = new Date().getFullYear()

  return (
    <div className="flex h-screen overflow-hidden">
      <Sidebar />
      <div className="flex-1 flex flex-col overflow-hidden">
        <TopBar title={title} />
        <main className="flex-1 overflow-y-auto p-6">
          <div className="animate-in max-w-screen-2xl mx-auto">
            <Outlet />
          </div>
        </main>
        <footer className="shrink-0 border-t border-white/10 bg-surface-800/90 px-6 py-3 backdrop-blur">
          <div className="mx-auto flex max-w-screen-2xl items-center justify-between text-xs text-slate-400">
            <p>Transaction Monitoring System</p>
            <p>Developed by StackSmashers • {year}</p>
          </div>
        </footer>
      </div>
    </div>
  )
}

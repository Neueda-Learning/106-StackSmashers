import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getDashboardStats } from '../api/dashboardApi'
import { PageLoader } from '../components/common/LoadingStates'
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend
} from 'recharts'
import {
  Bell, ArrowLeftRight, ShieldAlert, TrendingUp,
  AlertTriangle, CheckCircle, XCircle, Send
} from 'lucide-react'

const STATUS_COLORS = {
  OPEN:      '#3b82f6',
  FORWARDED: '#f59e0b',
  DISMISSED: '#64748b',
  CLOSED:    '#10b981',
}

function StatCard({ label, value, icon: Icon, color = 'blue', sublabel, to }) {
  const colorMap = {
    blue:   'text-blue-400   bg-blue-500/10   border-blue-500/20',
    amber:  'text-amber-400  bg-amber-500/10  border-amber-500/20',
    red:    'text-red-400    bg-red-500/10    border-red-500/20',
    green:  'text-emerald-400 bg-emerald-500/10 border-emerald-500/20',
    slate:  'text-slate-400  bg-slate-500/10  border-slate-500/20',
    brand:  'text-brand-400  bg-brand-500/10  border-brand-500/20',
  }
  const cls = colorMap[color] || colorMap.blue
  const Inner = (
    <div className="stat-card h-full hover:border-white/10 transition-all duration-200">
      <div className="flex items-start justify-between">
        <div>
          <div className="stat-label">{label}</div>
          <div className="stat-value mt-1">{value ?? '—'}</div>
          {sublabel && <div className="text-xs text-slate-500 mt-1">{sublabel}</div>}
        </div>
        <div className={`p-2.5 rounded-xl border ${cls}`}>
          <Icon className={`w-5 h-5 ${cls.split(' ')[0]}`} />
        </div>
      </div>
    </div>
  )
  return to ? <Link to={to} className="block h-full">{Inner}</Link> : Inner
}

const CustomTooltip = ({ active, payload }) => {
  if (active && payload?.length) {
    return (
      <div className="card px-3 py-2 text-xs">
        <div className="font-semibold text-white">{payload[0].name}</div>
        <div className="text-slate-400">{payload[0].value} alerts</div>
      </div>
    )
  }
  return null
}

export default function DashboardPage() {
  const [stats, setStats] = useState(null)
  const [loading, setLoading] = useState(true)

  const fetchStats = async () => {
    try {
      const { data } = await getDashboardStats()
      setStats(data)
    } catch (e) {
      console.error(e)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchStats()
    const id = setInterval(fetchStats, 15000)
    return () => clearInterval(id)
  }, [])

  if (loading) return <PageLoader />

  const pieData = (stats?.alertsByStatus || []).map(d => ({
    name: d.status,
    value: Number(d.count),
    color: STATUS_COLORS[d.status] || '#64748b',
  }))

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h2 className="page-title text-gradient">Overview Dashboard</h2>
        <p className="page-subtitle mt-1">Real-time transaction monitoring and alert status</p>
      </div>

      {/* Alert Stats Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Open Alerts"    value={stats?.openAlerts}
                  icon={Bell}            color="blue"  to="/alerts?status=OPEN" />
        <StatCard label="Forwarded"       value={stats?.forwardedAlerts}
                  icon={Send}            color="amber" to="/alerts?status=FORWARDED" />
        <StatCard label="Dismissed"       value={stats?.dismissedAlerts}
                  icon={XCircle}         color="slate" to="/alerts?status=DISMISSED" />
        <StatCard label="Closed"          value={stats?.closedAlerts}
                  icon={CheckCircle}     color="green" to="/alerts?status=CLOSED" />
      </div>

      {/* Second Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard label="Total Alerts"           value={stats?.totalAlerts}
                  icon={AlertTriangle}            color="red" />
        <StatCard label="% Forwarded"
                  value={`${stats?.percentageForwarded ?? 0}%`}
                  icon={TrendingUp}               color="amber"
                  sublabel="of all alerts" />
        <StatCard label="Total Transactions"     value={stats?.totalTransactions}
                  icon={ArrowLeftRight}            color="brand" to="/transactions" />
        <StatCard label="Transactions (24h)"     value={stats?.transactionsLast24h}
                  icon={ArrowLeftRight}            color="blue" />
      </div>

      {/* Chart + Recent */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-4">
        {/* Pie Chart */}
        <div className="card p-6 lg:col-span-2">
          <h3 className="section-title">Alerts by Status</h3>
          {pieData.length === 0 ? (
            <div className="flex items-center justify-center h-48 text-slate-600 text-sm">
              No alerts yet
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={220}>
              <PieChart>
                <Pie
                  data={pieData}
                  cx="50%" cy="50%"
                  innerRadius={60} outerRadius={90}
                  paddingAngle={3}
                  dataKey="value"
                >
                  {pieData.map((entry, i) => (
                    <Cell key={i} fill={entry.color} strokeWidth={0} />
                  ))}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
                <Legend
                  formatter={(v) => <span className="text-xs text-slate-400">{v}</span>}
                  iconType="circle" iconSize={8}
                />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* Info Panel */}
        <div className="card p-6 lg:col-span-3 space-y-4">
          <h3 className="section-title">Activity Summary</h3>
          <div className="space-y-3">
            {[
              { label: 'Alerts last 24 hours', value: stats?.alertsLast24h, icon: Bell, color: 'text-blue-400' },
              { label: 'Alerts last 7 days',   value: stats?.alertsLast7d,  icon: Bell, color: 'text-purple-400' },
              { label: 'Forwarded to investigation', value: `${stats?.percentageForwarded ?? 0}%`, icon: Send, color: 'text-amber-400' },
            ].map(({ label, value, icon: Icon, color }) => (
              <div key={label} className="flex items-center justify-between py-3 border-b border-white/5 last:border-0">
                <div className="flex items-center gap-3">
                  <Icon className={`w-4 h-4 ${color}`} />
                  <span className="text-sm text-slate-400">{label}</span>
                </div>
                <span className="text-sm font-semibold text-white">{value ?? '—'}</span>
              </div>
            ))}
          </div>

          <div className="pt-2 flex gap-3">
            <Link to="/alerts" className="btn-primary flex-1 justify-center">
              <Bell className="w-4 h-4" /> View Alerts
            </Link>
            <Link to="/transactions" className="btn-secondary flex-1 justify-center">
              <ArrowLeftRight className="w-4 h-4" /> Transactions
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}

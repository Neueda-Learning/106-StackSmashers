import { useState, useEffect, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getAlerts } from '../api/alertApi'
import Badge from '../components/common/Badge'
import Pagination from '../components/common/Pagination'
import { PageLoader, EmptyState } from '../components/common/LoadingStates'
import { format } from 'date-fns'
import { Bell, Search, Filter, X, Eye } from 'lucide-react'
import toast from 'react-hot-toast'

const STATUSES   = ['', 'OPEN', 'FORWARDED', 'DISMISSED', 'CLOSED']
const SEVERITIES = ['', 'CRITICAL', 'HIGH', 'MEDIUM', 'LOW']

export default function AlertsPage() {
  const [searchParams] = useSearchParams()
  const [data, setData]       = useState(null)
  const [loading, setLoading] = useState(true)
  const [page, setPage]       = useState(0)
  const [showFilters, setShowFilters] = useState(false)
  const [filters, setFilters] = useState({
    search:   searchParams.get('search') || '',
    status:   searchParams.get('status') || '',
    severity: '',
  })

  const fetch = useCallback(async () => {
    setLoading(true)
    try {
      const params = {
        page, size: 20,
        ...(filters.search   && { search:   filters.search   }),
        ...(filters.status   && { status:   filters.status   }),
        ...(filters.severity && { severity: filters.severity }),
      }
      const { data: res } = await getAlerts(params)
      setData(res)
    } catch {
      toast.error('Failed to load alerts')
    } finally {
      setLoading(false)
    }
  }, [page, filters])

  useEffect(() => { fetch() }, [fetch])

  const clearFilters = () => {
    setFilters({ search: '', status: '', severity: '' })
    setPage(0)
  }

  const hasFilters = Object.values(filters).some(Boolean)

  return (
    <div className="space-y-4">
      <div>
        <h2 className="page-title">Alerts</h2>
        <p className="page-subtitle">{data?.totalElements ?? 0} total alerts</p>
      </div>

      <div className="card p-4 space-y-3">
        <div className="flex gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input
              className="input pl-9"
              placeholder="Search by account, customer, transaction ref…"
              value={filters.search}
              onChange={e => { setFilters(f => ({ ...f, search: e.target.value })); setPage(0) }}
            />
          </div>
          <button
            onClick={() => setShowFilters(p => !p)}
            className={`btn-secondary ${showFilters ? 'border-brand-500/40 text-brand-400' : ''}`}
          >
            <Filter className="w-4 h-4" /> Filters
            {hasFilters && <span className="w-1.5 h-1.5 rounded-full bg-brand-400" />}
          </button>
          {hasFilters && (
            <button onClick={clearFilters} className="btn-secondary text-red-400 hover:text-red-300">
              <X className="w-4 h-4" /> Clear
            </button>
          )}
        </div>
        {showFilters && (
          <div className="grid grid-cols-2 gap-3 pt-1 border-t border-white/5 animate-in">
            <div>
              <label className="label">Status</label>
              <select className="input" value={filters.status}
                onChange={e => { setFilters(f => ({ ...f, status: e.target.value })); setPage(0) }}>
                {STATUSES.map(s => <option key={s} value={s}>{s || 'All Statuses'}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Severity</label>
              <select className="input" value={filters.severity}
                onChange={e => { setFilters(f => ({ ...f, severity: e.target.value })); setPage(0) }}>
                {SEVERITIES.map(s => <option key={s} value={s}>{s || 'All Severities'}</option>)}
              </select>
            </div>
          </div>
        )}
      </div>

      <div className="card">
        {loading ? <PageLoader /> : (
          <>
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>#</th>
                    <th>Transaction Ref</th>
                    <th>Account</th>
                    <th>Customer</th>
                    <th>Rule Triggered</th>
                    <th>Severity</th>
                    <th>Status</th>
                    <th>Created</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {data?.content?.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="py-8">
                        <EmptyState icon={Bell} title="No alerts found" description="All clear or adjust your filters" />
                      </td>
                    </tr>
                  ) : data?.content?.map(alert => (
                    <tr key={alert.id}>
                      <td className="text-slate-500 text-xs">{alert.id}</td>
                      <td className="font-mono text-xs text-brand-400">{alert.transactionRef}</td>
                      <td className="text-xs">{alert.accountId}</td>
                      <td>{alert.customerName}</td>
                      <td className="text-xs text-slate-300">{alert.ruleName}</td>
                      <td><Badge value={alert.severity} /></td>
                      <td><Badge value={alert.status} /></td>
                      <td className="text-xs text-slate-500">
                        {alert.createdAt ? format(new Date(alert.createdAt), 'dd MMM yy HH:mm') : '—'}
                      </td>
                      <td>
                        <Link
                          to={`/alerts/${alert.id}`}
                          className="btn-secondary px-2.5 py-1.5 text-xs"
                        >
                          <Eye className="w-3.5 h-3.5" /> View
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <div className="px-4 pb-4">
              <Pagination page={page} totalPages={data?.totalPages || 0} onPageChange={setPage} />
            </div>
          </>
        )}
      </div>
    </div>
  )
}

import { useState, useEffect, useCallback } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { getTransactions } from '../api/transactionApi'
import Badge from '../components/common/Badge'
import Pagination from '../components/common/Pagination'
import { PageLoader, EmptyState } from '../components/common/LoadingStates'
import { format } from 'date-fns'
import { Search, Filter, X, ArrowLeftRight, Eye } from 'lucide-react'
import toast from 'react-hot-toast'

const STATUSES = ['', 'PENDING', 'COMPLETED', 'FLAGGED']
const TYPES    = ['', 'CREDIT', 'DEBIT', 'TRANSFER']

export default function TransactionsPage() {
  const [searchParams] = useSearchParams()
  const [data, setData]     = useState(null)
  const [loading, setLoading] = useState(true)
  const [page, setPage]     = useState(0)
  const [filters, setFilters] = useState({
    search: '',
    status: searchParams.get('status') || '',
    type: '',
    countryCode: '',
  })
  const [showFilters, setShowFilters] = useState(false)

  const fetch = useCallback(async () => {
    setLoading(true)
    try {
      const params = {
        page, size: 20,
        ...(filters.search && { search: filters.search }),
        ...(filters.status && { status: filters.status }),
        ...(filters.type   && { type:   filters.type   }),
        ...(filters.countryCode && { countryCode: filters.countryCode }),
      }
      const { data: res } = await getTransactions(params)
      setData(res)
    } catch {
      toast.error('Failed to load transactions')
    } finally {
      setLoading(false)
    }
  }, [page, filters])

  useEffect(() => { fetch() }, [fetch])

  const clearFilters = () => {
    setFilters({ search: '', status: '', type: '', countryCode: '' })
    setPage(0)
  }

  const hasFilters = Object.values(filters).some(Boolean)

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="page-title">Transactions</h2>
          <p className="page-subtitle">{data?.totalElements ?? 0} total records</p>
        </div>
      </div>

      {/* Search & Filters */}
      <div className="card p-4 space-y-3">
        <div className="flex gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-500" />
            <input
              className="input pl-9"
              placeholder="Search by ref, account, customer…"
              value={filters.search}
              onChange={e => { setFilters(f => ({ ...f, search: e.target.value })); setPage(0) }}
            />
          </div>
          <button
            onClick={() => setShowFilters(p => !p)}
            className={`btn-secondary gap-2 ${showFilters ? 'border-brand-500/40 text-brand-400' : ''}`}
          >
            <Filter className="w-4 h-4" />
            Filters
            {hasFilters && <span className="w-1.5 h-1.5 rounded-full bg-brand-400 inline-block" />}
          </button>
          {hasFilters && (
            <button onClick={clearFilters} className="btn-secondary text-red-400 hover:text-red-300">
              <X className="w-4 h-4" /> Clear
            </button>
          )}
        </div>
        {showFilters && (
          <div className="grid grid-cols-3 gap-3 pt-1 border-t border-white/5 animate-in">
            <div>
              <label className="label">Status</label>
              <select className="input" value={filters.status}
                onChange={e => { setFilters(f => ({ ...f, status: e.target.value })); setPage(0) }}>
                {STATUSES.map(s => <option key={s} value={s}>{s || 'All'}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Type</label>
              <select className="input" value={filters.type}
                onChange={e => { setFilters(f => ({ ...f, type: e.target.value })); setPage(0) }}>
                {TYPES.map(t => <option key={t} value={t}>{t || 'All'}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Country Code</label>
              <input className="input" placeholder="e.g. US, KP" value={filters.countryCode}
                onChange={e => { setFilters(f => ({ ...f, countryCode: e.target.value })); setPage(0) }} />
            </div>
          </div>
        )}
      </div>

      {/* Table */}
      <div className="card">
        {loading ? <PageLoader /> : (
          <>
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Reference</th>
                    <th>Account</th>
                    <th>Customer</th>
                    <th>Amount</th>
                    <th>Type</th>
                    <th>Country</th>
                    <th>Status</th>
                    <th>Date</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {data?.content?.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="py-8">
                        <EmptyState icon={ArrowLeftRight} title="No transactions found" />
                      </td>
                    </tr>
                  ) : data?.content?.map(tx => (
                    <tr key={tx.id}>
                      <td className="font-mono text-xs text-brand-400">{tx.transactionRef}</td>
                      <td className="text-xs">{tx.accountId}</td>
                      <td>{tx.customerName}</td>
                      <td className="font-medium">
                        {tx.currency} {Number(tx.amount).toLocaleString(undefined, { minimumFractionDigits: 2 })}
                      </td>
                      <td><Badge value={tx.transactionType} /></td>
                      <td className="font-mono text-xs">{tx.countryCode}</td>
                      <td><Badge value={tx.status} /></td>
                      <td className="text-xs text-slate-500">
                        {tx.createdAt ? format(new Date(tx.createdAt), 'dd MMM yy HH:mm') : '—'}
                      </td>
                      <td>
                        <Link to={`/alerts?search=${tx.accountId}`}
                          className="p-1.5 rounded hover:bg-white/5 text-slate-500 hover:text-white inline-flex"
                          title="View alerts">
                          <Eye className="w-3.5 h-3.5" />
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

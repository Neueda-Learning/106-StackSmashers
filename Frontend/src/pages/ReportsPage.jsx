import { useState } from 'react'
import {
  getTransactionReport, getAlertReport, getAuditReport
} from '../api/reportApi'
import Badge from '../components/common/Badge'
import Pagination from '../components/common/Pagination'
import { PageLoader, EmptyState } from '../components/common/LoadingStates'
import { format } from 'date-fns'
import { BookOpen, ArrowLeftRight, Bell, FileText, Download } from 'lucide-react'
import toast from 'react-hot-toast'

const TABS = [
  { key: 'transactions', label: 'Transaction History', icon: ArrowLeftRight },
  { key: 'alerts',       label: 'Alert History',       icon: Bell           },
  { key: 'audit',        label: 'Audit Trail',         icon: FileText       },
]

function DateFilter({ fromDate, toDate, onFrom, onTo }) {
  return (
    <div className="flex gap-3 items-end">
      <div>
        <label className="label">From Date</label>
        <input type="datetime-local" className="input text-xs" value={fromDate} onChange={e => onFrom(e.target.value)} />
      </div>
      <div>
        <label className="label">To Date</label>
        <input type="datetime-local" className="input text-xs" value={toDate}   onChange={e => onTo(e.target.value)} />
      </div>
    </div>
  )
}

function TransactionReport() {
  const [data, setData]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [page, setPage]   = useState(0)
  const [fromDate, setFromDate] = useState('')
  const [toDate,   setToDate]   = useState('')

  const fetch = async (p = 0) => {
    setLoading(true)
    setPage(p)
    try {
      const params = {
        page: p, size: 50,
        ...(fromDate && { fromDate: new Date(fromDate).toISOString() }),
        ...(toDate   && { toDate:   new Date(toDate  ).toISOString() }),
      }
      const { data: res } = await getTransactionReport(params)
      setData(res)
    } catch { toast.error('Failed to generate report') }
    finally { setLoading(false) }
  }

  return (
    <div className="space-y-4">
      <div className="card p-4 flex items-end gap-4 flex-wrap">
        <DateFilter fromDate={fromDate} toDate={toDate} onFrom={setFromDate} onTo={setToDate} />
        <button onClick={() => fetch(0)} className="btn-primary">Generate Report</button>
      </div>
      {loading && <PageLoader />}
      {data && !loading && (
        <div className="card">
          <div className="px-4 py-3 border-b border-white/5 text-xs text-slate-400">
            {data.totalElements} records found
          </div>
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Reference</th><th>Account</th><th>Customer</th>
                  <th>Amount</th><th>Type</th><th>Country</th>
                  <th>Status</th><th>Date</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 ? (
                  <tr><td colSpan={8}><EmptyState icon={ArrowLeftRight} title="No transactions in range" /></td></tr>
                ) : data.content.map(tx => (
                  <tr key={tx.id}>
                    <td className="font-mono text-xs text-brand-400">{tx.transactionRef}</td>
                    <td className="text-xs">{tx.accountId}</td>
                    <td>{tx.customerName}</td>
                    <td className="font-medium text-sm">
                      {tx.currency} {Number(tx.amount).toLocaleString()}
                    </td>
                    <td><Badge value={tx.transactionType} /></td>
                    <td className="text-xs">{tx.countryCode}</td>
                    <td><Badge value={tx.status} /></td>
                    <td className="text-xs text-slate-500">
                      {tx.createdAt ? format(new Date(tx.createdAt), 'dd MMM yy HH:mm') : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="p-4">
            <Pagination page={page} totalPages={data.totalPages} onPageChange={(p) => fetch(p)} />
          </div>
        </div>
      )}
    </div>
  )
}

function AlertReport() {
  const [data, setData]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [page, setPage]   = useState(0)
  const [fromDate, setFromDate] = useState('')
  const [toDate,   setToDate]   = useState('')
  const [status,   setStatus]   = useState('')

  const fetch = async (p = 0) => {
    setLoading(true)
    setPage(p)
    try {
      const params = {
        page: p, size: 50,
        ...(fromDate && { fromDate: new Date(fromDate).toISOString() }),
        ...(toDate   && { toDate:   new Date(toDate  ).toISOString() }),
        ...(status   && { status }),
      }
      const { data: res } = await getAlertReport(params)
      setData(res)
    } catch { toast.error('Failed to generate report') }
    finally { setLoading(false) }
  }

  return (
    <div className="space-y-4">
      <div className="card p-4 flex items-end gap-4 flex-wrap">
        <DateFilter fromDate={fromDate} toDate={toDate} onFrom={setFromDate} onTo={setToDate} />
        <div>
          <label className="label">Status</label>
          <select className="input" value={status} onChange={e => setStatus(e.target.value)}>
            {['', 'OPEN', 'FORWARDED', 'DISMISSED', 'CLOSED'].map(s => (
              <option key={s} value={s}>{s || 'All'}</option>
            ))}
          </select>
        </div>
        <button onClick={() => fetch(0)} className="btn-primary">Generate Report</button>
      </div>
      {loading && <PageLoader />}
      {data && !loading && (
        <div className="card">
          <div className="px-4 py-3 border-b border-white/5 text-xs text-slate-400">
            {data.totalElements} alerts found
          </div>
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>#</th><th>Tx Ref</th><th>Account</th><th>Rule</th>
                  <th>Severity</th><th>Status</th><th>Created</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 ? (
                  <tr><td colSpan={7}><EmptyState icon={Bell} title="No alerts in range" /></td></tr>
                ) : data.content.map(a => (
                  <tr key={a.id}>
                    <td className="text-xs text-slate-500">{a.id}</td>
                    <td className="font-mono text-xs text-brand-400">{a.transactionRef}</td>
                    <td className="text-xs">{a.accountId}</td>
                    <td className="text-xs">{a.ruleName}</td>
                    <td><Badge value={a.severity} /></td>
                    <td><Badge value={a.status} /></td>
                    <td className="text-xs text-slate-500">
                      {a.createdAt ? format(new Date(a.createdAt), 'dd MMM yy HH:mm') : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="p-4">
            <Pagination page={page} totalPages={data.totalPages} onPageChange={(p) => fetch(p)} />
          </div>
        </div>
      )}
    </div>
  )
}

function AuditTrailReport() {
  const [data, setData]   = useState(null)
  const [loading, setLoading] = useState(false)
  const [page, setPage]   = useState(0)
  const [fromDate, setFromDate] = useState('')
  const [toDate,   setToDate]   = useState('')
  const [action,   setAction]   = useState('')

  const fetch = async (p = 0) => {
    setLoading(true)
    setPage(p)
    try {
      const params = {
        page: p, size: 50,
        ...(fromDate && { fromDate: new Date(fromDate).toISOString() }),
        ...(toDate   && { toDate:   new Date(toDate  ).toISOString() }),
        ...(action   && { action }),
      }
      const { data: res } = await getAuditReport(params)
      setData(res)
    } catch { toast.error('Failed to generate report') }
    finally { setLoading(false) }
  }

  return (
    <div className="space-y-4">
      <div className="card p-4 flex items-end gap-4 flex-wrap">
        <DateFilter fromDate={fromDate} toDate={toDate} onFrom={setFromDate} onTo={setToDate} />
        <div>
          <label className="label">Action Filter</label>
          <select className="input" value={action} onChange={e => setAction(e.target.value)}>
            {['', 'CREATED', 'FORWARDED', 'DISMISSED', 'CLOSED', 'NOTE_ADDED'].map(a => (
              <option key={a} value={a}>{a || 'All Actions'}</option>
            ))}
          </select>
        </div>
        <button onClick={() => fetch(0)} className="btn-primary">Generate Report</button>
      </div>
      {loading && <PageLoader />}
      {data && !loading && (
        <div className="card">
          <div className="px-4 py-3 border-b border-white/5 text-xs text-slate-400">
            {data.totalElements} audit entries found
          </div>
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Alert #</th><th>Account</th><th>Tx Ref</th>
                  <th>Action</th><th>Performed By</th><th>Notes</th><th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {data.content.length === 0 ? (
                  <tr><td colSpan={7}><EmptyState icon={FileText} title="No audit entries in range" /></td></tr>
                ) : data.content.map(t => (
                  <tr key={t.id}>
                    <td className="text-xs text-slate-500">{t.alertId}</td>
                    <td className="text-xs">{t.accountId}</td>
                    <td className="font-mono text-xs text-brand-400">{t.transactionRef}</td>
                    <td>
                      <span className="badge badge-open">{t.action}</span>
                    </td>
                    <td className="text-xs">{t.performedBy}</td>
                    <td className="text-xs text-slate-400 max-w-[200px] truncate">{t.notes || '—'}</td>
                    <td className="text-xs text-slate-500">
                      {t.createdAt ? format(new Date(t.createdAt), 'dd MMM yy HH:mm') : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="p-4">
            <Pagination page={page} totalPages={data.totalPages} onPageChange={(p) => fetch(p)} />
          </div>
        </div>
      )}
    </div>
  )
}

export default function ReportsPage() {
  const [activeTab, setActiveTab] = useState('transactions')

  return (
    <div className="space-y-4">
      <div>
        <h2 className="page-title">Reports & Audit</h2>
        <p className="page-subtitle">Generate and export compliance reports</p>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface-800 border border-white/5 rounded-xl p-1 w-fit">
        {TABS.map(({ key, label, icon: Icon }) => (
          <button
            key={key}
            onClick={() => setActiveTab(key)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all
              ${activeTab === key
                ? 'bg-brand-600/20 text-brand-400 border border-brand-500/20'
                : 'text-slate-400 hover:text-white hover:bg-white/5'}`}
          >
            <Icon className="w-4 h-4" />
            {label}
          </button>
        ))}
      </div>

      {/* Content */}
      <div className="animate-in">
        {activeTab === 'transactions' && <TransactionReport />}
        {activeTab === 'alerts'       && <AlertReport />}
        {activeTab === 'audit'        && <AuditTrailReport />}
      </div>
    </div>
  )
}

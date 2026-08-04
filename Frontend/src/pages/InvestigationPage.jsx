import { useState, useEffect } from 'react'
import { getForwardedAlerts } from '../api/alertApi'
import { Link } from 'react-router-dom'
import Badge from '../components/common/Badge'
import { PageLoader, EmptyState } from '../components/common/LoadingStates'
import { format } from 'date-fns'
import { Gavel, Eye } from 'lucide-react'
import toast from 'react-hot-toast'

export default function InvestigationPage() {
  const [alerts, setAlerts] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const fetch = async () => {
      setLoading(true)
      try {
        const { data } = await getForwardedAlerts()
        setAlerts(data)
      } catch {
        toast.error('Failed to load investigation queue')
      } finally {
        setLoading(false)
      }
    }
    fetch()
  }, [])

  return (
    <div className="space-y-4">
      <div>
        <h2 className="page-title">Investigation Queue</h2>
        <p className="page-subtitle">{alerts.length} alerts forwarded to investigation team</p>
      </div>

      <div className="card">
        {loading ? <PageLoader /> : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Transaction Ref</th>
                  <th>Account</th>
                  <th>Customer</th>
                  <th>Rule</th>
                  <th>Severity</th>
                  <th>Assigned To</th>
                  <th>Forwarded At</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {alerts.length === 0 ? (
                  <tr>
                    <td colSpan={9} className="py-8">
                      <EmptyState icon={Gavel} title="No alerts in investigation queue"
                        description="Alerts forwarded to investigation will appear here" />
                    </td>
                  </tr>
                ) : alerts.map(alert => (
                  <tr key={alert.id}>
                    <td className="text-slate-500 text-xs">{alert.id}</td>
                    <td className="font-mono text-xs text-brand-400">{alert.transactionRef}</td>
                    <td className="text-xs">{alert.accountId}</td>
                    <td>{alert.customerName}</td>
                    <td className="text-xs">{alert.ruleName}</td>
                    <td><Badge value={alert.severity} /></td>
                    <td className="text-xs text-amber-400">{alert.assignedTo || 'Investigation Team'}</td>
                    <td className="text-xs text-slate-500">
                      {alert.updatedAt ? format(new Date(alert.updatedAt), 'dd MMM yy HH:mm') : '—'}
                    </td>
                    <td>
                      <Link to={`/alerts/${alert.id}`} className="btn-secondary px-2.5 py-1.5 text-xs">
                        <Eye className="w-3.5 h-3.5" /> View
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

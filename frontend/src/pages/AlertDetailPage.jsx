import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { getAlert, getAlertAudit, forwardAlert, dismissAlert, closeAlert } from '../api/alertApi'
import Badge from '../components/common/Badge'
import Modal from '../components/common/Modal'
import { PageLoader } from '../components/common/LoadingStates'
import { format } from 'date-fns'
import { ArrowLeft, Send, XCircle, CheckCircle, Clock, User, FileText } from 'lucide-react'
import toast from 'react-hot-toast'

const AUDIT_ICONS = {
  CREATED:   { icon: Clock,        color: 'text-blue-400'  },
  FORWARDED: { icon: Send,         color: 'text-amber-400' },
  DISMISSED: { icon: XCircle,      color: 'text-slate-400' },
  CLOSED:    { icon: CheckCircle,  color: 'text-emerald-400' },
  NOTE_ADDED:{ icon: FileText,     color: 'text-brand-400' },
  REOPENED:  { icon: Clock,        color: 'text-purple-400' },
}

function ActionModal({ isOpen, onClose, title, onConfirm, loading }) {
  const [notes, setNotes] = useState('')
  return (
    <Modal isOpen={isOpen} onClose={onClose} title={title} size="sm">
      <div className="space-y-4">
        <div>
          <label className="label">Notes (optional)</label>
          <textarea
            className="input resize-none h-24"
            placeholder="Add a note about this action…"
            value={notes}
            onChange={e => setNotes(e.target.value)}
          />
        </div>
        <div className="flex gap-3 justify-end">
          <button onClick={onClose} className="btn-secondary">Cancel</button>
          <button
            onClick={() => onConfirm(notes)}
            disabled={loading}
            className="btn-primary"
          >
            {loading ? 'Processing…' : 'Confirm'}
          </button>
        </div>
      </div>
    </Modal>
  )
}

export default function AlertDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [alert, setAlert]   = useState(null)
  const [audit, setAudit]   = useState([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [modal, setModal]   = useState(null) // 'forward' | 'dismiss' | 'close'

  useEffect(() => {
    const fetchAll = async () => {
      setLoading(true)
      try {
        const [alertRes, auditRes] = await Promise.all([
          getAlert(id),
          getAlertAudit(id)
        ])
        setAlert(alertRes.data)
        setAudit(auditRes.data)
      } catch {
        toast.error('Failed to load alert')
        navigate('/alerts')
      } finally {
        setLoading(false)
      }
    }
    fetchAll()
  }, [id, navigate])

  const handleAction = async (action, notes) => {
    setActionLoading(true)
    try {
      const fn = { forward: forwardAlert, dismiss: dismissAlert, close: closeAlert }[action]
      const { data } = await fn(id, { notes })
      setAlert(data)
      setModal(null)
      // Reload audit trail
      const { data: auditData } = await getAlertAudit(id)
      setAudit(auditData)
      toast.success(`Alert ${action}ed successfully`)
    } catch (err) {
      toast.error(err.response?.data?.message || `Failed to ${action} alert`)
    } finally {
      setActionLoading(false)
    }
  }

  if (loading) return <PageLoader />
  if (!alert)  return null

  const canForward  = alert.status === 'OPEN'
  const canDismiss  = alert.status === 'OPEN'
  const canClose    = alert.status !== 'CLOSED'

  return (
    <div className="space-y-5 max-w-4xl">
      {/* Header */}
      <div className="flex items-start gap-4">
        <button onClick={() => navigate(-1)} className="btn-secondary p-2 mt-0.5">
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div className="flex-1">
          <div className="flex items-center gap-3 flex-wrap">
            <h2 className="page-title">Alert #{alert.id}</h2>
            <Badge value={alert.status} />
            <Badge value={alert.severity} />
          </div>
          <p className="page-subtitle mt-1">{alert.ruleName}</p>
        </div>
        <div className="flex gap-2">
          {canForward && (
            <button onClick={() => setModal('forward')} className="btn-warning">
              <Send className="w-4 h-4" /> Forward
            </button>
          )}
          {canDismiss && (
            <button onClick={() => setModal('dismiss')} className="btn-secondary text-red-400">
              <XCircle className="w-4 h-4" /> Dismiss
            </button>
          )}
          {canClose && (
            <button onClick={() => setModal('close')} className="btn-success">
              <CheckCircle className="w-4 h-4" /> Close
            </button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-5 gap-5">
        {/* Details */}
        <div className="lg:col-span-3 space-y-4">
          <div className="card p-5">
            <h3 className="section-title">Alert Details</h3>
            <div className="space-y-3">
              <Field label="Description"     value={alert.description} />
              <Field label="Rule Code"       value={<code className="text-brand-400 text-xs">{alert.ruleCode}</code>} />
              <Field label="Assigned To"     value={alert.assignedTo || '—'} />
              <Field label="Created"         value={alert.createdAt ? format(new Date(alert.createdAt), 'dd MMM yyyy HH:mm:ss') : '—'} />
              {alert.updatedAt && <Field label="Last Updated" value={format(new Date(alert.updatedAt), 'dd MMM yyyy HH:mm:ss')} />}
            </div>
          </div>

          <div className="card p-5">
            <h3 className="section-title">Transaction</h3>
            <div className="space-y-3">
              <Field label="Transaction Ref" value={<span className="font-mono text-brand-400 text-sm">{alert.transactionRef}</span>} />
              <Field label="Account ID"      value={alert.accountId} />
              <Field label="Customer"        value={alert.customerName} />
            </div>
          </div>
        </div>

        {/* Audit Trail */}
        <div className="lg:col-span-2">
          <div className="card p-5 h-full">
            <h3 className="section-title">Audit Trail</h3>
            {audit.length === 0 ? (
              <p className="text-sm text-slate-500">No audit entries.</p>
            ) : (
              <div className="relative">
                <div className="absolute left-3.5 top-0 bottom-0 w-px bg-white/5" />
                <div className="space-y-4 pl-10">
                  {audit.map((entry, i) => {
                    const { icon: Icon, color } = AUDIT_ICONS[entry.action] || AUDIT_ICONS.NOTE_ADDED
                    return (
                      <div key={i} className="relative">
                        <div className={`absolute -left-[26px] p-1 rounded-full bg-surface-800 border border-white/10 ${color}`}>
                          <Icon className="w-3 h-3" />
                        </div>
                        <div className="text-xs text-slate-500 mb-0.5">
                          {entry.createdAt ? format(new Date(entry.createdAt), 'dd MMM HH:mm') : '—'}
                        </div>
                        <div className="text-xs font-semibold text-white">{entry.action}</div>
                        <div className="flex items-center gap-1 text-xs text-slate-500 mt-0.5">
                          <User className="w-3 h-3" /> {entry.performedBy}
                        </div>
                        {entry.notes && (
                          <div className="text-xs text-slate-400 mt-1 bg-surface-700 rounded px-2 py-1 border border-white/5">
                            {entry.notes}
                          </div>
                        )}
                      </div>
                    )
                  })}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Action Modals */}
      <ActionModal isOpen={modal === 'forward'} onClose={() => setModal(null)}
        title="Forward to Investigation" loading={actionLoading}
        onConfirm={(notes) => handleAction('forward', notes)} />
      <ActionModal isOpen={modal === 'dismiss'} onClose={() => setModal(null)}
        title="Dismiss Alert" loading={actionLoading}
        onConfirm={(notes) => handleAction('dismiss', notes)} />
      <ActionModal isOpen={modal === 'close'} onClose={() => setModal(null)}
        title="Close Alert" loading={actionLoading}
        onConfirm={(notes) => handleAction('close', notes)} />
    </div>
  )
}

function Field({ label, value }) {
  return (
    <div className="flex flex-col sm:flex-row sm:items-start gap-1">
      <span className="text-xs text-slate-500 w-32 shrink-0">{label}</span>
      <span className="text-sm text-slate-200">{value}</span>
    </div>
  )
}

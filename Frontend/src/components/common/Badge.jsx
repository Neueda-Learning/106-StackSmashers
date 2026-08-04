import clsx from 'clsx'

const statusMap = {
  OPEN:      'badge-open',
  FORWARDED: 'badge-forwarded',
  DISMISSED: 'badge-dismissed',
  CLOSED:    'badge-closed',
  CRITICAL:  'badge-critical',
  HIGH:      'badge-high',
  MEDIUM:    'badge-medium',
  LOW:       'badge-low',
  FLAGGED:   'badge-flagged',
  COMPLETED: 'badge-completed',
  PENDING:   'badge-pending',
  CREDIT:    'badge-completed',
  DEBIT:     'badge-high',
  TRANSFER:  'badge-open',
}

export default function Badge({ value, className }) {
  const key = value?.toString().toUpperCase()
  return (
    <span className={clsx(statusMap[key] || 'badge bg-slate-700 text-slate-300', className)}>
      {value}
    </span>
  )
}

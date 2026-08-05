import { Loader2 } from 'lucide-react'

export function Spinner({ className = 'w-6 h-6' }) {
  return <Loader2 className={`${className} animate-spin text-brand-400`} />
}

export function PageLoader() {
  return (
    <div className="flex items-center justify-center h-64">
      <Spinner className="w-8 h-8" />
    </div>
  )
}

export function EmptyState({ icon: Icon, title, description }) {
  return (
    <div className="flex flex-col items-center justify-center h-48 text-center gap-3">
      {Icon && <Icon className="w-10 h-10 text-slate-600" />}
      <div>
        <div className="text-sm font-medium text-slate-400">{title}</div>
        {description && <div className="text-xs text-slate-600 mt-1">{description}</div>}
      </div>
    </div>
  )
}

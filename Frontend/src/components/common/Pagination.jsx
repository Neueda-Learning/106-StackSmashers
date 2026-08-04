import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from 'lucide-react'

export default function Pagination({ page, totalPages, onPageChange }) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-center gap-1 mt-4">
      <button className="btn-secondary px-2 py-1.5 text-xs" onClick={() => onPageChange(0)} disabled={page === 0}>
        <ChevronsLeft className="w-3.5 h-3.5" />
      </button>
      <button className="btn-secondary px-2 py-1.5 text-xs" onClick={() => onPageChange(page - 1)} disabled={page === 0}>
        <ChevronLeft className="w-3.5 h-3.5" />
      </button>
      <span className="px-4 py-1.5 text-xs text-slate-400">
        Page <span className="text-white font-medium">{page + 1}</span> of {totalPages}
      </span>
      <button className="btn-secondary px-2 py-1.5 text-xs" onClick={() => onPageChange(page + 1)} disabled={page >= totalPages - 1}>
        <ChevronRight className="w-3.5 h-3.5" />
      </button>
      <button className="btn-secondary px-2 py-1.5 text-xs" onClick={() => onPageChange(totalPages - 1)} disabled={page >= totalPages - 1}>
        <ChevronsRight className="w-3.5 h-3.5" />
      </button>
    </div>
  )
}

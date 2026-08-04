import { useState, useEffect } from 'react'
import { getRules, updateRule } from '../api/ruleApi'
import Modal from '../components/common/Modal'
import { PageLoader } from '../components/common/LoadingStates'
import { ShieldAlert, Edit2, ToggleLeft, ToggleRight, Plus, Trash2 } from 'lucide-react'
import toast from 'react-hot-toast'
import clsx from 'clsx'

const SEVERITY_COLOR = {
  CRITICAL: 'text-red-400 bg-red-500/10 border-red-500/20',
  HIGH:     'text-orange-400 bg-orange-500/10 border-orange-500/20',
  MEDIUM:   'text-amber-400 bg-amber-500/10 border-amber-500/20',
  LOW:      'text-slate-400 bg-slate-500/10 border-slate-500/20',
}

function ParamEditor({ params, onChange }) {
  const [entries, setEntries] = useState(
    Object.entries(params || {}).map(([k, v]) => ({ key: k, value: JSON.stringify(v) }))
  )

  const update = (newEntries) => {
    setEntries(newEntries)
    try {
      const obj = Object.fromEntries(newEntries.map(e => [e.key, JSON.parse(e.value)]))
      onChange(obj)
    } catch { /* invalid JSON, ignore */ }
  }

  const add    = () => update([...entries, { key: '', value: '' }])
  const remove = (i) => update(entries.filter((_, idx) => idx !== i))
  const set    = (i, field, val) => update(entries.map((e, idx) => idx === i ? { ...e, [field]: val } : e))

  return (
    <div className="space-y-2">
      {entries.map((e, i) => (
        <div key={i} className="flex gap-2 items-center">
          <input className="input flex-1" placeholder="key" value={e.key}
            onChange={ev => set(i, 'key', ev.target.value)} />
          <input className="input flex-1" placeholder="value (JSON)" value={e.value}
            onChange={ev => set(i, 'value', ev.target.value)} />
          <button onClick={() => remove(i)} className="text-red-400 hover:text-red-300 p-1.5">
            <Trash2 className="w-3.5 h-3.5" />
          </button>
        </div>
      ))}
      <button onClick={add} className="btn-secondary text-xs py-1.5 px-3">
        <Plus className="w-3 h-3" /> Add Parameter
      </button>
    </div>
  )
}

export default function RulesPage() {
  const [rules, setRules]       = useState([])
  const [loading, setLoading]   = useState(true)
  const [editing, setEditing]   = useState(null)
  const [editParams, setEditParams] = useState({})
  const [saving, setSaving]     = useState(false)

  useEffect(() => {
    const fetch = async () => {
      try {
        const { data } = await getRules()
        setRules(data)
      } catch {
        toast.error('Failed to load rules')
      } finally {
        setLoading(false)
      }
    }
    fetch()
  }, [])

  const toggleActive = async (rule) => {
    try {
      const { data } = await updateRule(rule.id, { isActive: !rule.isActive })
      setRules(rs => rs.map(r => r.id === rule.id ? data : r))
      toast.success(`Rule ${data.isActive ? 'enabled' : 'disabled'}`)
    } catch {
      toast.error('Failed to update rule')
    }
  }

  const openEdit = (rule) => {
    setEditing(rule)
    setEditParams(rule.parameters || {})
  }

  const saveEdit = async () => {
    setSaving(true)
    try {
      const { data } = await updateRule(editing.id, { parameters: editParams })
      setRules(rs => rs.map(r => r.id === editing.id ? data : r))
      setEditing(null)
      toast.success('Rule parameters updated')
    } catch {
      toast.error('Failed to save')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <PageLoader />

  return (
    <div className="space-y-4">
      <div>
        <h2 className="page-title">Monitoring Rules</h2>
        <p className="page-subtitle">Configure thresholds and toggle rules on/off</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {rules.map(rule => (
          <div key={rule.id}
            className={clsx('card p-5 flex flex-col gap-4 transition-all duration-200',
              !rule.isActive && 'opacity-60')}
          >
            <div className="flex items-start justify-between gap-3">
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <h3 className="text-sm font-semibold text-white">{rule.ruleName}</h3>
                  <span className={clsx('badge border', SEVERITY_COLOR[rule.severity])}>
                    {rule.severity}
                  </span>
                </div>
                <code className="text-[10px] text-slate-500 mt-0.5 block">{rule.ruleCode}</code>
                <p className="text-xs text-slate-400 mt-2">{rule.description}</p>
              </div>
              <button
                onClick={() => toggleActive(rule)}
                className={clsx('shrink-0 transition-colors', rule.isActive ? 'text-emerald-400' : 'text-slate-600')}
                title={rule.isActive ? 'Disable rule' : 'Enable rule'}
              >
                {rule.isActive
                  ? <ToggleRight className="w-8 h-8" />
                  : <ToggleLeft  className="w-8 h-8" />}
              </button>
            </div>

            {/* Parameters preview */}
            {rule.parameters && Object.keys(rule.parameters).length > 0 && (
              <div className="bg-surface-700 rounded-lg p-3 border border-white/5">
                <div className="text-[10px] text-slate-500 uppercase tracking-wider mb-2">Parameters</div>
                <div className="space-y-1">
                  {Object.entries(rule.parameters).map(([k, v]) => (
                    <div key={k} className="flex justify-between text-xs">
                      <span className="text-slate-500">{k}</span>
                      <span className="text-slate-200 font-medium">
                        {Array.isArray(v) ? v.join(', ') : String(v)}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <button onClick={() => openEdit(rule)} className="btn-secondary self-start text-xs py-1.5">
              <Edit2 className="w-3.5 h-3.5" /> Edit Parameters
            </button>
          </div>
        ))}
      </div>

      {/* Edit Modal */}
      <Modal isOpen={!!editing} onClose={() => setEditing(null)} title={`Edit: ${editing?.ruleName}`}>
        <div className="space-y-4">
          <p className="text-xs text-slate-400">{editing?.description}</p>
          <div>
            <label className="label">Parameters (JSON values)</label>
            <ParamEditor params={editParams} onChange={setEditParams} />
          </div>
          <div className="flex gap-3 justify-end pt-2 border-t border-white/5">
            <button onClick={() => setEditing(null)} className="btn-secondary">Cancel</button>
            <button onClick={saveEdit} disabled={saving} className="btn-primary">
              {saving ? 'Saving…' : 'Save Changes'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  )
}

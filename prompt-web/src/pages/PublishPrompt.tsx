import React, { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api } from '../api/client'
import type { ApiResponse, CategoryTreeVO } from '../types'

export default function PublishPrompt() {
  const { user } = useAuth()
  const nav = useNavigate()
  const [form, setForm] = useState({ title: '', description: '', content: '', price: 0, categoryId: 0, cover: '' })
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)
  const [aiLoading, setAiLoading] = useState<'classify' | 'optimize' | null>(null)
  const [categories, setCategories] = useState<CategoryTreeVO[]>([])

  useEffect(() => {
    api.get<ApiResponse<CategoryTreeVO[]>>('/prompt/category').then(r => {
      if (r.code === 200) setCategories(r.data)
    })
    const prefill = localStorage.getItem('publish_prefill')
    if (prefill) {
      try {
        const data = JSON.parse(prefill)
        setForm(f => ({ ...f, ...data }))
        localStorage.removeItem('publish_prefill')
      } catch {}
    }
  }, [])

  if (!user) { nav('/login'); return null }

  const flatCategories = (cats: CategoryTreeVO[]): CategoryTreeVO[] => {
    let result: CategoryTreeVO[] = []
    for (const c of cats) {
      result.push(c)
      if (c.children) result = result.concat(flatCategories(c.children))
    }
    return result
  }

  const aiClassify = async () => {
    if (!form.title.trim() || form.content.length < 10) { setErr('请先填写标题和内容（内容至少10个字符）'); return }
    setAiLoading('classify')
    setErr('')
    try {
      const r = await api.post<ApiResponse<any>>('/agent/classify', { title: form.title, description: form.description, content: form.content })
      if (r.code === 200 && r.data) {
        const flat = flatCategories(categories)
        const match = flat.find(c => c.name === r.data.category)
        if (match) setForm(f => ({ ...f, categoryId: match.id }))
        if (r.data.tags?.length > 0) {
          setErr('✨ AI 建议分类: ' + r.data.category + ' | 标签: ' + r.data.tags.join(', ') + '（已自动填入分类）')
        }
      }
    } catch (ex: any) { setErr('AI分类失败: ' + ex.message) }
    setAiLoading(null)
  }

  const aiOptimize = async () => {
    if (form.content.length < 10) { setErr('内容至少10个字符才能优化'); return }
    setAiLoading('optimize')
    setErr('')
    try {
      const r = await api.post<ApiResponse<any>>('/agent/optimize', { prompt: form.content, intent: form.title })
      if (r.code === 200 && r.data) {
        setForm(f => ({ ...f, content: r.data.optimized }))
        setErr('✅ 已用AI优化，评分: ' + r.data.score + ' | 改动: ' + r.data.changes.join('; '))
      }
    } catch (ex: any) { setErr('AI优化失败: ' + ex.message) }
    setAiLoading(null)
  }

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErr('')
    if (!form.title.trim() || !form.content.trim()) { setErr('标题和内容不能为空'); return }
    setLoading(true)
    try {
      const res = await api.post<ApiResponse<null>>('/prompt', { ...form, categoryId: form.categoryId || null, price: form.price || 0 })
      if (res.code === 200) { alert('创建成功'); nav('/') }
      else setErr(res.message || '创建失败')
    } catch (ex: any) { setErr(ex.message) }
    setLoading(false)
  }

  return (
    <div style={{ maxWidth: 700, margin: '24px auto' }}>
      <div className="card">
        <h2 style={{ marginBottom: 24 }}>发布提示词</h2>
        <form onSubmit={submit}>
          <div style={{ marginBottom: 16 }}>
            <label className="label">标题 *</label>
            <input className="input" placeholder="给提示词起个名字" value={form.title}
              onChange={e => setForm({ ...form, title: e.target.value })} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">描述</label>
            <textarea className="input" rows={3} placeholder="简要描述这个提示词" value={form.description}
              onChange={e => setForm({ ...form, description: e.target.value })} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
              <label className="label" style={{ marginBottom: 0 }}>提示词内容 *</label>
              <button type="button" className="btn-secondary" onClick={aiOptimize} disabled={aiLoading === 'optimize'}
                style={{ fontSize: 13, padding: '4px 14px' }}>
                {aiLoading === 'optimize' ? '⏳ AI优化中...' : '✨ AI 优化'}
              </button>
            </div>
            <textarea className="input" rows={8} placeholder="输入提示词内容（可点击上方 AI 优化自动改进）" value={form.content}
              onChange={e => setForm({ ...form, content: e.target.value })} style={{ fontFamily: 'monospace' }} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
              <label className="label" style={{ marginBottom: 0 }}>分类 + 标签</label>
              <button type="button" className="btn-secondary" onClick={aiClassify} disabled={aiLoading === 'classify'}
                style={{ fontSize: 13, padding: '4px 14px' }}>
                {aiLoading === 'classify' ? '⏳ AI识别中...' : '🤖 AI 自动分类'}
              </button>
            </div>
            <select className="input" value={form.categoryId} onChange={e => setForm({ ...form, categoryId: Number(e.target.value) })}>
              <option value={0}>选择分类</option>
              {flatCategories(categories).map(c => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
          <div style={{ display: 'flex', gap: 16, marginBottom: 16 }}>
            <div style={{ flex: 1 }}>
              <label className="label">价格 (0=免费)</label>
              <input className="input" type="number" min="0" step="0.01" value={form.price}
                onChange={e => setForm({ ...form, price: Number(e.target.value) })} />
            </div>
          </div>
          {err && <div className="error" style={{ marginBottom: 12, whiteSpace: 'pre-wrap' }}>{err}</div>}
          <button className="btn-primary" type="submit" disabled={loading} style={{ width: '100%' }}>
            {loading ? '发布中...' : '发布提示词'}
          </button>
        </form>
      </div>
    </div>
  )
}

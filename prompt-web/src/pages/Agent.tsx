import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import type { ApiResponse } from '../types'

export default function Agent() {
  const [result, setResult] = useState<any>(null)
  const [resultType, setResultType] = useState('')
  const [loading, setLoading] = useState(false)
  const [err, setErr] = useState('')
  const nav = useNavigate()

  const callAgent = async (endpoint: string, body: any) => {
    setLoading(true)
    setErr('')
    setResult(null)
    try {
      const res = await api.post<ApiResponse<any>>(endpoint, body)
      if (res.code === 200) {
        setResultType(endpoint.split('/').pop() || '')
        setResult(res.data)
      } else setErr(res.message || '请求失败')
    } catch (ex: any) { setErr(ex.message) }
    setLoading(false)
  }

  return (
    <div style={{ paddingTop: 24, maxWidth: 900, margin: '0 auto' }}>
      <h2 style={{ marginBottom: 24 }}>🤖 AI 提示词工具</h2>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 24 }}>
        <AgentCard title="优化 (Optimize)" desc="优化你的提示词，使其更清晰有效">
          <OptimizeForm onSubmit={body => callAgent('/agent/optimize', body)} loading={loading} />
        </AgentCard>

        <AgentCard title="分类 (Classify)" desc="自动为提示词分类和打标签">
          <ClassifyForm onSubmit={body => callAgent('/agent/classify', body)} loading={loading} />
        </AgentCard>

        <AgentCard title="生成 (Generate)" desc="根据意图自动生成提示词模板">
          <GenerateForm onSubmit={body => callAgent('/agent/generate', body)} loading={loading} />
        </AgentCard>

        <AgentCard title="评测 (Evaluate)" desc="评测提示词的质量">
          <EvaluateForm onSubmit={body => callAgent('/agent/evaluate', body)} loading={loading} />
        </AgentCard>

        <AgentCard title="调试 (Debug)" desc="用 LLM 运行提示词看效果" style={{ gridColumn: '1 / -1' }}>
          <DebugForm onSubmit={body => callAgent('/agent/debug', body)} loading={loading} />
        </AgentCard>
      </div>

      {err && <div className="card" style={{ marginTop: 16, border: '1px solid #e74c3c', background: '#fdf0ef' }}><p className="error">{err}</p></div>}

      {result && <ResultDisplay type={resultType} data={result} onPublish={() => {
        const prefill: any = { price: 0 }
        if (result.optimized) prefill.content = result.optimized
        if (result.template) prefill.content = result.template
        if (result.description) prefill.description = result.description
        localStorage.setItem('publish_prefill', JSON.stringify(prefill))
        nav('/publish')
      }} />}
    </div>
  )
}

function ResultDisplay({ type, data, onPublish }: { type: string; data: any; onPublish: () => void }) {
  const styles = {
    card: { marginTop: 16, padding: 24, background: '#fff', borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.08)' } as React.CSSProperties,
    heading: { marginBottom: 12, color: '#1a1a2e' },
    text: { lineHeight: 1.8, whiteSpace: 'pre-wrap' } as React.CSSProperties,
    code: { background: '#1a1a2e', color: '#e0e0e0', padding: 16, borderRadius: 8, fontSize: 14, lineHeight: 1.6, whiteSpace: 'pre-wrap' } as React.CSSProperties,
    tag: { display: 'inline-block', padding: '2px 10px', background: '#e8f0fe', color: '#4a90d9', borderRadius: 12, fontSize: 13, marginRight: 6, marginBottom: 6 },
    score: { fontSize: 32, fontWeight: 700, color: '#27ae60' },
    dim: { display: 'flex', flexDirection: 'column', gap: 4, marginTop: 8 },
    dimRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
    dimBarBg: { flex: 1, height: 6, background: '#eee', borderRadius: 3, marginLeft: 12, overflow: 'hidden' },
    dimBar: (w: number) => ({ height: 6, background: '#4a90d9', borderRadius: 3, width: `${w}%` }),
    meta: { fontSize: 13, color: '#888', marginTop: 4 },
  }

  if (type === 'optimize') return (
    <div style={styles.card}>
      <h3 style={{ ...styles.heading, color: '#27ae60' }}>★ 评分: {data.score}</h3>
      <div style={styles.text}>{data.optimized}</div>
      {data.changes?.length > 0 && <div style={{ marginTop: 12 }}>
        <h4 style={{ marginBottom: 6, color: '#555' }}>改动说明：</h4>
        {data.changes.map((c: string, i: number) => <div key={i} style={{ ...styles.text, color: '#666', fontSize: 14 }}>• {c}</div>)}
      </div>}
      <button className="btn-primary" onClick={onPublish} style={{ marginTop: 16 }}>📤 一键发布到市场</button>
    </div>
  )

  if (type === 'classify') return (
    <div style={styles.card}>
      <div style={{ marginBottom: 12 }}><span style={{ fontWeight: 600 }}>分类：</span> <span className="tag">{data.category}</span></div>
      <div><span style={{ fontWeight: 600 }}>标签：</span> {data.tags?.map((t: string) => <span key={t} style={styles.tag}>{t}</span>)}</div>
    </div>
  )

  if (type === 'generate') return (
    <div style={styles.card}>
      <h3 style={styles.heading}>生成的模板</h3>
      <pre style={styles.code}>{data.template}</pre>
      {data.description && <p style={{ ...styles.text, color: '#666', marginTop: 12 }}>{data.description}</p>}
      <button className="btn-primary" onClick={onPublish} style={{ marginTop: 16 }}>📤 一键发布到市场</button>
    </div>
  )

  if (type === 'evaluate') return (
    <div style={styles.card}>
      <div style={{ textAlign: 'center', marginBottom: 16 }}>
        <div style={styles.score}>{data.total_score}</div>
        <div style={{ color: '#888', fontSize: 14 }}>综合评分</div>
      </div>
      {data.dimensions && <div style={styles.dim}>
        {Object.entries(data.dimensions as Record<string, number>).map(([k, v]) => (
          <div key={k} style={styles.dimRow}>
            <span style={{ width: 100, fontSize: 14, color: '#555' }}>{k}</span>
            <div style={styles.dimBarBg}><div style={styles.dimBar(v * 10)} /></div>
            <span style={{ width: 30, textAlign: 'right', fontSize: 13, color: '#888' }}>{v}</span>
          </div>
        ))}
      </div>}
      {data.suggestions?.length > 0 && <div style={{ marginTop: 12 }}>
        <h4 style={{ marginBottom: 6, color: '#555' }}>建议：</h4>
        {data.suggestions.map((s: string, i: number) => <div key={i} style={{ ...styles.text, color: '#666', fontSize: 14 }}>• {s}</div>)}
      </div>}
    </div>
  )

  if (type === 'debug') return (
    <div style={styles.card}>
      <div style={styles.meta}>模型: {data.model}</div>
      <div style={styles.text}>{data.output}</div>
    </div>
  )

  return (
    <div style={styles.card}>
      <pre style={styles.code}>{JSON.stringify(data, null, 2)}</pre>
    </div>
  )
}

function AgentCard({ title, desc, children, style }: { title: string; desc: string; children: React.ReactNode; style?: React.CSSProperties }) {
  return (
    <div className="card" style={style}>
      <h4 style={{ marginBottom: 4 }}>{title}</h4>
      <p style={{ fontSize: 13, color: '#888', marginBottom: 16 }}>{desc}</p>
      {children}
    </div>
  )
}

function OptimizeForm({ onSubmit, loading }: { onSubmit: (b: any) => void; loading: boolean }) {
  const [v, setV] = useState({ prompt: '', intent: '', style: '' })
  const [err, setErr] = useState('')
  const submit = () => {
    if (v.prompt.length < 10) { setErr('提示词至少需要10个字符'); return }
    setErr('')
    onSubmit(v)
  }
  return <AgentForm onSubmit={submit} loading={loading} error={err}>
    <textarea className="input" rows={4} placeholder="输入要优化的提示词（至少10个字符）..." value={v.prompt}
      onChange={e => { setV({ ...v, prompt: e.target.value }); setErr('') }} />
    <span style={{ fontSize: 11, color: v.prompt.length < 10 ? '#e74c3c' : '#999' }}>{v.prompt.length}/10 最小字符</span>
    <div style={{ display: 'flex', gap: 8 }}>
      <input className="input" placeholder="目标意图（可选）" value={v.intent} onChange={e => setV({ ...v, intent: e.target.value })} style={{ flex: 1 }} />
      <input className="input" placeholder="期望风格（可选）" value={v.style} onChange={e => setV({ ...v, style: e.target.value })} style={{ flex: 1 }} />
    </div>
  </AgentForm>
}

function ClassifyForm({ onSubmit, loading }: { onSubmit: (b: any) => void; loading: boolean }) {
  const [v, setV] = useState({ title: '', description: '', content: '' })
  const [err, setErr] = useState('')
  const submit = () => {
    if (!v.title.trim()) { setErr('标题不能为空'); return }
    if (v.content.length < 10) { setErr('内容至少需要10个字符'); return }
    setErr('')
    onSubmit(v)
  }
  return <AgentForm onSubmit={submit} loading={loading} error={err}>
    <input className="input" placeholder="标题 *" value={v.title} onChange={e => { setV({ ...v, title: e.target.value }); setErr('') }} />
    <input className="input" placeholder="描述（可选）" value={v.description} onChange={e => setV({ ...v, description: e.target.value })} />
    <textarea className="input" rows={3} placeholder="提示词内容（至少10个字符）" value={v.content} onChange={e => { setV({ ...v, content: e.target.value }); setErr('') }} />
    <span style={{ fontSize: 11, color: v.content.length < 10 ? '#e74c3c' : '#999' }}>{v.content.length}/10 最小字符</span>
  </AgentForm>
}

function GenerateForm({ onSubmit, loading }: { onSubmit: (b: any) => void; loading: boolean }) {
  const [v, setV] = useState({ intent: '', variables: '' })
  const [err, setErr] = useState('')
  const submit = () => {
    if (v.intent.length < 5) { setErr('意图描述至少需要5个字符'); return }
    setErr('')
    onSubmit({ intent: v.intent, variables: v.variables ? v.variables.split(',').map(s => s.trim()).filter(Boolean) : undefined })
  }
  return <AgentForm onSubmit={submit} loading={loading} error={err}>
    <textarea className="input" rows={3} placeholder="描述你想生成的提示词意图（至少5个字符）..." value={v.intent}
      onChange={e => { setV({ ...v, intent: e.target.value }); setErr('') }} />
    <span style={{ fontSize: 11, color: v.intent.length < 5 ? '#e74c3c' : '#999' }}>{v.intent.length}/5 最小字符</span>
    <input className="input" placeholder="变量名（逗号分隔，可选，如：topic,style）" value={v.variables}
      onChange={e => setV({ ...v, variables: e.target.value })} />
  </AgentForm>
}

function EvaluateForm({ onSubmit, loading }: { onSubmit: (b: any) => void; loading: boolean }) {
  const [v, setV] = useState({ prompt_content: '' })
  const [err, setErr] = useState('')
  const submit = () => {
    if (v.prompt_content.length < 10) { setErr('提示词至少需要10个字符'); return }
    setErr('')
    onSubmit(v)
  }
  return <AgentForm onSubmit={submit} loading={loading} error={err}>
    <textarea className="input" rows={4} placeholder="输入要评测的提示词（至少10个字符）..." value={v.prompt_content}
      onChange={e => { setV({ ...v, prompt_content: e.target.value }); setErr('') }} />
    <span style={{ fontSize: 11, color: v.prompt_content.length < 10 ? '#e74c3c' : '#999' }}>{v.prompt_content.length}/10 最小字符</span>
  </AgentForm>
}

function DebugForm({ onSubmit, loading }: { onSubmit: (b: any) => void; loading: boolean }) {
  const [v, setV] = useState({ prompt_content: '', variables: '', model: '' })
  const [err, setErr] = useState('')
  const submit = () => {
    if (v.prompt_content.length < 5) { setErr('提示词至少需要5个字符'); return }
    setErr('')
    let vars: Record<string, string> | undefined
    if (v.variables) {
      vars = {}
      v.variables.split(',').forEach(pair => {
        const [k, val] = pair.split(':').map(s => s.trim())
        if (k && val) vars![k] = val
      })
    }
    onSubmit({ prompt_content: v.prompt_content, variables: vars, model: v.model || undefined })
  }
  return <AgentForm onSubmit={submit} loading={loading} error={err}>
    <textarea className="input" rows={4} placeholder="输入提示词模板（如：写一首关于{{topic}}的诗，至少5个字符）" value={v.prompt_content}
      onChange={e => { setV({ ...v, prompt_content: e.target.value }); setErr('') }} />
    <span style={{ fontSize: 11, color: v.prompt_content.length < 5 ? '#e74c3c' : '#999' }}>{v.prompt_content.length}/5 最小字符</span>
    <input className="input" placeholder="变量值（可选，如：topic:春天,style:现代）" value={v.variables}
      onChange={e => setV({ ...v, variables: e.target.value })} />
    <input className="input" placeholder="模型（可选，默认 deepseek-v4-pro）" value={v.model}
      onChange={e => setV({ ...v, model: e.target.value })} />
  </AgentForm>
}

function AgentForm({ children, onSubmit, loading, error }: { children: React.ReactNode; onSubmit: () => void; loading: boolean; error?: string }) {
  return (
    <>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {children}
      </div>
      {error && <div className="error" style={{ marginTop: 8 }}>{error}</div>}
      <button className="btn-primary" onClick={onSubmit} disabled={loading} style={{ marginTop: 12, width: '100%' }}>
        {loading ? '处理中...' : '提交'}
      </button>
    </>
  )
}

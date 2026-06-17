import React, { useEffect, useState, useRef } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import type { ApiResponse, PageResult, PromptListVO, CategoryTreeVO, PromptTag, PromptQueryDTO } from '../types'

export default function Home() {
  const [prompts, setPrompts] = useState<PageResult<PromptListVO> | null>(null)
  const [categories, setCategories] = useState<CategoryTreeVO[]>([])
  const [tags, setTags] = useState<PromptTag[]>([])
  const [query, setQuery] = useState<PromptQueryDTO>({ page: 1, size: 20 })
  const [loading, setLoading] = useState(true)

  // Smart search state
  const [smartInput, setSmartInput] = useState('')
  const [routeLoading, setRouteLoading] = useState(false)
  const [routeResult, setRouteResult] = useState<any>(null)
  const nav = useNavigate()

  useEffect(() => {
    api.get<ApiResponse<CategoryTreeVO[]>>('/prompt/category').then(r => { if (r.code === 200) setCategories(r.data) })
    api.get<ApiResponse<PromptTag[]>>('/prompt/tag').then(r => { if (r.code === 200) setTags(r.data) })
  }, [])

  useEffect(() => {
    setLoading(true)
    api.get<ApiResponse<PageResult<PromptListVO>>>('/prompt/list', query).then(r => {
      if (r.code === 200) setPrompts(r.data)
    }).finally(() => setLoading(false))
  }, [query])

  const handleSmartSearch = async () => {
    const input = smartInput.trim()
    if (!input || input.length < 2) return
    setRouteLoading(true)
    setRouteResult(null)
    try {
      const res = await api.post<ApiResponse<any>>('/agent/route', { message: input })
      if (res.code === 200 && res.data) {
        const intent = res.data
        setRouteResult(intent)
        executeIntent(intent)
      }
    } catch (ex: any) {
      setRouteResult({ target: 'unknown', action: 'unknown', error: ex.message })
    }
    setRouteLoading(false)
  }

  const executeIntent = (intent: any) => {
    const { target, action, params } = intent

    if (target === 'market' && action === 'search') {
      const kw = params?.keyword || smartInput.trim()
      // 先弹"正在搜索..."，搜完根据结果决定
      api.get<ApiResponse<PageResult<PromptListVO>>>('/prompt/list', { keyword: kw, size: 20 })
        .then(r => {
          if (r.code === 200 && r.data && r.data.total > 0) {
            setQuery(q => ({ ...q, keyword: kw, page: 1 }))
            setRouteResult(null)
          } else {
            // 没搜到 → 弹生成弹窗
            setRouteResult({ target: 'agent', action: 'generate', noResults: true, keyword: kw, confidence: 0.9 })
          }
        })
        .catch(() => {})
      setSmartInput('')
      return
    }

    if (target === 'market' && action === 'buy') {
      setQuery(q => ({ ...q, keyword: params?.keyword || '', page: 1 }))
      return
    }

    if (target === 'agent' && action === 'generate') {
      const kw = params?.keyword || params?.intent || smartInput.trim()
      // Always show generate dialog first, then check market in background
      setRouteResult({ ...intent, noResults: true, keyword: kw })

      api.get<ApiResponse<PageResult<PromptListVO>>>('/prompt/list', { keyword: kw, size: 3 })
        .then(r => {
          if (r.code === 200 && r.data && r.data.total > 0) {
            setRouteResult(prev => prev?.generated ? null : { ...intent, noResults: true, keyword: kw, marketHits: r.data.records?.slice(0, 3) })
          }
        })
        .catch(() => {})
      return
    }

    if (target === 'agent') {
      nav('/agent')
      return
    }

    if (target === 'account') {
      nav('/dashboard')
      return
    }

    if (target === 'knowledge') {
      handleRagQuery(intent, smartInput.trim())
      return
    }

    if (target === 'unknown' || intent.confidence < 0.5) {
      handleRagQuery(intent, smartInput.trim())
      return
    }
  }

  const handleRagQuery = async (intent: any, question: string) => {
    if (!question) return
    setRouteLoading(true)
    setRouteResult({ ...intent, noResults: true, keyword: question, isRag: true, generated: { template: '查询中...' } })
    try {
      const token = localStorage.getItem('token')
      const headers: Record<string, string> = { 'Content-Type': 'application/json' }
      if (token) headers['Authorization'] = `Bearer ${token}`

      const res = await fetch('/api/agent/rag', {
        method: 'POST', headers,
        body: JSON.stringify({ question, stream: false }),
      })
      const json = await res.json()
      if (json.code === 200 && json.data) {
        setRouteResult(prev => ({
          ...prev,
          generated: { template: json.data.answer },
          ragSources: json.data.sources || [],
        }))
      } else {
        setRouteResult(prev => ({ ...prev, error: '回答失败' }))
      }
    } catch (ex: any) {
      setRouteResult(prev => ({ ...prev, error: '回答失败: ' + (ex.message || '请重试') }))
    }
    setRouteLoading(false)
  }
  const handleWorkflow = async (intent: any) => {
    const genIntent = intent.params?.intent || intent.params?.keyword || intent.keyword || smartInput.trim()
    if (genIntent.length < 5) {
      setRouteResult(prev => ({ ...prev, error: '意图描述太短，至少5个字符' }))
      return
    }
    setRouteLoading(true)
    setRouteResult({ ...intent, noResults: true, isWorkflow: true, generated: { template: '' }, workflowSteps: [] })
    try {
      const token = localStorage.getItem('token')
      const headers: Record<string, string> = { 'Content-Type': 'application/json' }
      if (token) headers['Authorization'] = `Bearer ${token}`

      const res = await fetch('/api/agent/workflow/publish', {
        method: 'POST', headers,
        body: JSON.stringify({ intent: genIntent, auto_publish: true, user_id: 1 }),
      })
      const reader = res.body?.getReader()
      if (!reader) throw new Error('No reader')
      const decoder = new TextDecoder()
      let fullTemplate = '', steps: any[] = [], finalReport: any = null

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const chunk = decoder.decode(value, { stream: true })
        for (const line of chunk.split('\n')) {
          if (line.startsWith('data: ')) {
            try {
              const data = JSON.parse(line.slice(6))
              if (data.done) { finalReport = data.report }
              else if (data.step && data.status === 'done') {
                steps.push(data)
                if (data.step === 'optimize' && data.data?.optimized) fullTemplate = data.data.optimized
              } else if (data.step && data.status === 'running') {
                steps.push(data)
              }
              setRouteResult(prev => ({
                ...prev,
                generated: { template: fullTemplate || prev.generated?.template || '处理中...' },
                workflowSteps: steps,
                workflowReport: finalReport,
              }))
            } catch {}
          }
        }
      }
    } catch (ex: any) {
      setRouteResult(prev => ({ ...prev, error: 'Workflow 失败: ' + ex.message }))
    }
    setRouteLoading(false)
  }
  const handleGenerate = async (intent: any) => {
    let genIntent = intent.params?.intent || intent.params?.keyword || intent.keyword || smartInput.trim()
    if (genIntent.length < 5) genIntent = '写一个关于' + genIntent + '的提示词'
    setRouteLoading(true)
    setRouteResult({ ...intent, generated: { template: '' }, generatedAt: Date.now() })
    try {
      const token = localStorage.getItem('token')
      const headers: Record<string, string> = { 'Content-Type': 'application/json' }
      if (token) headers['Authorization'] = `Bearer ${token}`

      const res = await fetch('/api/agent/generate/stream', {
        method: 'POST',
        headers,
        body: JSON.stringify({ intent: genIntent }),
      })

      const reader = res.body?.getReader()
      if (!reader) throw new Error('No reader')

      const decoder = new TextDecoder()
      let fullText = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const chunk = decoder.decode(value, { stream: true })
        for (const line of chunk.split('\n')) {
          if (line.startsWith('data: ')) {
            try {
              const data = JSON.parse(line.slice(6))
              if (data.text) {
                fullText += data.text
                setRouteResult(prev => ({ ...prev, generated: { template: fullText }, generatedAt: Date.now() }))
              }
            } catch {}
          }
        }
      }
    } catch (ex: any) {
      setRouteResult(prev => ({ ...prev, error: '生成失败: ' + (ex.message || '网络错误，请重试') }))
    }
    setRouteLoading(false)
  }

  const handlePublishGenerated = (data: any) => {
    const prefill: any = { price: 0 }
    if (data.template) prefill.content = data.template
    if (data.description) prefill.description = data.description
    localStorage.setItem('publish_prefill', JSON.stringify(prefill))
    nav('/publish')
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSmartSearch()
  }

  return (
    <div style={{ display: 'flex', gap: 24, paddingTop: 24 }}>
      <aside style={{ width: 220, flexShrink: 0 }}>
        <div className="card" style={{ padding: 16, position: 'sticky', top: 80 }}>
          <h3 style={{ marginBottom: 16 }}>分类</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
            <button onClick={() => setQuery({ ...query, categoryId: undefined, page: 1 })}
              style={{ ...catBtnStyle, fontWeight: !query.categoryId ? 700 : 400, color: !query.categoryId ? '#4a90d9' : '#555' }}>
              全部
            </button>
            {categories.map(c => (
              <div key={c.id}>
                <button onClick={() => setQuery({ ...query, categoryId: c.id, page: 1 })}
                  style={{ ...catBtnStyle, fontWeight: query.categoryId === c.id ? 700 : 400, color: query.categoryId === c.id ? '#4a90d9' : '#555' }}>
                  {c.name}
                </button>
                {c.children?.map(sub => (
                  <button key={sub.id} onClick={() => setQuery({ ...query, categoryId: sub.id, page: 1 })}
                    style={{ ...catBtnStyle, paddingLeft: 28, fontSize: 13, fontWeight: query.categoryId === sub.id ? 700 : 400, color: query.categoryId === sub.id ? '#4a90d9' : '#777' }}>
                    {sub.name}
                  </button>
                ))}
              </div>
            ))}
          </div>
          <h3 style={{ marginTop: 24, marginBottom: 12 }}>标签</h3>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
            {tags.map(t => (
              <button key={t.id} onClick={() => setQuery({ ...query, tagId: query.tagId === t.id ? undefined : t.id, page: 1 })}
                style={{ ...tagBtnStyle, background: query.tagId === t.id ? '#4a90d9' : '#e8f0fe', color: query.tagId === t.id ? '#fff' : '#4a90d9' }}>
                {t.name}
              </button>
            ))}
          </div>
          <h3 style={{ marginTop: 24, marginBottom: 12 }}>排序</h3>
          <select className="input" value={query.sortBy || 'createTime'}
            onChange={e => setQuery({ ...query, sortBy: e.target.value, page: 1 })}>
            <option value="createTime">最新发布</option>
            <option value="viewCount">最多浏览</option>
            <option value="downloadCount">最多下载</option>
            <option value="avgRating">最高评分</option>
            <option value="price">价格排序</option>
          </select>
        </div>
      </aside>

      <div style={{ flex: 1, minWidth: 0 }}>
        {/* Smart Search Bar */}
        <div className="card" style={{ marginBottom: 16, padding: 20, background: 'linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)' }}>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <span style={{ fontSize: 18 }}>🤖</span>
            <input
              className="input"
              placeholder="告诉我你想做什么... 比如：有没有PPT的提示词？ / 帮我写一个翻译的提示词 / 我的余额还有多少"
              value={smartInput}
              onChange={e => setSmartInput(e.target.value)}
              onKeyDown={handleKeyDown}
              style={{ flex: 1, background: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.2)', color: '#fff', fontSize: 15 }}
            />
            <button className="btn-primary" onClick={handleSmartSearch} disabled={routeLoading}
              style={{ whiteSpace: 'nowrap', padding: '10px 20px' }}>
              {routeLoading ? '⏳ 思考中...' : '🚀 智能搜索'}
            </button>
          </div>

          {/* Route Result */}
          {routeResult && !routeResult.noResults && !routeResult.generated && (
            <div style={{ marginTop: 10, padding: '8px 12px', background: 'rgba(74,144,217,0.15)', borderRadius: 6, fontSize: 13, color: '#a0c4ff' }}>
              🤖 识别意图：{routeResult.target}.{routeResult.action}
              {routeResult.params?.keyword && ` → 搜索 "${routeResult.params.keyword}"`}
              {routeResult.params?.intent && ` → 生成 "${routeResult.params.intent}"`}
              {routeResult.suggestion && ` — ${routeResult.suggestion}`}
              {routeResult.confidence && ` (置信度: ${(routeResult.confidence * 100).toFixed(0)}%)`}
            </div>
          )}

          {/* No Results → Ask to Generate */}
          {routeResult?.noResults && !routeResult?.isRag && (
            <div className="card" style={{ marginTop: 12, padding: 16, background: '#fff', textAlign: 'center' }}>
              {routeResult.error ? (
                <p className="error" style={{ fontSize: 14, marginBottom: 12 }}>{routeResult.error}</p>
              ) : (
                <>
                  <p style={{ fontSize: 15, color: '#666', marginBottom: 12 }}>
                    😕 没找到关于「{routeResult.keyword || smartInput}」的提示词
                  </p>
                  <p style={{ fontSize: 14, color: '#888', marginBottom: 16 }}>
                    {routeResult.marketHits?.length > 0 ? '市场有相关提示词，但要不要让 AI 专门给你写一个？' : '要不要让 AI 帮你生成一个？'}
                  </p>
                </>
              )}
              {routeResult.marketHits?.length > 0 && (
                <div style={{ marginBottom: 16, display: 'flex', gap: 8, flexWrap: 'wrap', justifyContent: 'center' }}>
                  {routeResult.marketHits.map((p: any) => (
                    <Link key={p.id} to={`/prompt/${p.id}`} style={marketHitStyle}>
                      {p.title} {p.price > 0 ? `¥${p.price}` : '免费'}
                    </Link>
                  ))}
                </div>
              )}
              <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
                <button className="btn-primary" onClick={() => handleGenerate(routeResult)} disabled={routeLoading}>
                  {routeLoading ? '⏳ 生成中...' : '✨ 帮我生成'}
                </button>
                <button className="btn-primary" onClick={() => handleWorkflow(routeResult)} disabled={routeLoading}
                  style={{ background: '#e67e22' }}>
                  🚀 一条龙发布
                </button>
                <button className="btn-secondary" onClick={() => setRouteResult(null)}>不用了</button>
              </div>
            </div>
          )}

          {/* Generated Result */}
          {routeResult?.generated && (!routeResult.noResults || routeResult.isRag || routeResult.isWorkflow) && (
            <div className="card" style={{ marginTop: 12, padding: 16, background: '#fff' }}>
              <h4 style={{ marginBottom: 8, color: routeResult.isRag ? '#4a90d9' : routeResult.isWorkflow ? '#e67e22' : '#27ae60' }}>
                {routeResult.isRag ? '📖 知识库回答' : routeResult.isWorkflow ? '🚀 一条龙发布' : '✅ AI 已生成'}{routeLoading ? ' (处理中...)' : ''}
              </h4>
              {routeResult.isWorkflow && routeResult.workflowSteps && routeResult.workflowSteps.length > 0 && (
                <div style={{ marginBottom: 12, display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                  {['generate', 'optimize', 'evaluate', 'publish'].map(name => {
                    const step = routeResult.workflowSteps?.find((s: any) => s.step === name)
                    const done = step?.status === 'done'
                    const running = step?.status === 'running'
                    const err = step?.status === 'error'
                    return (
                      <span key={name} className="tag" style={{
                        background: done ? '#d4edda' : err ? '#f8d7da' : running ? '#fff3cd' : '#eee',
                        color: done ? '#155724' : err ? '#721c24' : running ? '#856404' : '#999',
                        fontSize: 12,
                      }}>
                        {name === 'generate' ? '🤖 生成' : name === 'optimize' ? '✨ 优化' : name === 'evaluate' ? '📊 评测' : '📤 发布'}
                        {done ? ' ✅' : err ? ' ❌' : running ? ' ⏳' : ''}
                      </span>
                    )
                  })}
                </div>
              )}
              <div style={{ fontSize: 14, lineHeight: 1.7, whiteSpace: 'pre-wrap' }}>
                {routeResult.generated.template || '...'}
              </div>
              {routeResult.workflowReport?.published && (
                <div style={{ marginTop: 8, color: '#27ae60', fontWeight: 600 }}>✅ 已自动发布到市场！</div>
              )}
              {routeResult.workflowReport && !routeResult.workflowReport.published && (
                <div style={{ marginTop: 8, color: '#856404' }}>⚠️ 评分 {routeResult.workflowReport.final_score}，未达标自动发布</div>
              )}
              {routeResult.ragSources?.length > 0 && (
                <div style={{ marginTop: 12, borderTop: '1px solid #eee', paddingTop: 10 }}>
                  <span style={{ fontSize: 12, color: '#999' }}>参考来源：</span>
                  {routeResult.ragSources.map((s: any, i: number) => (
                    <span key={i} className="tag" style={{ marginLeft: 6, fontSize: 11 }}>{s.source} · {s.title}</span>
                  ))}
                </div>
              )}
              {(routeResult.isRag || routeResult.isWorkflow) && (
                <div style={{ display: 'flex', gap: 8, marginTop: 12 }}>
                  <button className="btn-secondary" onClick={() => setRouteResult(null)}>关闭</button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Regular search & keyword fallback */}
        <div style={{ marginBottom: 16, display: 'flex', gap: 12 }}>
          <input className="input" placeholder="搜索提示词..." value={query.keyword || ''}
            onChange={e => setQuery({ ...query, keyword: e.target.value, page: 1 })}
            style={{ flex: 1 }} />
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: 60, color: '#999' }}>加载中...</div>
        ) : (
          <>
            <div style={{ display: 'grid', gap: 16 }}>
              {(prompts?.records || []).map(p => (
                <Link key={p.id} to={`/prompt/${p.id}`} style={{ textDecoration: 'none' }}>
                  <div className="card" style={{ display: 'flex', gap: 16, padding: 20, cursor: 'pointer', transition: 'box-shadow 0.2s' }}
                    onMouseEnter={e => (e.currentTarget.style.boxShadow = '0 4px 16px rgba(0,0,0,0.12)')}
                    onMouseLeave={e => (e.currentTarget.style.boxShadow = '0 2px 8px rgba(0,0,0,0.08)')}>
                    <div style={{ flex: 1 }}>
                      <h3 style={{ marginBottom: 8, color: '#1a1a2e' }}>{p.title}</h3>
                      <p style={{ color: '#666', fontSize: 14, marginBottom: 10, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                        {p.description || '暂无描述'}
                      </p>
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 10 }}>
                        {p.tags?.map(tag => <span key={tag} className="tag">{tag}</span>)}
                        {p.categoryName && <span className="tag">{p.categoryName}</span>}
                      </div>
                      <div style={{ display: 'flex', gap: 16, fontSize: 13, color: '#999' }}>
                        <span>👤 {p.userNickname}</span>
                        {p.avgRating > 0 && <span className="rating">★ {p.avgRating.toFixed(1)}</span>}
                        <span>👁 {p.viewCount}</span>
                        <span>⬇ {p.downloadCount}</span>
                      </div>
                    </div>
                    <div style={{ textAlign: 'right', flexShrink: 0 }}>
                      {p.price > 0 ? (
                        <span style={{ color: '#e74c3c', fontWeight: 700, fontSize: 18 }}>¥{p.price.toFixed(2)}</span>
                      ) : (
                        <span style={{ color: '#27ae60', fontWeight: 700 }}>免费</span>
                      )}
                      <div style={{ marginTop: 4 }}>
                        <span style={{ fontSize: 12, padding: '2px 8px', borderRadius: 4, background: p.status === 'PUBLISHED' ? '#d4edda' : '#fff3cd', color: p.status === 'PUBLISHED' ? '#155724' : '#856404' }}>
                          {p.status === 'PUBLISHED' ? '已发布' : p.status === 'DRAFT' ? '草稿' : '已下线'}
                        </span>
                      </div>
                    </div>
                  </div>
                </Link>
              ))}
              {(!prompts?.records || prompts.records.length === 0) && (
                <div style={{ textAlign: 'center', padding: 60, color: '#999' }}>暂无提示词</div>
              )}
            </div>
            <div style={{ display: 'flex', justifyContent: 'center', gap: 8, marginTop: 24, marginBottom: 40 }}>
              <button className="btn-secondary" disabled={query.page === 1} onClick={() => setQuery({ ...query, page: (query.page || 1) - 1 })}>上一页</button>
              <span style={{ lineHeight: '36px', fontSize: 14, color: '#666' }}>第 {query.page} / {Math.ceil((prompts?.total || 0) / (query.size || 20))} 页</span>
              <button className="btn-secondary" disabled={(query.page || 1) * (query.size || 20) >= (prompts?.total || 0)}
                onClick={() => setQuery({ ...query, page: (query.page || 1) + 1 })}>下一页</button>
            </div>
          </>
        )}
      </div>
    </div>
  )
}

const catBtnStyle: React.CSSProperties = { background: 'none', border: 'none', padding: '4px 12px', cursor: 'pointer', textAlign: 'left', fontSize: 14, borderRadius: 6 }
const tagBtnStyle: React.CSSProperties = { border: 'none', padding: '2px 10px', borderRadius: 12, fontSize: 12, cursor: 'pointer' }
const marketHitStyle: React.CSSProperties = { display: 'inline-block', padding: '6px 14px', background: '#e8f0fe', color: '#4a90d9', borderRadius: 8, fontSize: 13, textDecoration: 'none' }

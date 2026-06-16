import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import type { ApiResponse, PageResult, PromptListVO, CategoryTreeVO, PromptTag, PromptQueryDTO } from '../types'

export default function Home() {
  const [prompts, setPrompts] = useState<PageResult<PromptListVO> | null>(null)
  const [categories, setCategories] = useState<CategoryTreeVO[]>([])
  const [tags, setTags] = useState<PromptTag[]>([])
  const [query, setQuery] = useState<PromptQueryDTO>({ page: 1, size: 20 })
  const [loading, setLoading] = useState(true)

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

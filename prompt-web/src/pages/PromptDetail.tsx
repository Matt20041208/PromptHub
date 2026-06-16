import React, { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api } from '../api/client'
import type { ApiResponse, PromptDetailVO, ReviewVO, PageResult } from '../types'

export default function PromptDetail() {
  const { id } = useParams<{ id: string }>()
  const { user } = useAuth()
  const nav = useNavigate()
  const [prompt, setPrompt] = useState<PromptDetailVO | null>(null)
  const [reviews, setReviews] = useState<PageResult<ReviewVO> | null>(null)
  const [rating, setRating] = useState(5)
  const [reviewText, setReviewText] = useState('')
  const [fav, setFav] = useState(false)
  const [loading, setLoading] = useState(true)
  const [buying, setBuying] = useState(false)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    Promise.all([
      api.get<ApiResponse<PromptDetailVO>>(`/prompt/${id}`),
      api.get<ApiResponse<PageResult<ReviewVO>>>(`/review/list/${id}`),
    ]).then(([pr, rr]) => {
      if (pr.code === 200) setPrompt(pr.data)
      if (rr.code === 200) setReviews(rr.data)
    }).catch(() => {}).finally(() => setLoading(false))

    if (user) {
      api.get<ApiResponse<boolean>>(`/review/favorite/check/${id}`).then(r => {
        if (r.code === 200) setFav(r.data)
      }).catch(() => {})
    }
  }, [id, user])

  const submitReview = async () => {
    if (!user) { alert('请先登录'); return }
    try {
      const res = await api.post<ApiResponse<null>>('/review', { promptId: Number(id), rating, content: reviewText })
      if (res.code === 200) {
        alert('评价成功')
        setReviewText('')
        const rr = await api.get<ApiResponse<PageResult<ReviewVO>>>(`/review/list/${id}`)
        if (rr.code === 200) setReviews(rr.data)
      }
    } catch (ex: any) { alert(ex.message) }
  }

  const buy = async () => {
    if (!user) { alert('请先登录'); return }
    const payNow = confirm('💳 模拟支付测试\n\n点击「确定」→ 模拟支付成功，发送通知\n点击「取消」→ 仅生成待支付订单')
    setBuying(true)
    try {
      const order = await api.post<ApiResponse<string>>('/trade/order', { promptId: Number(id) })
      if (order.code === 200 && order.data) {
        if (payNow) {
          const pay = await api.post<ApiResponse<null>>(`/trade/order/${order.data}/pay`)
          if (pay.code === 200) {
            alert('✅ 购买成功！订单已支付，通知已通过消息队列发送')
            const pr = await api.get<ApiResponse<PromptDetailVO>>(`/prompt/${id}`)
            if (pr.code === 200) setPrompt(pr.data)
          } else {
            alert('支付失败：' + pay.message)
          }
        } else {
          alert('📋 订单已生成（订单号：' + order.data + '），状态：待支付\n可在「我的」→「订单」中查看')
        }
      }
    } catch (ex: any) { alert(ex.message || '操作失败，请先充值余额') }
    setBuying(false)
  }

  const toggleFav = async () => {
    if (!user) { alert('请先登录'); return }
    try {
      if (fav) {
        await api.delete<ApiResponse<null>>(`/review/favorite/${id}`)
        setFav(false)
      } else {
        await api.post<ApiResponse<null>>(`/review/favorite/${id}`)
        setFav(true)
      }
    } catch (ex: any) { alert(ex.message) }
  }

  if (loading) return <div style={{ textAlign: 'center', padding: 60 }}>加载中...</div>
  if (!prompt) return <div style={{ textAlign: 'center', padding: 60 }}>提示词不存在</div>

  return (
    <div style={{ paddingTop: 24 }}>
      <div className="card" style={{ marginBottom: 24 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start' }}>
          <div>
            <h1 style={{ fontSize: 24, marginBottom: 12 }}>{prompt.title}</h1>
            <div style={{ display: 'flex', gap: 16, fontSize: 14, color: '#888', marginBottom: 12 }}>
              <span>👤 {prompt.userNickname}</span>
              <span>{prompt.categoryName}</span>
              <span className="rating">★ {prompt.avgRating.toFixed(1)}</span>
              <span>👁 {prompt.viewCount}</span>
              <span>⬇ {prompt.downloadCount}</span>
            </div>
            <div style={{ display: 'flex', gap: 8, marginBottom: 16 }}>
              {prompt.tags?.map(t => <span key={t} className="tag">{t}</span>)}
            </div>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'center' }}>
            <span style={{ fontSize: 28, fontWeight: 700, color: prompt.price > 0 ? '#e74c3c' : '#27ae60' }}>
              {prompt.price > 0 ? `¥${prompt.price.toFixed(2)}` : '免费'}
            </span>
            {prompt.price > 0 && !prompt.purchased && (
              <button className="btn-primary" onClick={buy} disabled={buying} style={{ padding: '10px 32px' }}>
                {buying ? '处理中...' : '立即购买'}
              </button>
            )}
            {prompt.price > 0 && prompt.purchased && (
              <span className="tag" style={{ color: '#27ae60', background: '#d4edda', padding: '6px 16px' }}>✅ 已购买</span>
            )}
            <button className="btn-secondary" onClick={toggleFav} style={{ padding: '6px 20px' }}>
              {fav ? '❤️ 已收藏' : '🤍 收藏'}
            </button>
          </div>
        </div>

        {prompt.description && (
          <div style={{ marginTop: 16, padding: 16, background: '#f8f9fa', borderRadius: 8 }}>
            <h4 style={{ marginBottom: 8 }}>描述</h4>
            <p style={{ color: '#555', lineHeight: 1.6, whiteSpace: 'pre-wrap' }}>{prompt.description}</p>
          </div>
        )}
        <div style={{ marginTop: 16, padding: 16, background: '#1a1a2e', borderRadius: 8, position: 'relative' }}>
          <h4 style={{ color: '#aaa', marginBottom: 8 }}>提示词内容</h4>
          <pre style={{ color: '#e0e0e0', whiteSpace: 'pre-wrap', fontFamily: 'monospace', fontSize: 14, lineHeight: 1.6 }}>{prompt.content}</pre>
          {prompt.contentLocked && (
            <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: '60%', background: 'linear-gradient(transparent, #1a1a2e 80%)', borderRadius: '0 0 8px 8px', display: 'flex', alignItems: 'flex-end', justifyContent: 'center', paddingBottom: 20 }}>
              <span style={{ color: '#ffc107', fontSize: 16, fontWeight: 600 }}>🔒 购买后解锁完整内容</span>
            </div>
          )}
        </div>
      </div>

      <div className="card" style={{ marginBottom: 24 }}>
        <h3 style={{ marginBottom: 16 }}>评价 ({reviews?.total || 0})</h3>
        {user && (
          <div style={{ display: 'flex', gap: 12, marginBottom: 20, alignItems: 'center' }}>
            <select className="input" style={{ width: 80 }} value={rating} onChange={e => setRating(Number(e.target.value))}>
              {[5, 4, 3, 2, 1].map(n => <option key={n} value={n}>{'★'.repeat(n)}</option>)}
            </select>
            <input className="input" placeholder="写下你的评价..." value={reviewText} onChange={e => setReviewText(e.target.value)} style={{ flex: 1 }} />
            <button className="btn-primary" onClick={submitReview}>发表</button>
          </div>
        )}
        {reviews?.records?.map(r => (
          <div key={r.id} style={{ padding: '12px 0', borderBottom: '1px solid #eee' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
              <span style={{ fontWeight: 500 }}>{r.userNickname}</span>
              <span className="rating">{'★'.repeat(r.rating)}</span>
            </div>
            <p style={{ color: '#555', fontSize: 14 }}>{r.content || '无内容'}</p>
            <span style={{ color: '#aaa', fontSize: 12 }}>{new Date(r.createTime).toLocaleDateString()}</span>
          </div>
        ))}
        {(!reviews?.records || reviews.records.length === 0) && (
          <div style={{ color: '#999', textAlign: 'center', padding: 20 }}>暂无评价</div>
        )}
      </div>
    </div>
  )
}

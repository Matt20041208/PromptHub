import React, { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { api } from '../api/client'
import type { ApiResponse, PageResult, OrderVO, UserBalance, NotificationVO } from '../types'
import { useNavigate } from 'react-router-dom'

export default function Dashboard() {
  const { user } = useAuth()
  const nav = useNavigate()
  const [tab, setTab] = useState<'orders' | 'notify' | 'profile'>('orders')
  const [orders, setOrders] = useState<PageResult<OrderVO> | null>(null)
  const [notifications, setNotifications] = useState<PageResult<NotificationVO> | null>(null)
  const [balance, setBalance] = useState<UserBalance | null>(null)
  const [unread, setUnread] = useState(0)
  const [rechargeAmount, setRechargeAmount] = useState(100)
  const [profileForm, setProfileForm] = useState({ nickname: '', email: '', bio: '' })

  useEffect(() => {
    if (!user) { nav('/login'); return }
    loadOrders()
    loadBalance()
    loadNotifications()
    loadUnread()
    setProfileForm({ nickname: user.nickname || '', email: user.email || '', bio: '' })
  }, [user])

  const loadOrders = async () => {
    try { const r = await api.get<ApiResponse<PageResult<OrderVO>>>('/trade/order/list'); if (r.code === 200) setOrders(r.data) } catch {}
  }
  const loadBalance = async () => {
    try { const r = await api.get<ApiResponse<UserBalance>>('/trade/balance'); if (r.code === 200) setBalance(r.data) } catch {}
  }
  const loadNotifications = async () => {
    try { const r = await api.get<ApiResponse<PageResult<NotificationVO>>>('/notify/list'); if (r.code === 200) setNotifications(r.data) } catch {}
  }
  const loadUnread = async () => {
    try { const r = await api.get<ApiResponse<number>>('/notify/unread-count'); if (r.code === 200) setUnread(r.data) } catch {}
  }

  const recharge = async () => {
    if (!confirm(`💴 模拟充值测试\n\n确认充值 ¥${rechargeAmount} 到账户？`)) return
    try {
      const r = await api.post<ApiResponse<null>>(`/trade/recharge?amount=${rechargeAmount}`)
      if (r.code === 200) {
        alert(`✅ 充值成功！已到账 ¥${rechargeAmount}`)
        loadBalance()
      }
      else alert(r.message)
    } catch (ex: any) { alert(ex.message) }
  }

  const buyVip = async () => {
    if (!confirm('👑 秒杀购买超级VIP\n\n售价：¥999\n权益：所有提示词免费查看\n\n限量100份，先到先得！\n\n确认购买？')) return
    try {
      const r = await api.post<ApiResponse<string>>('/user/vip/buy')
      if (r.code === 200) { alert('🎉 ' + r.data); window.location.reload() }
      else alert('❌ ' + (r.message || r as any))
    } catch (ex: any) { alert(ex.message) }
  }

  const markRead = async (id: number) => {
    await api.put(`/notify/${id}/read`)
    loadNotifications()
    loadUnread()
  }

  const markAllRead = async () => {
    await api.put('/notify/read-all')
    loadNotifications()
    setUnread(0)
  }

  const updateProfile = async () => {
    try {
      const r = await api.put<ApiResponse<null>>('/user/info', {
        nickname: profileForm.nickname,
        email: profileForm.email,
        bio: profileForm.bio,
      })
      if (r.code === 200) alert('更新成功')
      else alert(r.message)
    } catch (ex: any) { alert(ex.message) }
  }

  if (!user) return null

  return (
    <div style={{ paddingTop: 24 }}>
      <div style={{ display: 'flex', gap: 16, marginBottom: 24, borderBottom: '2px solid #eee', paddingBottom: 12 }}>
        {(['orders', 'notify', 'profile'] as const).map(t => (
          <button key={t} onClick={() => setTab(t)}
            style={{ ...tabStyle, color: tab === t ? '#4a90d9' : '#888', borderBottom: tab === t ? '2px solid #4a90d9' : '2px solid transparent' }}>
            {t === 'orders' ? '我的订单' : t === 'notify' ? `通知${unread > 0 ? ` (${unread})` : ''}` : '个人信息'}
          </button>
        ))}
        <div style={{ marginLeft: 'auto', display: 'flex', alignItems: 'center', gap: 12 }}>
          <span style={{ fontSize: 14, color: '#666' }}>
            余额: <b style={{ color: '#27ae60' }}>¥{balance?.balance.toFixed(2) || '0.00'}</b>
          </span>
          <div style={{ display: 'flex', gap: 8 }}>
            <input className="input" type="number" min="1" value={rechargeAmount}
              onChange={e => setRechargeAmount(Number(e.target.value))} style={{ width: 80 }} />
            <button className="btn-primary" onClick={recharge}>充值</button>
            {!user.vip && (
              <button className="btn-primary" onClick={buyVip} style={{ background: '#e74c3c', marginLeft: 8 }}>
                👑 VIP ¥999
              </button>
            )}
            {user.vip && (
              <span style={{ background: '#f39c12', color: '#fff', padding: '6px 14px', borderRadius: 8, fontWeight: 700, fontSize: 14, marginLeft: 8 }}>
                👑 超级VIP
              </span>
            )}
          </div>
        </div>
      </div>

      {tab === 'orders' && (
        <div>
          {(!orders?.records || orders.records.length === 0) ? (
            <div style={{ textAlign: 'center', padding: 40, color: '#999' }}>暂无订单</div>
          ) : (
            orders.records.map(o => (
              <div key={o.orderNo} className="card" style={{ marginBottom: 12, padding: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <h4>{o.promptTitle}</h4>
                    <div style={{ fontSize: 13, color: '#888', marginTop: 4 }}>
                      <span>订单号: {o.orderNo}</span>
                      <span style={{ marginLeft: 16 }}>卖家: {o.sellerNickname}</span>
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontWeight: 700, color: '#e74c3c' }}>¥{o.amount.toFixed(2)}</div>
                    <span style={{
                      fontSize: 12, padding: '2px 8px', borderRadius: 4,
                      background: o.status === 'PAID' ? '#d4edda' : o.status === 'UNPAID' ? '#fff3cd' : '#f8d7da',
                      color: o.status === 'PAID' ? '#155724' : o.status === 'UNPAID' ? '#856404' : '#721c24',
                    }}>
                      {o.status === 'PAID' ? '已支付' : o.status === 'UNPAID' ? '待支付' : o.status === 'CANCELLED' ? '已取消' : '已退款'}
                    </span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'notify' && (
        <div>
          {notifications && notifications.total > 0 && (
            <div style={{ marginBottom: 12, textAlign: 'right' }}>
              <button className="btn-secondary" onClick={markAllRead}>全部标为已读</button>
            </div>
          )}
          {(!notifications?.records || notifications.records.length === 0) ? (
            <div style={{ textAlign: 'center', padding: 40, color: '#999' }}>暂无通知</div>
          ) : (
            notifications.records.map(n => (
              <div key={n.id} className="card" style={{ marginBottom: 8, padding: 16, background: n.isRead ? '#fff' : '#f0f7ff', cursor: 'pointer' }}
                onClick={() => !n.isRead && markRead(n.id)}>
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <h4 style={{ color: n.isRead ? '#888' : '#333' }}>{n.title}</h4>
                  <span style={{ fontSize: 12, color: '#aaa' }}>{new Date(n.createTime).toLocaleString()}</span>
                </div>
                <p style={{ fontSize: 14, color: n.isRead ? '#999' : '#555', marginTop: 4 }}>{n.content}</p>
              </div>
            ))
          )}
        </div>
      )}

      {tab === 'profile' && (
        <div className="card" style={{ maxWidth: 500 }}>
          <h3 style={{ marginBottom: 20 }}>个人信息</h3>
          <div style={{ marginBottom: 16 }}>
            <label className="label">用户名</label>
            <input className="input" value={user.username} disabled style={{ background: '#f5f5f5' }} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">昵称</label>
            <input className="input" value={profileForm.nickname}
              onChange={e => setProfileForm({ ...profileForm, nickname: e.target.value })} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">邮箱</label>
            <input className="input" value={profileForm.email}
              onChange={e => setProfileForm({ ...profileForm, email: e.target.value })} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">简介</label>
            <textarea className="input" rows={3} value={profileForm.bio}
              onChange={e => setProfileForm({ ...profileForm, bio: e.target.value })} />
          </div>
          <button className="btn-primary" onClick={updateProfile}>保存修改</button>
        </div>
      )}
    </div>
  )
}

const tabStyle: React.CSSProperties = { background: 'none', border: 'none', fontSize: 16, padding: '8px 20px', cursor: 'pointer', marginBottom: -2 }

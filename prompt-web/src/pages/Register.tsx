import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { api } from '../api/client'
import type { ApiResponse, RegisterRequest } from '../types'

export default function Register() {
  const [form, setForm] = useState<RegisterRequest>({ username: '', password: '', email: '', nickname: '' })
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)
  const nav = useNavigate()

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErr('')
    if (!form.username || form.username.length < 3) { setErr('用户名至少3个字符'); return }
    if (!form.password || form.password.length < 6) { setErr('密码至少6个字符'); return }
    setLoading(true)
    try {
      const res = await api.post<ApiResponse<null>>('/user/register', form)
      if (res.code === 200) {
        alert('注册成功，请登录')
        nav('/login')
      } else {
        setErr(res.message || '注册失败')
      }
    } catch (ex: any) { setErr(ex.message) }
    setLoading(false)
  }

  return (
    <div style={{ maxWidth: 420, margin: '80px auto' }}>
      <div className="card">
        <h2 style={{ textAlign: 'center', marginBottom: 24 }}>注册账号</h2>
        <form onSubmit={submit}>
          <div style={{ marginBottom: 16 }}>
            <label className="label">用户名 *</label>
            <input className="input" placeholder="至少3个字符" value={form.username}
              onChange={e => setForm({ ...form, username: e.target.value })} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">昵称</label>
            <input className="input" placeholder="显示名称" value={form.nickname}
              onChange={e => setForm({ ...form, nickname: e.target.value })} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">邮箱</label>
            <input className="input" type="email" placeholder="可选" value={form.email}
              onChange={e => setForm({ ...form, email: e.target.value })} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">密码 *</label>
            <input className="input" type="password" placeholder="至少6个字符" value={form.password}
              onChange={e => setForm({ ...form, password: e.target.value })} />
          </div>
          {err && <div className="error" style={{ marginBottom: 12 }}>{err}</div>}
          <button className="btn-primary" type="submit" disabled={loading} style={{ width: '100%' }}>
            {loading ? '注册中...' : '注册'}
          </button>
        </form>
        <p style={{ textAlign: 'center', marginTop: 16, fontSize: 14, color: '#888' }}>
          已有账号？<Link to="/login">立即登录</Link>
        </p>
      </div>
    </div>
  )
}

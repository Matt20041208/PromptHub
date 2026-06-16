import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { api } from '../api/client'
import type { ApiResponse, LoginResultDTO, LoginRequest } from '../types'

export default function Login() {
  const [form, setForm] = useState<LoginRequest>({ username: '', password: '' })
  const [err, setErr] = useState('')
  const [loading, setLoading] = useState(false)
  const { login: doLogin } = useAuth()
  const nav = useNavigate()

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setErr('')
    if (!form.username || !form.password) { setErr('请填写所有字段'); return }
    setLoading(true)
    try {
      const res = await api.post<ApiResponse<LoginResultDTO>>('/user/login', form)
      if (res.code === 200 && res.data) {
        doLogin(res.data.token, res.data.user)
        nav('/')
      } else {
        setErr(res.message || '登录失败')
      }
    } catch (ex: any) { setErr(ex.message) }
    setLoading(false)
  }

  return (
    <div style={{ maxWidth: 420, margin: '80px auto' }}>
      <div className="card">
        <h2 style={{ textAlign: 'center', marginBottom: 24 }}>登录提示词市场</h2>
        <form onSubmit={submit}>
          <div style={{ marginBottom: 16 }}>
            <label className="label">用户名</label>
            <input className="input" placeholder="请输入用户名" value={form.username}
              onChange={e => setForm({ ...form, username: e.target.value })} />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">密码</label>
            <input className="input" type="password" placeholder="请输入密码" value={form.password}
              onChange={e => setForm({ ...form, password: e.target.value })} />
          </div>
          {err && <div className="error" style={{ marginBottom: 12 }}>{err}</div>}
          <button className="btn-primary" type="submit" disabled={loading} style={{ width: '100%' }}>
            {loading ? '登录中...' : '登录'}
          </button>
        </form>
        <p style={{ textAlign: 'center', marginTop: 16, fontSize: 14, color: '#888' }}>
          还没有账号？<Link to="/register">立即注册</Link>
        </p>
      </div>
    </div>
  )
}

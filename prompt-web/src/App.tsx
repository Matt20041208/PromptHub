import React from 'react'
import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Home from './pages/Home'
import Login from './pages/Login'
import Register from './pages/Register'
import PromptDetail from './pages/PromptDetail'
import PublishPrompt from './pages/PublishPrompt'
import Dashboard from './pages/Dashboard'
import Agent from './pages/Agent'

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
          <NavBar />
          <main style={{ flex: 1, padding: '0 16px', maxWidth: 1200, margin: '0 auto', width: '100%' }}>
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/prompt/:id" element={<PromptDetail />} />
              <Route path="/publish" element={<PublishPrompt />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/agent" element={<Agent />} />
            </Routes>
          </main>
        </div>
      </BrowserRouter>
    </AuthProvider>
  )
}

function NavBar() {
  const { user, logout } = useAuth()
  const nav = useNavigate()

  const handleLogout = async () => {
    const token = localStorage.getItem('token')
    if (token) {
      try { await fetch('/api/user/logout', { method: 'POST', headers: { 'Authorization': `Bearer ${token}` } }) } catch {}
    }
    logout()
    nav('/')
  }

  return (
    <nav style={{
      background: '#1a1a2e', color: '#fff', padding: '0 16px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      height: 56, position: 'sticky', top: 0, zIndex: 100,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
        <Link to="/" style={{ color: '#fff', textDecoration: 'none', fontSize: 20, fontWeight: 700 }}>
          💡 Prompt Market
        </Link>
        <Link to="/agent" style={linkStyle}>AI 工具</Link>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        {user ? (
          <>
            <Link to="/publish" style={{ ...linkStyle, background: '#4a90d9', padding: '6px 16px', borderRadius: 6 }}>
              + 发布
            </Link>
            <Link to="/dashboard" style={linkStyle}>
              {user.nickname || user.username}
            </Link>
            <button onClick={handleLogout} style={btnStyle}>退出</button>
          </>
        ) : (
          <>
            <Link to="/login" style={linkStyle}>登录</Link>
            <Link to="/register" style={{ ...linkStyle, background: '#4a90d9', padding: '6px 16px', borderRadius: 6 }}>注册</Link>
          </>
        )}
      </div>
    </nav>
  )
}

const linkStyle: React.CSSProperties = { color: '#e0e0ff', textDecoration: 'none', fontSize: 14, cursor: 'pointer' }
const btnStyle: React.CSSProperties = { background: 'none', border: '1px solid #666', color: '#ccc', padding: '4px 12px', borderRadius: 4, cursor: 'pointer' }

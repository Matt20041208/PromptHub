import React, { useState, useRef, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'

function genId() { return Date.now().toString(36) + Math.random().toString(36).slice(2) }

interface Message {
  role: 'user' | 'assistant' | 'system'
  content: string
}

export default function Chat() {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const streamingRef = useRef(false)
  const [sessionId] = useState(() => localStorage.getItem('chat_session') || genId())
  const [showPublishBtn, setShowPublishBtn] = useState(false)
  const [lastFullContent, setLastFullContent] = useState('')
  const bottomRef = useRef<HTMLDivElement>(null)
  const nav = useNavigate()

  useEffect(() => {
    localStorage.setItem('chat_session', sessionId)
  }, [sessionId])

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }) }, [messages])

  const send = useCallback(async () => {
    const msg = input.trim()
    if (!msg || streamingRef.current) return
    streamingRef.current = true
    streamingRef.current = true
    setInput('')
    setShowPublishBtn(false)

    setMessages(prev => [...prev, { role: 'user', content: msg }])
    setStreaming(true)

    const assistantMsg: Message = { role: 'assistant', content: '' }
    setMessages(prev => [...prev, assistantMsg])

    try {
      const token = localStorage.getItem('token')
      const headers: Record<string, string> = { 'Content-Type': 'application/json' }
      if (token) headers['Authorization'] = `Bearer ${token}`

      const res = await fetch('/api/agent/chat', {
        method: 'POST', headers,
        body: JSON.stringify({ sessionId, message: msg }),
      })

      const reader = res.body?.getReader()
      if (!reader) throw new Error('No reader')
      const decoder = new TextDecoder()
      let full = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        const chunk = decoder.decode(value, { stream: true })
        for (const line of chunk.split('\n')) {
          if (line.startsWith('data: ')) {
            try {
              const data = JSON.parse(line.slice(6))
              if (data.error) { full += `\n[错误: ${data.error}]` }
              else if (data.text) { full += data.text }
              else if (data.done) {
                full = data.full || full
                if (data.action === 'publish') setShowPublishBtn(true)
                setLastFullContent(data.full || full)
              }
              setMessages(prev => {
                const copy = [...prev]
                copy[copy.length - 1] = { role: 'assistant', content: full }
                return copy
              })
            } catch {}
          }
        }
      }
    } catch (ex: any) {
      setMessages(prev => {
        const copy = [...prev]
        copy[copy.length - 1] = { role: 'assistant', content: '网络错误，请重试' }
        return copy
      })
    }
    setStreaming(false)
    streamingRef.current = false
  }, [input, sessionId])

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send() }
  }

  const publish = () => {
    const prefill: any = { price: 0 }
    if (lastFullContent) prefill.content = lastFullContent
    localStorage.setItem('publish_prefill', JSON.stringify(prefill))
    nav('/publish')
  }

  const clearChat = async () => {
    try { await fetch(`/api/agent/chat/${sessionId}`, { method: 'DELETE' }) } catch {}
    setMessages([])
    setShowPublishBtn(false)
  }

  return (
    <div style={{ maxWidth: 800, margin: '24px auto', display: 'flex', flexDirection: 'column', height: 'calc(100vh - 120px)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <h2>💬 多轮对话优化</h2>
        <button className="btn-secondary" onClick={clearChat} style={{ fontSize: 13 }}>🗑 清空对话</button>
      </div>

      <div style={{ flex: 1, overflow: 'auto', border: '1px solid #eee', borderRadius: 12, padding: 16, background: '#fafafa', marginBottom: 16 }}>
        {messages.length === 0 && (
          <div style={{ textAlign: 'center', color: '#999', padding: 60 }}>
            <p style={{ fontSize: 18, marginBottom: 12 }}>🤖 提示词优化助手</p>
            <p style={{ fontSize: 14 }}>你可以对我说：</p>
            <p style={{ fontSize: 13, color: '#aaa', marginTop: 8 }}>
              "帮我写一个翻译的提示词"<br />
              "这个提示词怎么优化"<br />
              "帮我分类一下这个提示词"<br />
              "精简到50字以内"
            </p>
          </div>
        )}
        {messages.map((m, i) => (
          <div key={i} style={{
            marginBottom: 16, display: 'flex', justifyContent: m.role === 'user' ? 'flex-end' : 'flex-start'
          }}>
            <div style={{
              maxWidth: '80%', padding: '10px 16px', borderRadius: 12,
              background: m.role === 'user' ? '#4a90d9' : m.role === 'system' ? '#f0f0f0' : '#fff',
              color: m.role === 'user' ? '#fff' : '#333',
              boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
              whiteSpace: 'pre-wrap',
              fontSize: 14, lineHeight: 1.6,
            }}>
              {m.content || (i === messages.length - 1 && streaming ? '⏳ ...' : '')}
            </div>
          </div>
        ))}
        {showPublishBtn && (
          <div style={{ textAlign: 'center', marginTop: 8 }}>
            <button className="btn-primary" onClick={publish}>📤 发布到市场</button>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      <div style={{ display: 'flex', gap: 8 }}>
        <textarea className="input" rows={2} placeholder="输入你的需求...（Shift+Enter 换行）" value={input}
          onChange={e => setInput(e.target.value)} onKeyDown={handleKeyDown}
          style={{ flex: 1, resize: 'none' }} />
        <button className="btn-primary" onClick={send} disabled={streaming || !input.trim()}
          style={{ alignSelf: 'flex-end', height: 52 }}>
          {streaming ? '⏳' : '发送'}
        </button>
      </div>
    </div>
  )
}

import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'

const globalStyles = `
  * { margin: 0; padding: 0; box-sizing: border-box; }
  body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #f5f5f5; color: #333; }
  a { color: #4a90d9; text-decoration: none; }
  button { font-family: inherit; }
  input, textarea, select { font-family: inherit; font-size: 14px; }
  .card { background: #fff; border-radius: 12px; padding: 24px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
  .btn-primary { background: #4a90d9; color: #fff; border: none; padding: 10px 24px; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 500; }
  .btn-primary:hover { background: #357abd; }
  .btn-primary:disabled { background: #a0c4e8; cursor: not-allowed; }
  .btn-secondary { background: #fff; color: #4a90d9; border: 1px solid #4a90d9; padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 14px; }
  .btn-secondary:hover { background: #f0f6ff; }
  .btn-danger { background: #e74c3c; color: #fff; border: none; padding: 8px 20px; border-radius: 8px; cursor: pointer; font-size: 14px; }
  .input { width: 100%; padding: 10px 12px; border: 1px solid #ddd; border-radius: 8px; font-size: 14px; transition: border-color 0.2s; }
  .input:focus { outline: none; border-color: #4a90d9; }
  .label { display: block; margin-bottom: 6px; font-size: 14px; font-weight: 500; color: #555; }
  .error { color: #e74c3c; font-size: 13px; margin-top: 4px; }
  .tag { display: inline-block; padding: 2px 10px; background: #e8f0fe; color: #4a90d9; border-radius: 12px; font-size: 12px; }
  .rating { color: #f39c12; font-size: 14px; }
`

const styleEl = document.createElement('style')
styleEl.textContent = globalStyles
document.head.appendChild(styleEl)

ReactDOM.createRoot(document.getElementById('root')!).render(<App />)

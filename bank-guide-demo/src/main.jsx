import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import AgentApp from './AgentApp.jsx'

// 고객 화면은 '/', 상담원 화면은 '/agent'
const isAgent = window.location.pathname.startsWith('/agent')

createRoot(document.getElementById('root')).render(
  <StrictMode>{isAgent ? <AgentApp /> : <App />}</StrictMode>,
)

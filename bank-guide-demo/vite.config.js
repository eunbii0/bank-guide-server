import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// 화면(5173)에서 '/api'로 보낸 요청을 Spring Boot 서버(8080)로 넘겨준다.
// 같은 주소에서 나간 요청처럼 보이므로 서버에 CORS 설정이 필요 없다.
const proxy = { '/api': 'http://localhost:8080' }

export default defineConfig({
  plugins: [react()],
  server: { proxy },
  preview: { proxy },
})

# bank-guide-demo (고객용 화면)

외국인 고객을 위한 은행 앱 AI 길안내 화면. 은행 앱 화면은 실제 앱처럼 한국어 그대로이고,
채팅과 안내 문구만 고객 언어로 나온다. 안내 문구에는 화면에 보이는 한국어 버튼 이름과 뜻이 함께 있다.
예) Tap '상품관리' (My Products) below

## 실행 순서

1. `bank-guide-server`를 IntelliJ에서 먼저 실행 (8080)
2. 이 폴더(`package.json`이 있는 곳)에서

```bash
npm install
npm run dev
```

3. http://localhost:5173 접속

## 파일

| 파일 | 역할 |
| --- | --- |
| `src/i18n.js` | 채팅창 문구 4개 언어 (ko, en, vi, zh) |
| `src/api/guideApi.js` | 서버 API 호출 |
| `vite.config.js` | `/api` 요청을 localhost:8080으로 넘기는 프록시 |
| `src/App.jsx` | 화면 이동, 서버 호출, 고객 언어 상태, 통신 로그와 DB 기록 패널 |
| `src/components/ChatPanel.jsx` | 채팅창, 확인 버튼, 되묻기 선택지 버튼 |
| `src/components/TicketPreview.jsx` | 상담원에게 넘어간 티켓 미리보기 (5단계 상담원 화면과 같은 내용) |
| `src/components/GuideOverlay.jsx` | 서버가 준 `target` 버튼에 하이라이트 |
| `src/pages/Pages.jsx` | 은행 앱 화면들 (한국어). 안내 대상 버튼에는 id가 붙어 있음 |

## 시연 순서

1. "Need help? · 도움이 필요하세요?" → "I want to close my deposit early" → [Yes, that's right]
2. 영어 안내를 따라 상품관리 → 예금 → 중도해지 → 해지 신청 (중간에 '대출'을 누르면 뒤로가기 안내)
3. 베트남어 "Tôi muốn rút tiền gửi trước hạn", 중국어 "我想开存款余额证明"도 같은 방식
4. 오른쪽 로그의 `chat (LLM)` / `chat (KEYWORD)`로 어떤 방식으로 분류됐는지 확인
5. (4단계) 안내 중 7초 멈추기, [질문하기]로 "Sao tiền lãi ít vậy?" 묻기 → 상담원 연결 → 오른쪽에 상담 티켓
   - 멈춤 판단은 기본 7초이고, http://localhost:5173/?stuck=5 처럼 열면 그 시간(초)으로 바뀐다

## 다음 단계

5. 상담원 화면 + 번역 실시간 채팅 (WebSocket)

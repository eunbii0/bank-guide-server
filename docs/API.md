# API 약속

화면(frontend)과 서버(backend)가 주고받는 데이터 모양입니다.
이 문서를 바꿀 때는 팀에 먼저 알려주세요.

- 기본 주소: `http://localhost:8080`
- 모든 요청·응답은 JSON
- 시간은 ISO 8601 형식 (`2026-10-27T14:02:00`)

## 1. 이벤트 저장

`POST /api/events`

고객 화면에서 일어난 일을 하나씩 보냅니다.

```json
{
  "sessionId": "s-1234",
  "type": "INPUT_RETRY",
  "screen": "ACCOUNT_OPEN_NAME",
  "target": "englishNameInput",
  "lang": "en",
  "time": "2026-10-27T14:02:00",
  "meta": { "value": "NGUYEN VAN A", "count": 3 }
}
```

응답: `201 Created`

```json
{ "eventId": 1 }
```

### type 값

| type | 언제 |
|---|---|
| `SCREEN_VIEW` | 화면에 들어왔을 때 |
| `CLICK` | 버튼을 눌렀을 때 |
| `BACK` | 뒤로가기를 눌렀을 때 |
| `INPUT_RETRY` | 같은 입력칸을 다시 고쳤을 때 |
| `OCR_FAIL` | 여권/외국인등록증 인식 실패 |
| `ERROR` | 오류 팝업이 떴을 때 (`meta.code`에 오류코드) |
| `IDLE` | 일정 시간 아무것도 안 할 때 |
| `GUIDE_START` | AI 화면 가이드 시작 |
| `GUIDE_END` | 안내 끝내기 |
| `HANDOVER_REQUEST` | 상담원 연결 요청 |

## 2. AI 질문

`POST /api/chat`

```json
{
  "sessionId": "s-1234",
  "message": "Where can I send money abroad?",
  "lang": "en"
}
```

응답:

```json
{
  "intent": "OVERSEAS_REMITTANCE",
  "reply": "I'll show you step by step. Tap the highlighted button.",
  "steps": [
    { "screen": "HOME", "target": "menuButton", "text": "Tap Menu" },
    { "screen": "MENU", "target": "overseasRemittance", "text": "Tap Overseas Remittance" }
  ]
}
```

## 3. 막힘 감지 결과

`GET /api/sessions/{sessionId}/stuck`

서버가 이벤트 로그를 보고 막힘인지 판단합니다.

```json
{
  "stuck": true,
  "reason": "INPUT_RETRY_3",
  "message": "Having trouble entering your name?",
  "actions": [
    { "label": "Use passport name", "action": "RETRY_WITH_PASSPORT" },
    { "label": "Talk to an agent", "action": "HANDOVER" }
  ]
}
```

## 4. 상담원 연결 요청

`POST /api/handover`

```json
{ "sessionId": "s-1234" }
```

응답:

```json
{ "ticketId": 10, "waitingCount": 3 }
```

## 5. 상담 대기 목록 (상담원 화면)

`GET /api/agent/tickets`

```json
[
  {
    "ticketId": 10,
    "lang": "en",
    "summary": "영문명 입력 3회 수정 후 E-402 오류",
    "status": "WAITING",
    "createdAt": "2026-10-27T14:05:00"
  }
]
```

status 값: `WAITING`, `IN_PROGRESS`, `DONE`

## 6. 상담 상세 (상담원 화면)

`GET /api/agent/tickets/{ticketId}`

```json
{
  "ticketId": 10,
  "sessionId": "s-1234",
  "lang": "en",
  "status": "WAITING",
  "aiSummary": "고객이 계좌 개설 중 영문명을 3회 수정했고 외국인등록증 영문명 불일치(E-402) 오류가 발생함",
  "events": [
    { "type": "SCREEN_VIEW", "screen": "ACCOUNT_OPEN_NAME", "time": "2026-10-27T14:01:00" },
    { "type": "INPUT_RETRY", "screen": "ACCOUNT_OPEN_NAME", "time": "2026-10-27T14:02:00" },
    { "type": "ERROR", "screen": "ACCOUNT_OPEN_NAME", "time": "2026-10-27T14:03:00", "meta": { "code": "E-402" } }
  ],
  "chats": [
    { "role": "USER", "message": "My name doesn't work", "time": "2026-10-27T14:04:00" },
    { "role": "AI", "message": "Let's try your passport name.", "time": "2026-10-27T14:04:05" }
  ]
}
```

## DB 테이블 (초안)

| 테이블 | 주요 컬럼 |
|---|---|
| `session` | id, lang, created_at |
| `event_log` | id, session_id, type, screen, target, meta(JSON), created_at |
| `chat_message` | id, session_id, role, message, created_at |
| `ticket` | id, session_id, status, ai_summary, created_at |

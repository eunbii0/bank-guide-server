# API 약속

화면(bank-guide-demo)과 서버(bank-guide-server)가 주고받는 데이터 모양입니다.
이 문서를 바꿀 때는 팀에 먼저 알려주세요.

- 기본 주소: `http://localhost:8080` (화면에서는 `/api/...`로 보내면 Vite 프록시가 넘겨줍니다)
- 모든 요청·응답은 JSON
- 날짜는 `2027-05-31`, 고객에게 보이는 문구 안의 날짜는 `2027.05.31`
- 고객 언어(`lang`): `ko`, `en`, `vi`, `zh`. 그 밖의 언어는 `en`으로 안내합니다.

---

## 이번 변경 요약 (feature/chatbot-menu-eligibility)

### 🔧 A. 실제 서버에 맞춘 수정 (기능 변화 없음)

이전 문서는 초안이라 실제 서버와 주소·모양이 달랐습니다. **지금 동작하는 서버 기준으로 다시 썼습니다.**

| 이전 문서 (초안) | 실제 서버 | 비고 |
|---|---|---|
| `POST /api/events` | `POST /api/guide/event` | 화면 진입만 보냄. 클릭·입력 이벤트는 아직 없음 |
| `POST /api/chat` `{sessionId, message, lang}` | `POST /api/chat` `{conversationId, text}` | 언어는 서버(LLM)가 알아냄 |
| `GET /api/sessions/{id}/stuck` | `POST /api/guide/stuck` | 화면이 멈춤을 알리고 서버가 연결 제안 문구를 줌 |
| `POST /api/handover` | `POST /api/handoff` | |
| `GET /api/agent/tickets` | `GET /api/tickets` | |
| `GET /api/agent/tickets/{ticketId}` | `GET /api/tickets/{ticketNo}` | 티켓 번호 `T-20261027-0001` |
| (없음) | `POST /api/chat/choose`, `POST /api/guide/start`, `POST /api/guide/stop`, `GET /api/guide/{sessionId}/events` | 문서에 빠져 있던 API |

### 🆕 B. 새 API 추가·확장 (챗봇 메뉴 + 가입 자격 판단)

| 구분 | API | 내용 |
|---|---|---|
| 🆕 새로 | `GET /api/customers` | 데모용 가상 고객 목록 (시연 패널에서 고객 바꾸기) |
| 🆕 새로 | `POST /api/chat/menu` | 챗봇 첫 화면: "도움이 필요하세요?" + 메뉴 버튼 3개 |
| 🆕 새로 | `POST /api/eligibility` | 고객 정보 확인 [네]/[아니요] → 가입 자격 결과 + 상담원 연결 제안 |
| ✏️ 확장 | `POST /api/chat`, `POST /api/chat/choose` | 요청에 `customerId`, 응답에 `category` 추가. 새 응답 `intent: "VERIFY"` |
| ✏️ 확장 | `POST /api/guide/start` | 요청에 `term`(가입 기간, 개월) 추가 |
| ⛔ 변경 없음 | `POST /api/handoff`, `GET /api/tickets/...` | develop 그대로. [상담원 연결]을 누르면 `reason: "CUSTOMER_REQUEST"`로 부름 |

> 새로 추가한 필드는 모두 **선택값**입니다. 보내지 않으면 이전과 똑같이 동작합니다
> (`customerId`를 빼면 데모 기본 고객 `A`로 봅니다).

**범위**: 이 브랜치는 "상담원을 연결해 드릴까요?" 제안을 띄우는 단계까지입니다.
[상담원 연결]을 누른 뒤의 티켓 생성·AI 요약·상담원 화면은 팀원 작업과 나중에 합칩니다.
그때 필요한 정보는 `eligibility_check` 테이블에 저장해 두었습니다 (2부 8).

**개인정보 원칙**: 가입 자격은 서버의 규칙 테이블(`product_rule`)로만 판단하고 LLM에 맡기지 않습니다.
체류자격·만료일 같은 고객 정보는 LLM에 보내지 않습니다. 채팅 기록(`chat_message`)에는
`[고객 정보 확인 질문]`처럼 자리표시만 남기고, 실제 정보는 `customer_profile`과 `eligibility_check`에만 있습니다.

---

# 1부. 기존 API (🔧 실제 서버 기준으로 수정)

## 1. 채팅: 고객 문장 분류

`POST /api/chat`

```json
{ "conversationId": "c-1234", "text": "Tôi muốn tất toán tiền gửi", "customerId": "A" }
```

- `conversationId`: 화면을 처음 열 때 만든 값. 같은 고객의 대화를 하나로 묶습니다.
- `customerId`: ✏️ 새 필드 (선택). 아래 2부 참고.

응답 (`ChatResponse`)

```json
{
  "intent": "DEPOSIT_CANCEL",
  "confirmText": "Bạn muốn tất toán khoản tiền gửi có kỳ hạn trước ngày đáo hạn, đúng không?",
  "lang": "vi",
  "message": null,
  "options": [],
  "source": "LLM",
  "escalation": "NONE",
  "category": null
}
```

| intent | 뜻 | 화면이 할 일 |
|---|---|---|
| 업무 코드 (`DEPOSIT_CANCEL` 등) | 확실함 | `confirmText` + [네]/[아니요] |
| `AMBIGUOUS` | 애매함 | `message` + `options` 버튼 |
| `UNKNOWN` | 모름 | `message`만 표시 |
| `ADVICE` | 손익·상품 판단 질문 (`escalation: ADVICE`) | 상담원 연결 제안 |
| `FRAUD` | 금융사기 의심 (`escalation: FRAUD`) | 사기 우려 경고 + 전달 내용 안내 후 [상담원 연결] / [혼자 계속] 묻기 |
| 🆕 `MENU` | 챗봇 첫 메뉴 | `message` + `options` 버튼 (2부 2) |
| 🆕 `VERIFY` | 가입 자격 확인이 필요한 업무 | `message`(고객 정보 확인 질문) + [네]/[아니요] (2부 3) |

- `source`: `LLM` / `KEYWORD`(LLM 실패 시) / `CHOICE`(버튼 선택) / 🆕 `MENU`
- `category`: 🆕 `VERIFY`일 때 `CARD` 또는 `JOIN`, 그 밖에는 `null`

## 2. 채팅: 선택지 버튼

`POST /api/chat/choose`

```json
{ "conversationId": "c-1234", "intent": "DEPOSIT_CANCEL", "lang": "vi", "customerId": "A" }
```

응답: 1과 같은 `ChatResponse`. `source`는 `CHOICE`.

## 3. 안내 시작

`POST /api/guide/start`

```json
{ "conversationId": "c1a2...", "intent": "DEPOSIT_CANCEL", "page": "home", "lang": "en", "term": null }
```

`conversationId`(선택): 채팅과 같은 값. 보내면 `guide_event`에 함께 저장되어, 한 대화에서 시작한
여러 안내 기록을 묶어 볼 수 있습니다 (서비스 개선 분석용).

**개인정보 최소화**: 상담원 연결 때 요약에는 **지금 진행 중인 안내의 기록만** 들어갑니다.
같은 대화에서 그만둔 다른 안내 기록은 저장만 하고 상담원에게 넘기지 않습니다.
화면은 [상담원 연결] 버튼 아래에 "이 채팅 내용과 지금 진행 중인 안내 상황만 전달된다"는 문구를 보여줍니다.

응답

```json
{
  "sessionId": "9f1c...",
  "guide": { "type": "STEP", "step": 1, "total": 4, "target": "btn-product", "message": "Tap '상품관리' (My Products) below" }
}
```

`guide.type`: `STEP`(이 버튼을 누르세요) / `OFF_PATH`(다른 화면, 뒤로가기 안내) / `DONE`(목표 화면 도착)

## 4. 화면 진입

`POST /api/guide/event`

```json
{ "sessionId": "9f1c...", "page": "product" }
```

응답

```json
{ "guide": { "type": "STEP", "step": 2, "total": 4, "target": "btn-deposit", "message": "Tap '예금' (Deposits)" }, "offer": null }
```

`offer`: 경로 이탈이 3번째가 되는 순간에만 상담원 연결 제안 문구가 들어옵니다.

## 5. 멈춤 알림

`POST /api/guide/stuck`

```json
{ "sessionId": "9f1c...", "page": "deposit-cancel", "idleSec": 30 }
```

응답: `{ "offer": "Having trouble? Shall I connect you to an agent?" }`

## 6. 안내 그만하기

`POST /api/guide/stop` `{ "sessionId": "9f1c..." }` → `204 No Content`

## 7. 행동 기록

`GET /api/guide/{sessionId}/events`

```json
[ { "time": "14:02:00", "type": "PAGE_ENTER", "page": "product", "detail": "STEP 2/4" } ]
```

`type`: `GUIDE_START`, `PAGE_ENTER`, `STUCK`, `OFFER`, `GUIDE_STOP`, `HANDOFF`

## 8. 상담원 연결

`POST /api/handoff`

```json
{
  "conversationId": "c-1234",
  "sessionId": "9f1c...",
  "reason": "STUCK",
  "page": "deposit-cancel",
  "lang": "vi",
  "intent": "DEPOSIT_CANCEL",
  "customerId": "A"
}
```

`customerId`(선택): 티켓에 고객을 연결만 합니다. 고객 정보는 티켓에 복사하지 않고,
상담 중 고객이 허용해야 상담원이 볼 수 있습니다 (9-1).

응답

```json
{ "ticketNo": "T-20261027-0001", "queue": "FOREIGN", "message": "Đang kết nối với nhân viên tư vấn..." }
```

- `reason`: `ADVICE` / `STUCK` / `OFF_PATH` / `FRAUD` / `CUSTOMER_REQUEST`
- `queue`: `FOREIGN`(외국인 전담) / `FRAUD`(금융사기 대응) / `GENERAL`(한국어 고객)

## 9. 상담 티켓 (상담원 화면)

`GET /api/tickets` (최신순 목록), `GET /api/tickets/{ticketNo}` (하나)

```json
{
  "ticketNo": "T-20261027-0001",
  "queue": "FOREIGN",
  "status": "WAITING",
  "reason": "같은 화면에서 오래 멈춤",
  "lang": "베트남어",
  "intentName": "예금 중도해지",
  "currentPage": "중도해지 확인 화면",
  "summary": ["...", "...", "..."],
  "checkpoints": ["자금 용도와 필요 시점", "제3자의 해지·송금·계좌 대여 요구 여부"],
  "riskFlag": "NONE",
  "summarySource": "LLM",
  "messages": [ { "time": "14:04:00", "sender": "CUSTOMER", "lang": "vi", "content": "Sao tiền lãi ít vậy?" } ],
  "createdAt": "14:05:00",
  "hasCustomer": true,
  "infoConsent": "NONE"
}
```

`status`: `WAITING`, `IN_PROGRESS`, `CLOSED`

### 상담원 답장 번역 실패

`POST /api/tickets/{ticketNo}/reply  {"text": "...", "sendOriginal": false}`

- 번역이 실패하면 **보내지 않고** `422` + `message: "TRANSLATION_FAILED"`로 알립니다.
  상담원 화면이 [다시 번역] / [입력한 그대로 보내기] / [취소]를 띄웁니다 (쉬운 영어로 고쳐 쓴 뒤 보내도 됨).
- `sendOriginal: true`면 번역 없이 입력한 그대로 보냅니다. 상담원 화면 대화에 `시스템` 안내가 남고,
  고객 화면의 `agent-messages` 응답에서 그 답장은 `"untranslated": true`가 되어 고객 언어로 안내가 붙습니다.

## 9-1. 고객 정보 공유 (상담 중 동의)

고객 정보(체류자격·만료일 등)와 가입 자격 판단 결과는 **상담 중 고객이 허용했을 때만** 상담원 창에 보입니다.

```
상담원 POST /api/tickets/{ticketNo}/info-request       → infoConsent: REQUESTED
고객   GET  /api/tickets/{ticketNo}/agent-messages      → { ..., "infoConsent": "REQUESTED" } → [허용]/[거절] 카드
고객   POST /api/tickets/{ticketNo}/info-consent {"granted": true}   → GRANTED (false면 DENIED, 상담원은 다시 요청 가능)
상담원 GET  /api/tickets/{ticketNo}/customer-info?agent=demo-agent   → 고객 정보 + 자격 판단 결과 + 조회 기록
```

- `customer-info`는 `GRANTED`이고 상담이 끝나지 않았을 때만 응답합니다. 그 밖에는 `403`.
- 조회할 때마다 `customer_info_access`에 한 줄씩 남습니다 (누가, 언제, 어떤 항목). 응답의 `accessLog`로도 보입니다.
- 고객 정보는 채팅 기록(`chat_message`)에 넣지 않습니다. 채팅 기록은 LLM 요약·답변 추천의 재료라서입니다.
  요청·허용·거절은 `시스템` 메시지로만 남습니다.

`customer-info` 응답

```json
{
  "customerId": "A", "name": "Nguyen", "nationalityKo": "베트남", "visaType": "E-9",
  "stayExpiry": "2027-05-31", "hasArc": true, "hasAccount": true, "incomeVerified": true,
  "eligibility": [ { "time": "22:11:18", "category": "CARD", "confirmed": true, "result": ["[카드 발급] 고객 정보 확인: 맞음 ...", "신용카드 발급: 불가 - ..."] } ],
  "accessLog": [ { "time": "22:12:29", "agentId": "demo-agent", "items": "PROFILE,ELIGIBILITY" } ]
}
```

---

# 2부. 🆕 새 API (챗봇 메뉴 + 가입 자격 판단)

## 전체 흐름

```
챗봇 열기 ─ POST /api/chat/menu ─▶ "도움이 필요하세요?" [카드 발급] [상품 해지] [상품 가입]
                                         │
     ┌───────────────────────────────────┼─────────────────────────────┐
  [상품 해지]                      [카드 발급]/[상품 가입]          직접 입력 "신용카드 만들래요"
     │ POST /api/chat/choose            │ POST /api/chat/choose         │ POST /api/chat
     ▼                                  ▼                               ▼
  기존 DEPOSIT_CANCEL 확인 질문    intent=VERIFY  "Nguyen님, E-9 비자, 체류기간 2027.05.31까지로 확인돼요. 맞으세요?"
                                         │
             [네] ─ POST /api/eligibility confirmed=true ─▶ 상품별 가능/불가 + 이유 + 대안 + 기간
                                         │                     │                 │ 불가 상품이 있으면 offer
                                         │                     │ 상품(·기간) 선택  └─▶ [상담원 연결] ─┐
                                         │                     ▼                                     │
                                         │          POST /api/guide/start {intent, term}             │
                                         │          → 기존 화면 안내 (마지막 신청 버튼은 고객이 직접)   │
           [아니요] ─ POST /api/eligibility confirmed=false ─▶ 체류정보 갱신 안내 + offer             │
                                                               [상담원 연결] [혼자 계속]              │
                                                                     │                               │
                                                                     ▼                               ▼
                                              POST /api/handoff (develop 그대로, reason=CUSTOMER_REQUEST)
                                              ── 여기서부터는 상담원 연결 쪽(팀원) 담당 ──
```

- 기존 이체(`TRANSFER`), 증명서 발급(`CERTIFICATE`)은 메뉴에 없고, 직접 물어볼 때만 지금처럼 안내합니다.
- 상품 해지는 자격 판단 없이 바로 기존 확인 질문으로 갑니다.
- 기존 멈춤·이탈·판단 질문·사기 의심 연결 제안은 바뀌지 않았습니다.

## 1. 데모용 가상 고객 목록

`GET /api/customers`

```json
[
  { "customerId": "A", "name": "Nguyen", "nationality": "베트남", "visaType": "E-9", "stayExpiry": "2027-05-31", "lang": "vi" },
  { "customerId": "B", "name": "Li Wei", "nationality": "중국", "visaType": "F-5", "stayExpiry": null, "lang": "zh" },
  { "customerId": "C", "name": "Aziz", "nationality": "우즈베키스탄", "visaType": "D-2", "stayExpiry": "2026-12-15", "lang": "en" }
]
```

시연 패널 전용입니다 (실제 서비스라면 로그인한 고객 한 명만 서버가 압니다).
`lang`은 고객이 아직 아무 말도 안 했을 때 메뉴를 보여줄 언어입니다.

## 2. 챗봇 첫 메뉴

`POST /api/chat/menu`

```json
{ "conversationId": "c-1234", "customerId": "A", "lang": null }
```

`lang`을 비우면 고객의 선호 언어로 보여줍니다.

응답 (`ChatResponse`)

```json
{
  "intent": "MENU",
  "confirmText": null,
  "lang": "vi",
  "message": "Bạn cần giúp đỡ không? Hãy chọn bên dưới hoặc nhập câu hỏi.",
  "options": [
    { "intent": "MENU_CARD", "name": "Làm thẻ" },
    { "intent": "MENU_CANCEL", "name": "Tất toán sản phẩm" },
    { "intent": "MENU_JOIN", "name": "Đăng ký sản phẩm" }
  ],
  "source": "MENU",
  "escalation": "NONE",
  "category": null
}
```

메뉴 버튼을 누르면 기존 `POST /api/chat/choose`에 `intent`로 `MENU_CARD` / `MENU_CANCEL` / `MENU_JOIN`을 보냅니다.

## 3. 고객 정보 확인 질문 (`intent: VERIFY`)

`MENU_CARD`, `MENU_JOIN`을 고르거나, 고객이 직접 쓴 문장이 카드·가입 업무(`CHECK_CARD`, `CREDIT_CARD`,
`SAVINGS_OPEN`, `TIME_DEPOSIT_OPEN`)로 분류되면 업무 확인 질문 대신 아래 응답이 옵니다.

```json
{
  "intent": "VERIFY",
  "confirmText": null,
  "lang": "vi",
  "message": "Anh/chị Nguyen, ngân hàng ghi nhận thị thực E-9, thời hạn lưu trú đến 2027.05.31. Thông tin này đúng không?",
  "options": [],
  "source": "CHOICE",
  "escalation": "NONE",
  "category": "CARD"
}
```

화면은 [네]/[아니요]를 보여주고, 누르면 `POST /api/eligibility`를 부릅니다.

## 4. 가입 자격 확인

`POST /api/eligibility`

```json
{ "conversationId": "c-1234", "customerId": "A", "category": "CARD", "lang": "ko", "confirmed": true }
```

- `category`: `CARD`(체크카드·신용카드) / `JOIN`(정기예금·적금)
- `confirmed`: [네]면 `true`, [아니요]면 `false`

### [네] → 판단 결과 (`status: CHECKED`)

고객 A(E-9, 체류 만료 2027-05-31), 기준일 2026-10-27, `category: "CARD"`, `lang: "ko"`

```json
{
  "status": "CHECKED",
  "lang": "ko",
  "message": "확인 감사합니다. 고객님 정보로 확인한 가입 가능 여부예요.",
  "notice": "※ 데모용 가상 규칙으로 판단한 결과예요.",
  "products": [
    {
      "productCode": "CHECK_CARD",
      "intent": "CHECK_CARD",
      "name": "체크카드 발급",
      "eligible": true,
      "reasons": [],
      "note": "카드 유효기간은 체류 만료일(2027.05.31)까지예요.",
      "terms": [],
      "alternative": null
    },
    {
      "productCode": "CREDIT_CARD",
      "intent": "CREDIT_CARD",
      "name": "신용카드 발급",
      "eligible": false,
      "reasons": ["체류자격(E-9)으로는 신청할 수 없어요. (가능: F-2, F-4, F-5, F-6, E-7)"],
      "note": null,
      "terms": [],
      "alternative": { "productCode": "CHECK_CARD", "intent": "CHECK_CARD", "name": "체크카드 발급", "message": "대신 '체크카드 발급'은(는) 신청할 수 있어요." }
    }
  ],
  "offer": "가입 조건이 궁금하시면 상담원이 고객님 상황에 맞춰 안내해 드릴 수 있어요. 상담원을 연결해 드릴까요?"
}
```

같은 고객, `category: "JOIN"`일 때 상품 하나 (정기예금도 같은 모양)

```json
{
  "productCode": "SAVINGS",
  "intent": "SAVINGS_OPEN",
  "name": "적금 가입",
  "eligible": true,
  "reasons": [],
  "note": null,
  "terms": [
    { "months": 6,  "available": true,  "reason": null },
    { "months": 12, "available": false, "reason": "만기일(2027.10.27)이 체류 만료일(2027.05.31) 이후라서 선택할 수 없어요." },
    { "months": 24, "available": false, "reason": "만기일(2028.10.27)이 체류 만료일(2027.05.31) 이후라서 선택할 수 없어요." }
  ],
  "alternative": null
}
```

| 필드 | 뜻 |
|---|---|
| `eligible` | 가입 가능 여부 |
| `reasons` | 불가 이유 (고객 언어) |
| `note` | 가능하지만 알아둘 점. 예) 카드 유효기간 |
| `terms` | 예금·적금만. 기간별 선택 가능 여부. 선택한 `months`를 안내 시작의 `term`으로 보냄 |
| `alternative` | 불가일 때 대신 가능한 상품 (그 상품이 가능할 때만) |
| `offer` | 불가 상품이 하나라도 있으면 상담원 연결 제안 문구, 모두 가능하면 `null` |

상품을 고르면 `POST /api/guide/start`에 `intent`(+ 예금·적금은 `term`)를 보냅니다.

### [아니요] → 정보 불일치 (`status: MISMATCH`)

```json
{
  "status": "MISMATCH",
  "lang": "en",
  "message": "If your stay was extended, the bank needs your updated residence card info. You can update it at a branch with your new card, or an agent can help you.",
  "notice": null,
  "products": [],
  "offer": "Shall I connect you to an agent?"
}
```

### `offer`가 있을 때 화면이 할 일

- `offer` 문구와 [상담원 연결] [혼자 계속] 버튼을 띄웁니다 (기존 연결 제안 버튼과 같은 모양).
- [상담원 연결] → **기존 `POST /api/handoff`를 develop 모양 그대로** 부릅니다:
  `{ conversationId, sessionId: null, reason: "CUSTOMER_REQUEST", page, lang, intent: null }`
- 원래 사유(`PROFILE_MISMATCH` / `NOT_ELIGIBLE`)는 서버가 이미 `eligibility_check`에 저장해 두었습니다.

## 5. 안내 시작에 기간 추가 (✏️ 확장)

```json
{ "intent": "SAVINGS_OPEN", "page": "home", "lang": "vi", "term": 6 }
```

예금·적금 안내에서는 기간 버튼(`btn-term-6` 등)을 하이라이트하고, 안내 문구에 고른 기간이 들어갑니다.
`SAVINGS_OPEN`, `TIME_DEPOSIT_OPEN`은 `term`이 없으면 `400`입니다.

## 6. 새 안내 업무와 화면 버튼 id

화면의 버튼 `id`가 아래 `target`과 같아야 하이라이트가 걸립니다.

| 업무 | 1 | 2 | 3 | 4 | 목표 화면 |
|---|---|---|---|---|---|
| `CHECK_CARD` 체크카드 발급 | `home` `btn-card` | `card` `btn-card-check` | `card-check-apply` `btn-card-apply` | | `card-done` |
| `CREDIT_CARD` 신용카드 발급 | `home` `btn-card` | `card` `btn-card-credit` | `card-credit-apply` `btn-card-apply` | | `card-done` |
| `SAVINGS_OPEN` 적금 가입 | `home` `btn-join` | `join` `btn-join-savings` | `savings-join` `btn-term-{기간}` | `join-confirm` `btn-join-submit` | `join-done` |
| `TIME_DEPOSIT_OPEN` 정기예금 가입 | `home` `btn-join` | `join` `btn-join-deposit` | `deposit-join` `btn-term-{기간}` | `join-confirm` `btn-join-submit` | `join-done` |

## 7. 가입 자격 규칙 (데모용 가상 규칙)

| 상품 | 규칙 |
|---|---|
| 체크카드 | 외국인등록증 + 우리 은행 계좌. 카드 유효기간은 체류 만료일까지 |
| 신용카드 | 외국인등록증 + 체류자격 F-2, F-4, F-5, F-6, E-7 중 하나 + 남은 체류기간 6개월 이상 + 소득 확인 |
| 정기예금·적금 | 우리 은행 계좌 + 만기일이 체류 만료일 이전인 기간(6·12·24개월)만 선택 가능. 하나도 없으면 불가 |

- 체류 만료일이 없는 고객(F-5 등)은 기간 제한이 없습니다.
- "6개월 이상"은 `오늘 + 6개월 ≤ 체류 만료일`이면 통과입니다 (정확히 6개월도 가능).
- 규칙은 `product_rule` 테이블에 있습니다. 규칙을 바꿀 때는 코드가 아니라 데이터를 고칩니다.
- 판단 기준일은 실제 오늘입니다. 시연 날짜를 고정하려면 `application.yml`의 `demo.today`(또는 환경변수 `DEMO_TODAY`)에 `2026-10-27`처럼 넣습니다.

## 8. 상담원 연결 쪽에 넘기는 데이터 (팀원 작업과 합칠 때)

이 브랜치는 `HandoffService`·`HandoffNote`·`HandoffSummarizer`·`HandoffController`·`HandoffTicket`·`TicketView`·`TicketPreview.jsx`를 **수정하지 않았습니다.**
합칠 때 아래 데이터를 `conversationId`로 찾아 쓰면 됩니다.

**연결 키**: `/api/handoff` 요청의 `conversationId` = `eligibility_check.conversation_id` = `chat_message.conversation_id`

**`eligibility_check`** (한 대화에 여러 줄 가능. 카드 확인 후 상품 가입도 확인하면 2줄, 최신은 `id`가 큰 줄)

| 컬럼 | 예 | 뜻 |
|---|---|---|
| `customer_id` | `C` | `customer_profile`과 조인 → 이름·국적·체류자격·만료일·외국인등록증·계좌·소득 확인 |
| `category` | `CARD` | 고객이 고른 메뉴 |
| `confirmed` | `false` | 고객 정보 확인 질문에 [네]/[아니요] |
| `handoff_reason` | `PROFILE_MISMATCH` | 연결 제안을 띄운 원래 사유. `PROFILE_MISMATCH`(정보 불일치) / `NOT_ELIGIBLE`(불가 상품 있음) / `NULL`(제안 없음) |
| `result_ko` | `[카드 발급] 고객 정보 확인: 맞음 ...`<br>`신용카드 발급: 불가 - 체류자격(E-9)... → 대안 체크카드 발급` | 상담원이 그대로 읽을 수 있는 한국어 줄 (줄바꿈 구분) |
| `result_json` | `[{"productCode":"CREDIT_CARD","intent":"CREDIT_CARD","eligible":false,"reasonKeys":["RULE_VISA"],"availableTerms":[],"validUntil":null,"altProduct":"CHECK_CARD"}, ...]` | 상품별 결과 (프로그램용). `MISMATCH`면 `NULL` |
| `created_at` | | 판단 시각 |

**주의**
- `/api/handoff`에는 `reason: "CUSTOMER_REQUEST"`가 들어옵니다. 원래 사유는 `eligibility_check.handoff_reason`에서 읽어야 합니다.
- `customer_profile`·`eligibility_check`의 내용(체류자격·만료일)은 **LLM 요약 입력에 넣지 말고** 티켓에 따로 붙여야 합니다.
  채팅 기록에는 `[고객 정보 확인 질문]`, `[가입 자격 안내]`, `[네] 고객 정보가 맞다고 답함`처럼 자리표시만 남아 있습니다.
- 조회용 Repository 메서드: `EligibilityCheckRepository.findByConversationIdOrderByIdAsc(conversationId)`, `CustomerProfileRepository.findById(customerId)`

---

# 3부. DB 테이블

서버를 켤 때마다 `schema.sql` → `data.sql`로 새로 만듭니다 (데모용).

| 테이블 | 내용 |
|---|---|
| `guide_task` / `guide_task_text` | 업무와 언어별 이름·확인 질문 |
| `guide_step` / `guide_step_text` | 업무별 화면·버튼과 언어별 안내 문구 |
| `ui_text` | 업무와 무관한 시스템 문구 (언어별) |
| `intent_keyword` | LLM 실패 시 대체 키워드 |
| `guide_event` | 화면 이동 기록 |
| `chat_message` | 채팅 원문 |
| `handoff_ticket` | 상담원 티켓 (변경 없음) |
| 🆕 `customer_profile` | 데모용 가상 고객: 이름, 국적, 체류자격, 체류 만료일, 외국인등록증·계좌·소득 확인 여부, 선호 언어 |
| 🆕 `product` | 상품: 코드, 분류(CARD/JOIN), 안내 업무 코드, 정렬 순서 |
| 🆕 `product_rule` | 데모용 가상 규칙: 규칙 종류, 값, 불가 이유 문구 키, 대안 상품 |
| 🆕 `eligibility_check` | 자격 판단 기록 + 원래 연결 사유 (상담원 연결 쪽에 넘길 재료, 2부 8) |

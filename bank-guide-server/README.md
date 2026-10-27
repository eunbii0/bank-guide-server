# bank-guide-server (4단계: 상담원 연결 + 한국어 요약)

외국인 고객을 위한 은행 앱 AI 길안내 서버.

- Java 17 이상, Spring Boot 3.5, Spring AI 1.1 (OpenAI), Gradle
- 기본 DB: H2 메모리 DB (설치 없이 실행). `mysql` 프로필로 MySQL 사용 가능
- 지원 언어: 한국어(ko), 영어(en), 베트남어(vi), 중국어(zh). 그 밖의 언어는 영어로 안내

## IntelliJ에서 실행

1. File → Open → `bank-guide-server` 폴더 선택 (처음엔 의존성 다운로드에 몇 분 걸림)
2. `BankGuideServerApplication.java`의 `main` 옆 ▶ 한 번 실행 (Run Configuration이 만들어짐)
3. 상단 실행 구성 드롭다운 → **Edit Configurations...**
   → Environment variables에 `OPENAI_API_KEY=sk-...` 입력 → OK
4. 다시 ▶ 실행. 콘솔에 `Tomcat started on port 8080`이 나오면 성공

API 키 없이 실행해도 서버는 뜬다. 이때는
- 업무 분류 → 키워드 분류로 대신 (화면 로그 `chat (KEYWORD)`)
- 상담원용 요약 → 규칙 기반 요약으로 대신 (티켓에 "요약: 규칙 기반")

Claude API를 쓰려면 `build.gradle`의 `spring-ai-starter-model-openai`를 `spring-ai-starter-model-anthropic`으로,
`application.yml`의 `spring.ai.openai` 블록을 `spring.ai.anthropic`(api-key, chat.options.model)으로 바꾼다.

## 4단계에서 확인할 것

| 해볼 것 | 기대 결과 |
| --- | --- |
| 안내 중 같은 화면에서 30초 가만히 있기 (화면 주소에 `?stuck=5`를 붙이면 5초) | "어려운 부분이 있으신가요?" + [상담원 연결] [혼자 계속] |
| 안내 중 '대출' 화면에 3번 잘못 들어가기 | 3번째에 같은 연결 제안 |
| 안내 중 [질문하기] → "Sao tiền lãi ít vậy? Tôi có bị mất tiền không?" | 손익 판단 질문 → 연결 제안 |
| "Someone will pay me if I lend my account" | 사기 우려 경고 후 연결을 물어보고, [상담원 연결]을 누르면 금융사기 대기열에 연결 |
| 연결 후 화면 오른쪽 "상담 티켓" | 한국어 요약 3줄, 확인할 것, 원문 대화 |

콘솔 로그: `[LLM 분류]`, `[상담원 연결] T-... queue=FOREIGN reason=ADVICE lang=vi summary=LLM`

## API

| 메서드 | 경로 | 요청 | 응답 |
| --- | --- | --- | --- |
| POST | /api/chat | `{"conversationId","text"}` | `{"intent","confirmText","lang","message","options","source","escalation"}` |
| POST | /api/chat/choose | `{"conversationId","intent","lang"}` | 위와 같음 |
| POST | /api/guide/start | `{"intent","page","lang"}` | `{"sessionId","guide"}` |
| POST | /api/guide/event | `{"sessionId","page"}` | `{"guide":{...},"offer":null 또는 연결 제안 문구}` |
| POST | /api/guide/stuck | `{"sessionId","page","idleSec"}` | `{"offer"}` |
| POST | /api/guide/stop | `{"sessionId"}` | 204 |
| GET | /api/guide/{sessionId}/events | - | 행동 기록 목록 |
| POST | /api/handoff | `{"conversationId","sessionId","reason","page","lang","intent"}` | `{"ticketNo","queue","message"}` |
| GET | /api/tickets | - | 티켓 목록 (최신순) |
| GET | /api/tickets/{ticketNo} | - | 한국어 요약, 확인할 것, 원문 대화 |

- `escalation`: `NONE` / `ADVICE`(손익·상품 판단 질문) / `FRAUD`(금융사기 의심)
- `reason`: `ADVICE` / `STUCK` / `OFF_PATH` / `FRAUD` / `CUSTOMER_REQUEST`
- `queue`: `FOREIGN`(외국인 전담) / `FRAUD`(금융사기 대응) / `GENERAL`(한국어 고객)

## 코드 구조

| 패키지 | 내용 |
| --- | --- |
| `handoff` | **4단계 핵심.** `HandoffService`(연결 전체 흐름), `HandoffSummarizer`(LLM 요약 프롬프트), `HandoffNote`(요약 입력 만들기·규칙 요약·검사, 순수 로직) |
| `intent` | 업무·언어·이관 판단. `LlmIntentClassifier`의 프롬프트에 FRAUD/ADVICE 규칙이 있음 |
| `engine` | 안내 규칙 (순수 로직) |
| `common` | `Masking`(LLM에 보내기 전 긴 숫자 가리기), `PageNames`(화면 ID → 한국어 이름) |
| `domain` | JPA 엔티티와 Repository — 테이블 10개 (`chat_message`, `handoff_ticket` 추가) |
| `service` | `GuideService`(세션·안내·멈춤·이탈), `TextService`(언어별 문구), `ConversationService`(채팅 원문 저장) |
| `api` | Controller와 요청·응답 record |

## 테스트

`src/test` 아래 테스트 11개 (안내 엔진 4, 가리기 3, 요약 4). 테스트 폴더 우클릭 → Run 'Tests in ...'.

## 언어 추가하기 (예: 네팔어 ne)

1. `data.sql`의 `guide_task_text`, `guide_step_text`, `ui_text`에 `'ne'` 행 추가
2. `TextService.SUPPORTED`에 `"ne"` 추가
3. 화면 `src/i18n.js`에 `ne` 문구 추가

## MySQL로 실행하기

1. `application.yml`의 mysql 프로필 비밀번호를 본인 것으로 수정 (또는 환경변수 `MYSQL_PASSWORD`)
2. Edit Configurations → Active profiles에 `mysql` 입력 → 실행

주의: 데모용이라 서버를 켤 때마다 테이블을 지우고 다시 만든다.

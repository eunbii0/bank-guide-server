# smart-guide-bank

외국인 특화 스마트 화면 가이드 챗봇

외국인 고객이 은행 앱에서 막히면 AI가 먼저 알아채고, 화면을 직접 짚어주고(Spotlight),
그래도 안 풀리면 지금까지의 이벤트 로그와 함께 상담원에게 연결합니다.

## 전체 흐름

1. **고객 화면 (React)**: 은행 앱 화면, 클릭·뒤로가기·입력 수정·오류 같은 이벤트 발생
2. **서버 (Spring Boot + MySQL)**: 이벤트 로그 저장
3. **AI 처리 (Spring AI)**: 키워드·의도 분류, 막힘 감지, 다국어 안내, 로그 요약
4. **상담원 연결**: 해결이 안 되면 상담 티켓 생성
5. **상담원 화면 (React)**: 고객의 이벤트 로그와 AI 요약을 보고 상담 시작

## 폴더 구조

```
frontend/   고객 화면 + 상담원 화면 (React)
backend/    Spring Boot + Spring AI + MySQL
docs/       API 약속 (API.md), 회의 기록
```

## 역할 분담

| 담당 | 이름 | 할 일 | 주로 작업하는 폴더 |
|---|---|---|---|
| A 고객 화면 | | React 은행 화면, 스포트라이트, 이벤트 전송, 영어 버전 | `frontend/` |
| B 서버·DB | | Spring Boot API, MySQL, 이벤트 로그 저장, 상담 티켓 | `backend/` |
| C AI 처리 | | Spring AI: 의도 분류, 막힘 감지 규칙, 로그 요약, 번역 | `backend/` (ai 패키지) |
| D 상담원 화면 | | 상담 대기 목록, 이벤트 로그 타임라인, AI 요약 표시 | `frontend/` (agent 화면) |
| E 기획·데이터·발표 | | 외국인 문제 리서치, 시나리오, 서류·전세/청약 설명 콘텐츠, 발표자료, 통합 테스트 | `docs/` |

## 작업 규칙

1. **main에 직접 push하지 않기.** 각자 브랜치에서 작업하고 Pull Request로 합칩니다.
   - 브랜치 이름 예: `feature/frontend-spotlight`, `feature/ai-intent`, `feature/agent-dashboard`
2. **작업 시작 전에 항상 최신 main 받기**
   ```bash
   git checkout main
   git pull
   git checkout -b feature/내작업
   ```
3. **자기 담당 폴더만 수정하기.** AI에게 시킬 때도 "frontend 폴더만 수정해"처럼 범위를 알려주세요.
4. **API를 바꾸고 싶으면 `docs/API.md`를 먼저 고치고 팀에 알리기.**
   AI에게 작업을 시킬 때는 "docs/API.md 약속대로 만들어"라고 먼저 말해주세요.
5. **API 키, DB 비밀번호는 절대 올리지 않기.** `application-local.yml`이나 `.env`에 넣으면 `.gitignore`가 막아줍니다.
6. **자주 합치기.** 점심 전, 저녁 전 하루 두 번 이상 PR을 올려주세요.

## 처음 받는 법

```bash
git clone https://github.com/eunbii0/smart-guide-bank.git
cd smart-guide-bank
```

-- 데모용: 서버를 켤 때마다 테이블을 새로 만든다 (MySQL 프로필에서도 동일)
DROP TABLE IF EXISTS handoff_ticket;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS guide_event;
DROP TABLE IF EXISTS ui_text;
DROP TABLE IF EXISTS intent_keyword;
DROP TABLE IF EXISTS guide_step_text;
DROP TABLE IF EXISTS guide_step;
DROP TABLE IF EXISTS guide_task_text;
DROP TABLE IF EXISTS guide_task;

-- 업무 (메뉴 지도의 머리). 언어와 무관한 정보만 둔다.
CREATE TABLE guide_task (
    code        VARCHAR(40)  PRIMARY KEY,   -- DEPOSIT_CANCEL
    goal_page   VARCHAR(60)  NOT NULL,      -- 이 화면에 도착하면 안내 종료
    priority    INT          NOT NULL,      -- 키워드 대체 분류 시 검사 순서
    description VARCHAR(255) NOT NULL       -- LLM에게 보여줄 업무 설명
);

-- 업무 이름과 확인 질문 (언어별)
CREATE TABLE guide_task_text (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_code    VARCHAR(40)  NOT NULL,
    lang         VARCHAR(5)   NOT NULL,     -- ko, en, vi, zh
    name         VARCHAR(100) NOT NULL,
    confirm_text VARCHAR(255) NOT NULL,
    FOREIGN KEY (task_code) REFERENCES guide_task (code)
);

-- 업무별 단계 (어느 화면에서 어떤 버튼을 누르는지)
CREATE TABLE guide_step (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_code VARCHAR(40) NOT NULL,
    step_no   INT         NOT NULL,
    page      VARCHAR(60) NOT NULL,
    target    VARCHAR(60) NOT NULL,         -- 하이라이트할 버튼 id
    FOREIGN KEY (task_code) REFERENCES guide_task (code)
);

-- 단계 안내 문구 (언어별). 화면은 한국어라서 버튼 이름은 한국어 그대로 넣고 뜻을 덧붙인다.
CREATE TABLE guide_step_text (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_code VARCHAR(40)  NOT NULL,
    step_no   INT          NOT NULL,
    lang      VARCHAR(5)   NOT NULL,
    message   VARCHAR(255) NOT NULL
);

-- 업무와 무관한 시스템 문구 (경로 이탈, 완료, 모르는 질문, 되묻기)
CREATE TABLE ui_text (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    text_key VARCHAR(40)  NOT NULL,
    lang     VARCHAR(5)   NOT NULL,
    content  VARCHAR(255) NOT NULL
);

-- LLM 호출이 실패했을 때 쓰는 대체 키워드
CREATE TABLE intent_keyword (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_code VARCHAR(40) NOT NULL,
    keyword   VARCHAR(40) NOT NULL,
    FOREIGN KEY (task_code) REFERENCES guide_task (code)
);

-- 고객 행동 기록 (4단계 요약의 재료)
CREATE TABLE guide_event (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(36)  NOT NULL,
    event_type VARCHAR(30)  NOT NULL,       -- GUIDE_START, PAGE_ENTER, GUIDE_STOP
    page       VARCHAR(60),
    detail     VARCHAR(500),
    created_at TIMESTAMP    NOT NULL
);

-- 4단계: 채팅 기록 (고객 원문 그대로 보관). 화면을 연 순간부터 하나의 conversation_id로 묶는다.
CREATE TABLE chat_message (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(36)   NOT NULL,
    sender          VARCHAR(10)   NOT NULL,   -- CUSTOMER, AI, AGENT
    lang            VARCHAR(5),
    content         VARCHAR(1000) NOT NULL,
    translated      VARCHAR(1000),            -- 상담원 답장을 고객 언어로 번역한 문장
    created_at      TIMESTAMP     NOT NULL
);

-- 4단계: 상담원에게 넘기는 티켓
CREATE TABLE handoff_ticket (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    ticket_no       VARCHAR(30)   NOT NULL,   -- T-20261026-0001
    conversation_id VARCHAR(36)   NOT NULL,
    session_id      VARCHAR(36),
    queue           VARCHAR(20)   NOT NULL,   -- FOREIGN, FRAUD, GENERAL
    reason          VARCHAR(20)   NOT NULL,   -- ADVICE, STUCK, OFF_PATH, FRAUD, CUSTOMER_REQUEST
    lang            VARCHAR(5)    NOT NULL,
    intent          VARCHAR(40),
    current_page    VARCHAR(60),
    summary_ko      VARCHAR(2000) NOT NULL,   -- 줄바꿈으로 구분한 한국어 요약
    checkpoints     VARCHAR(1000),            -- 줄바꿈으로 구분한 확인 권장 사항
    risk_flag       VARCHAR(20)   NOT NULL,   -- NONE, FRAUD_SUSPECTED
    summary_source  VARCHAR(10)   NOT NULL,   -- LLM, RULE
    status          VARCHAR(20)   NOT NULL,   -- WAITING, IN_PROGRESS, CLOSED
    created_at      TIMESTAMP     NOT NULL
);

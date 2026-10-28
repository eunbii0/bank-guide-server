-- reset 프로필 전용: 모든 테이블을 지운다. 이어서 schema.sql이 새로 만들고 data.sql이 기준 데이터를 넣는다.
-- 리허설 기록을 지우고 깨끗하게 시연을 시작할 때, 테이블 구조(schema.sql)를 바꾼 뒤에 쓴다.
DROP TABLE IF EXISTS customer_info_access;
DROP TABLE IF EXISTS eligibility_check;
DROP TABLE IF EXISTS product_rule;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS customer_profile;
DROP TABLE IF EXISTS handoff_ticket;
DROP TABLE IF EXISTS chat_message;
DROP TABLE IF EXISTS guide_event;
DROP TABLE IF EXISTS ui_text;
DROP TABLE IF EXISTS intent_keyword;
DROP TABLE IF EXISTS guide_step_text;
DROP TABLE IF EXISTS guide_step;
DROP TABLE IF EXISTS guide_task_text;
DROP TABLE IF EXISTS guide_task;

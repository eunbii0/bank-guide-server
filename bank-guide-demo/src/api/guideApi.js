// Spring Boot 서버(bank-guide-server)와 통신하는 함수 모음.
// '/api/...'로 보내면 vite.config.js의 proxy 설정이 localhost:8080으로 넘겨준다.

export class ServerDownError extends Error {}

async function request(path, options) {
  let res;
  try {
    res = await fetch(path, options);
  } catch {
    throw new ServerDownError();
  }
  // 서버가 꺼져 있으면 Vite 프록시가 502~504로 대신 응답한다
  if ([502, 503, 504].includes(res.status)) throw new ServerDownError();
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message || `Server error (${res.status})`);
  }
  return res.status === 204 ? null : res.json();
}

const post = (path, body) =>
  request(path, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
const get = (path) => request(path);

// ---------- 채팅 ----------

/** 고객 문장(어느 언어든) → { intent, confirmText, lang, message, options, source, escalation } */
export const chat = (conversationId, text, lang) => post('/api/chat', { conversationId, text, lang });

/** 되묻기 선택지를 골랐을 때 → 위와 같은 모양 */
export const choose = (conversationId, intent, lang) => post('/api/chat/choose', { conversationId, intent, lang });

// ---------- 안내 ----------

/** 안내 시작 → { sessionId, guide } */
export const startGuide = (intent, page, lang) => post('/api/guide/start', { intent, page, lang });

/** 화면 진입 → { guide, offer }  (offer: 경로 이탈 3회째일 때 상담원 연결 제안 문구) */
export const sendPageEnter = (sessionId, page) => post('/api/guide/event', { sessionId, page });

/** 같은 화면에서 오래 멈춤 → { offer } */
export const reportStuck = (sessionId, page, idleSec) => post('/api/guide/stuck', { sessionId, page, idleSec });

/** 안내 그만하기 */
export const stopGuide = (sessionId) => post('/api/guide/stop', { sessionId });

/** DB에 저장된 행동 기록 */
export const fetchEvents = (sessionId) => get(`/api/guide/${sessionId}/events`).catch(() => []);

// ---------- 상담원 연결 (4단계) ----------

/** 상담원 연결 → { ticketNo, queue, message } */
export const handoff = (body) => post('/api/handoff', body);

/** 티켓 하나 (한국어 요약 + 확인할 것 + 원문 대화) */
export const fetchTicket = (ticketNo) => get(`/api/tickets/${ticketNo}`);

// ---------- 상담원 화면 (5단계) ----------

/** 티켓 목록 (최신순) */
export const fetchTickets = () => get('/api/tickets');

/** 티켓 상태 변경: WAITING, IN_PROGRESS, CLOSED → 바뀐 티켓 */
export const changeTicketStatus = (ticketNo, status) =>
  request(`/api/tickets/${ticketNo}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  });

/** 상담원 답장(한국어). 서버가 고객 언어로 번역해 저장한다 → 바뀐 티켓 */
export const replyTicket = (ticketNo, text) => post(`/api/tickets/${ticketNo}/reply`, { text });

/** 상담 중 고객이 상담원에게 보내는 말(고객 언어 그대로). 서버가 한국어로 번역해 저장한다 */
export const sendCustomerMessage = (ticketNo, text) => post(`/api/tickets/${ticketNo}/customer-message`, { text });

/**
 * 대화 맥락에서 AI가 만든 추천 문장. role은 'agent'(상담원이 보낼 한국어 답변) 또는 'customer'(고객 언어).
 * → { suggestions: [...], source: 'LLM' | 'NONE' }. 실패하면 빈 목록을 돌려줘서 화면이 고정 예시를 쓴다.
 */
export const fetchSuggestions = (ticketNo, role) =>
  get(`/api/tickets/${ticketNo}/suggestions?role=${role}`).catch(() => ({ suggestions: [], source: 'NONE' }));

/** 고객이 상담 종료 버튼을 눌렀을 때. 서버가 티켓을 CLOSED로 바꾼다 → 바뀐 티켓 */
export const closeTicketByCustomer = (ticketNo) => post(`/api/tickets/${ticketNo}/customer-close`, {});

/** 고객 화면용: 티켓 상태 + afterId 이후의 상담원 답장(번역본) → { status, messages: [{ id, time, text }] } */
export const fetchAgentMessages = (ticketNo, afterId) => get(`/api/tickets/${ticketNo}/agent-messages?after=${afterId}`);

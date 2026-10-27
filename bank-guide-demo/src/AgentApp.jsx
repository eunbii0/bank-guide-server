import { useEffect, useRef, useState } from 'react';
import './AgentApp.css';
import * as api from './api/guideApi';
import CustomerInfoBox from './components/CustomerInfoBox';

const QUEUES = [
  { code: 'ALL', name: '전체' },
  { code: 'FOREIGN', name: '외국인 전담' },
  { code: 'FRAUD', name: '금융사기 대응' },
  { code: 'GENERAL', name: '일반' },
];
const QUEUE_NAMES = { FOREIGN: '외국인 전담', FRAUD: '금융사기 대응', GENERAL: '일반' };
const STATUS_NAMES = { WAITING: '대기', IN_PROGRESS: '상담 중', CLOSED: '종료' };
const SENDER_NAMES = { CUSTOMER: '고객', AI: 'AI', AGENT: '상담원', SYSTEM: '시스템' };
const POLL_MS = 3000;

// 자주 쓰는 답변. 누르면 입력창에 채워지고, 고친 뒤 보내면 된다.
const TEMPLATES = [
  '안녕하세요, 상담원입니다. 무엇을 도와드릴까요?',
  '여권에 적힌 영문 이름을 그대로 입력해 보세요.',
  '화면에 밝게 표시된 버튼을 눌러 주세요.',
  '중도해지를 하시면 약정 이자보다 낮은 이자가 적용될 수 있어요. 그래도 진행하시겠어요?',
  '혹시 다른 사람이 계좌를 빌려 달라고 하거나 송금을 요구했나요?',
  '더 궁금한 점이 있으면 말씀해 주세요. 상담을 마치겠습니다.',
];

// 상담원 화면 (주소: /agent). 서버의 티켓 API를 3초마다 읽어 온다.
export default function AgentApp() {
  const [tickets, setTickets] = useState([]);
  const [queue, setQueue] = useState('ALL');
  const [selected, setSelected] = useState(null); // ticketNo
  const [detail, setDetail] = useState(null);
  const [reply, setReply] = useState('');
  const [sending, setSending] = useState(false);
  const [showTemplates, setShowTemplates] = useState(false); // 자주 쓰는 답변 펼침 (기본은 접힘)
  const [error, setError] = useState(null);
  const threadRef = useRef(null);

  const message = (e) => (e instanceof api.ServerDownError ? '서버(8080)에 연결할 수 없어요.' : e.message);

  // 목록 + 선택한 티켓을 주기적으로 새로고침
  useEffect(() => {
    let alive = true;
    const load = async () => {
      try {
        const list = await api.fetchTickets();
        if (!alive) return;
        setTickets(list);
        if (selected) setDetail(await api.fetchTicket(selected));
        if (alive) setError(null);
      } catch (e) {
        if (alive) setError(message(e));
      }
    };
    load();
    const timer = setInterval(load, POLL_MS);
    return () => {
      alive = false;
      clearInterval(timer);
    };
  }, [selected]);

  useEffect(() => {
    const el = threadRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [detail?.messages.length]);

  // 상담원 차례(마지막 말이 고객·AI)가 되면 대화 맥락으로 AI가 답변 문장을 추천한다
  const [aiSuggest, setAiSuggest] = useState({ items: [], loading: false });
  const talk = detail ? detail.messages.filter((m) => m.sender !== 'SYSTEM') : [];
  const curNo = detail?.ticketNo;
  const curStatus = detail?.status;
  const lastSender = talk.at(-1)?.sender;
  const talkCount = talk.length;
  useEffect(() => {
    if (!curNo || curStatus === 'CLOSED' || !lastSender || lastSender === 'AGENT') {
      setAiSuggest({ items: [], loading: false });
      return undefined;
    }
    let alive = true;
    setAiSuggest({ items: [], loading: true });
    api.fetchSuggestions(curNo, 'agent').then((r) => alive && setAiSuggest({ items: r.suggestions, loading: false }));
    return () => {
      alive = false;
    };
  }, [curNo, curStatus, lastSender, talkCount]);

  const select = (ticketNo) => {
    setSelected(ticketNo);
    setDetail(tickets.find((t) => t.ticketNo === ticketNo) ?? null);
  };

  const setStatus = async (status) => {
    try {
      setDetail(await api.changeTicketStatus(detail.ticketNo, status));
      setTickets(await api.fetchTickets());
    } catch (e) {
      setError(message(e));
    }
  };

  const send = async (e) => {
    e.preventDefault();
    const text = reply.trim();
    if (!text || sending) return;
    setSending(true);
    try {
      setDetail(await api.replyTicket(detail.ticketNo, text));
      setReply('');
      setTickets(await api.fetchTickets());
    } catch (err) {
      setError(message(err));
    } finally {
      setSending(false);
    }
  };

  const shown = tickets.filter((t) => queue === 'ALL' || t.queue === queue);
  const waiting = tickets.filter((t) => t.status === 'WAITING').length;

  return (
    <main className="agent-app">
      <aside className="agent-list">
        <header>
          <h1>상담원 화면</h1>
          <p>대기 {waiting}건 · 전체 {tickets.length}건</p>
        </header>
        <div className="agent-tabs">
          {QUEUES.map((q) => (
            <button key={q.code} className={queue === q.code ? 'on' : ''} onClick={() => setQueue(q.code)}>
              {q.name}
            </button>
          ))}
        </div>
        <ul>
          {shown.map((t) => (
            <li key={t.ticketNo}>
              <button className={selected === t.ticketNo ? 'row on' : 'row'} onClick={() => select(t.ticketNo)}>
                <span className="row-top">
                  <strong>{t.ticketNo}</strong>
                  <span className={`badge ${t.queue.toLowerCase()}`}>{QUEUE_NAMES[t.queue] ?? t.queue}</span>
                </span>
                <span className="row-sub">
                  {t.intentName} · {t.lang} · {t.createdAt}
                </span>
                <span className="row-sub">
                  <span className={`status ${t.status.toLowerCase()}`}>{STATUS_NAMES[t.status] ?? t.status}</span>
                  {t.riskFlag !== 'NONE' && <span className="risk"> 금융사기 의심</span>}
                </span>
              </button>
            </li>
          ))}
          {shown.length === 0 && <li className="empty">티켓이 없습니다.</li>}
        </ul>
      </aside>

      <section className="agent-detail">
        {error && <p className="agent-error">{error}</p>}
        {!detail && <p className="empty">왼쪽에서 티켓을 선택하세요.</p>}
        {detail && (
          <>
            <header className="detail-head">
              <div>
                <h2>{detail.ticketNo}</h2>
                <p>
                  <span className={`badge ${detail.queue.toLowerCase()}`}>{QUEUE_NAMES[detail.queue] ?? detail.queue}</span>{' '}
                  <span className={`status ${detail.status.toLowerCase()}`}>{STATUS_NAMES[detail.status] ?? detail.status}</span>
                </p>
              </div>
              <div className="detail-actions">
                {detail.status === 'WAITING' && <button onClick={() => setStatus('IN_PROGRESS')}>상담 시작</button>}
                {detail.status !== 'CLOSED' && (
                  <button className="ghost" onClick={() => setStatus('CLOSED')}>
                    상담 종료
                  </button>
                )}
              </div>
            </header>

            {detail.riskFlag !== 'NONE' && <p className="risk-banner">금융사기가 의심됩니다. 제3자가 송금·계좌 대여를 요구했는지 먼저 확인하세요.</p>}

            <dl className="meta">
              <dt>고객 언어</dt>
              <dd>{detail.lang}</dd>
              <dt>업무</dt>
              <dd>{detail.intentName}</dd>
              <dt>연결 사유</dt>
              <dd>{detail.reason}</dd>
              <dt>현재 화면</dt>
              <dd>{detail.currentPage}</dd>
            </dl>

            <div className="cards">
              <div>
                <h3>요약 ({detail.summarySource === 'LLM' ? 'AI' : '규칙 기반'})</h3>
                <ol>
                  {detail.summary.map((s, i) => (
                    <li key={i}>{s}</li>
                  ))}
                </ol>
              </div>
              <div>
                <h3>확인할 것</h3>
                <ul>
                  {detail.checkpoints.map((c, i) => (
                    <li key={i}>{c}</li>
                  ))}
                </ul>
              </div>
            </div>

            <CustomerInfoBox
              ticket={detail}
              onChanged={async (t) => {
                setDetail(t);
                setTickets(await api.fetchTickets());
              }}
              onError={(e) => setError(message(e))}
            />

            <h3>대화</h3>
            <div className="thread" ref={threadRef}>
              {detail.messages.map((m, i) => (
                <p key={i} className={`msg ${m.sender.toLowerCase()}`}>
                  <span className="msg-head">
                    {m.time} {SENDER_NAMES[m.sender] ?? m.sender}
                    {(m.sender === 'CUSTOMER' || m.sender === 'AI') && ` (${m.lang})`}
                  </span>
                  {m.content}
                  {m.translated && <span className="msg-tr">한국어: {m.translated}</span>}
                </p>
              ))}
            </div>

            {detail.status !== 'CLOSED' && (aiSuggest.loading || aiSuggest.items.length > 0) && (
              <div className="templates ai">
                <span>AI 추천 답변</span>
                {aiSuggest.loading ? (
                  <em>대화 맥락을 읽는 중…</em>
                ) : (
                  aiSuggest.items.map((s) => (
                    <button key={s} type="button" onClick={() => setReply(s)}>
                      {s}
                    </button>
                  ))
                )}
              </div>
            )}
            {detail.status !== 'CLOSED' && (
              <div className="templates">
                <button
                  type="button"
                  className="templates-toggle"
                  aria-expanded={showTemplates}
                  onClick={() => setShowTemplates((v) => !v)}
                >
                  자주 쓰는 답변 {showTemplates ? '▾' : '▸'}
                </button>
                {showTemplates &&
                  TEMPLATES.map((s) => (
                    <button key={s} type="button" onClick={() => setReply(s)}>
                      {s}
                    </button>
                  ))}
              </div>
            )}
            <form className="reply" onSubmit={send}>
              <input
                value={reply}
                onChange={(e) => setReply(e.target.value)}
                placeholder={detail.status === 'CLOSED' ? '종료된 상담입니다' : `한국어로 입력하면 고객 언어(${detail.lang})로 번역됩니다`}
                disabled={detail.status === 'CLOSED'}
              />
              <button type="submit" disabled={sending || detail.status === 'CLOSED'}>
                {sending ? '번역 중…' : '보내기'}
              </button>
            </form>
          </>
        )}
      </section>
    </main>
  );
}

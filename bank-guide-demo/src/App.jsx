import { useEffect, useRef, useState } from 'react';
import './App.css';
import {
  HomePage,
  ProductPage,
  LoanPage,
  DepositListPage,
  DepositCancelPage,
  TransferPage,
  CertificatePage,
  CertIssuePage,
  DonePage,
} from './pages/Pages';
import ChatPanel from './components/ChatPanel';
import GuideOverlay from './components/GuideOverlay';
import TicketPreview from './components/TicketPreview';
import * as api from './api/guideApi';
import { t, GREETING, FAB_LABEL } from './i18n';

const now = () => new Date().toTimeString().slice(0, 8);
const newId = () => (crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`);

// 같은 화면에서 이 시간(초) 동안 아무것도 안 누르면 '멈춤'으로 본다.
// 시연할 때는 주소 뒤에 ?stuck=5 를 붙이면 5초로 줄어든다.
const STUCK_SECONDS = Number(new URLSearchParams(window.location.search).get('stuck')) || 30;

export default function App() {
  // 화면 이동: history 배열의 마지막이 현재 화면
  const [history, setHistory] = useState(['home']);
  const page = history.at(-1);

  // 이 화면을 연 고객의 대화 전체를 묶는 id (4단계: 서버가 채팅 원문을 이 id로 저장)
  const [conversationId] = useState(newId);

  // 고객 언어. 서버(LLM)가 첫 질문을 보고 알려준다.
  const [lang, setLang] = useState('en');

  // 안내 상태. 몇 번째 단계인지는 서버가 기억하고, 화면은 sessionId만 들고 있다.
  const [sessionId, setSessionId] = useState(null);
  const [guide, setGuide] = useState(null);
  const [currentIntent, setCurrentIntent] = useState(null);

  // 채팅
  const [chatOpen, setChatOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [messages, setMessages] = useState([{ from: 'ai', text: GREETING }]);
  const [pendingIntent, setPendingIntent] = useState(null); // 네/아니요를 기다리는 업무
  const [options, setOptions] = useState([]); // 애매할 때 보여줄 선택지
  const [offer, setOffer] = useState(null); // 상담원 연결 제안 { reason }
  const [ticketNo, setTicketNo] = useState(null); // 상담원에게 넘어간 티켓
  const say = (...msgs) => setMessages((m) => [...m, ...msgs]);

  // 서버와 주고받은 내용 (오른쪽 패널)
  const [traffic, setTraffic] = useState([]);
  const [dbEvents, setDbEvents] = useState(null);
  const [lastSessionId, setLastSessionId] = useState(null);
  const log = (dir, label, data) => setTraffic((prev) => [...prev, { t: now(), dir, label, data }]);

  const fail = (error, l = lang) => {
    const text = error instanceof api.ServerDownError ? t(l, 'serverDown') : error.message;
    log('err', 'ERROR', text);
    say({ from: 'ai', text });
    setChatOpen(true);
  };

  // 상담원 연결 제안을 띄운다 (판단 질문·멈춤·이탈 반복)
  const showOffer = (text, reason) => {
    setPendingIntent(null);
    setOptions([]);
    setOffer({ reason });
    say({ from: 'ai', text });
    setChatOpen(true);
  };

  // ---------- 4단계: 멈춤 감지 ----------
  // 안내 중인 화면에서 STUCK_SECONDS 동안 다음 화면으로 안 넘어가면 서버에 알린다. 화면마다 한 번만.
  const stuckReported = useRef(new Set());
  useEffect(() => {
    if (!sessionId || guide?.type !== 'STEP' || offer || chatOpen) return undefined;
    const key = `${sessionId}:${page}`;
    if (stuckReported.current.has(key)) return undefined;

    const timer = setTimeout(async () => {
      stuckReported.current.add(key);
      try {
        log('out', 'POST /api/guide/stuck', { page, idleSec: STUCK_SECONDS });
        const res = await api.reportStuck(sessionId, page, STUCK_SECONDS);
        log('in', 'stuck', res);
        showOffer(res.offer, 'STUCK');
      } catch (e) {
        fail(e);
      }
    }, STUCK_SECONDS * 1000);
    return () => clearTimeout(timer);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sessionId, guide, page, offer, chatOpen]);

  // 서버의 분류 결과를 채팅창에 반영한다
  const showChatResult = (res) => {
    setLang(res.lang);
    if (res.escalation === 'FRAUD') {
      // 사기 의심: 경고를 보여주고 묻지 않고 바로 연결한다
      say({ from: 'ai', text: res.message, warn: true });
      doHandoff('FRAUD', res.lang);
    } else if (res.escalation === 'ADVICE') {
      showOffer(res.message, 'ADVICE');
    } else if (res.intent === 'AMBIGUOUS') {
      setOptions(res.options);
      say({ from: 'ai', text: res.message });
    } else if (res.intent === 'UNKNOWN') {
      say({ from: 'ai', text: res.message });
    } else {
      setPendingIntent(res.intent);
      say({ from: 'ai', text: res.confirmText });
    }
  };

  // 화면에 들어올 때마다 서버에 알린다 → 다음 안내를 받는다
  const enterPage = async (next, nextHistory) => {
    setHistory(nextHistory);
    if (!sessionId) return;
    setGuide((g) => g && { ...g, target: null }); // 응답이 올 때까지 이전 하이라이트를 지운다
    try {
      log('out', 'POST /api/guide/event', { page: next });
      const res = await api.sendPageEnter(sessionId, next);
      log('in', 'guide', res);
      setGuide(res.guide);
      if (res.guide.type === 'DONE') setSessionId(null);
      if (res.offer) showOffer(res.offer, 'OFF_PATH'); // 경로 이탈 3회째
    } catch (e) {
      setGuide(null);
      setSessionId(null);
      fail(e);
    }
  };

  const go = (next) => enterPage(next, [...history, next]);
  const back = () => {
    const prev = history.slice(0, -1);
    enterPage(prev.at(-1), prev);
  };
  const home = () => {
    setHistory(['home']);
    setGuide(null);
    setSessionId(null);
  };

  // 채팅 질문 → 서버(LLM)가 업무·언어·이관 필요 여부를 판단
  const onAsk = async (text) => {
    say({ from: 'user', text });
    setOptions([]);
    setPendingIntent(null);
    setOffer(null);
    setBusy(true);
    try {
      log('out', 'POST /api/chat', { text });
      const res = await api.chat(conversationId, text);
      log('in', `chat (${res.source})`, res);
      showChatResult(res);
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  };

  // 되묻기 선택지를 골랐을 때
  const onChoose = async (intent) => {
    const chosen = options.find((o) => o.intent === intent);
    setOptions([]);
    say({ from: 'user', text: chosen?.name ?? intent });
    try {
      log('out', 'POST /api/chat/choose', { intent, lang });
      const res = await api.choose(conversationId, intent, lang);
      log('in', 'choose', res);
      showChatResult(res);
    } catch (e) {
      fail(e);
    }
  };

  // 의도 확인 → 서버에 안내 시작 요청
  const onConfirm = async (yes) => {
    const intent = pendingIntent;
    setPendingIntent(null);
    if (!yes) {
      say({ from: 'user', text: t(lang, 'no') }, { from: 'ai', text: t(lang, 'retry') });
      return;
    }
    say({ from: 'user', text: t(lang, 'yes') });
    try {
      log('out', 'POST /api/guide/start', { intent, page, lang });
      const res = await api.startGuide(intent, page, lang);
      log('in', 'start', res);
      setSessionId(res.sessionId);
      setLastSessionId(res.sessionId);
      setCurrentIntent(intent);
      setGuide(res.guide);
      setDbEvents(null);
      say({ from: 'ai', text: t(lang, 'intro') });
      setChatOpen(false);
    } catch (e) {
      fail(e);
    }
  };

  // ---------- 4단계: 상담원 연결 ----------
  async function doHandoff(reason, l = lang) {
    setOffer(null);
    setBusy(true);
    try {
      const body = { conversationId, sessionId, reason, page, lang: l, intent: currentIntent };
      log('out', 'POST /api/handoff', body);
      const res = await api.handoff(body);
      log('in', 'handoff', res);
      setSessionId(null);
      setGuide(null);
      setTicketNo(res.ticketNo);
      say({ from: 'ai', text: res.message });
      setChatOpen(true);
    } catch (e) {
      fail(e, l);
    } finally {
      setBusy(false);
    }
  }

  const acceptOffer = () => {
    say({ from: 'user', text: t(lang, 'connect') });
    doHandoff(offer.reason);
  };
  const declineOffer = () => {
    say({ from: 'user', text: t(lang, 'continueSelf') });
    setOffer(null);
    if (sessionId) setChatOpen(false);
  };

  const stopGuide = async () => {
    const id = sessionId;
    setSessionId(null);
    setGuide(null);
    log('out', 'POST /api/guide/stop', { sessionId: id });
    await api.stopGuide(id).catch(() => {});
  };

  const loadDbEvents = async () => setDbEvents(await api.fetchEvents(lastSessionId));

  const pages = {
    home: <HomePage go={go} />,
    product: <ProductPage go={go} back={back} />,
    loan: <LoanPage back={back} />,
    'deposit-list': <DepositListPage go={go} back={back} />,
    'deposit-cancel': <DepositCancelPage go={go} back={back} />,
    'deposit-done': <DonePage text="예금 해지 신청이 접수되었습니다. (데모 화면)" home={home} />,
    transfer: <TransferPage go={go} back={back} />,
    'transfer-done': <DonePage text="이체가 완료되었습니다. (데모 화면)" home={home} />,
    certificate: <CertificatePage go={go} back={back} />,
    'cert-issue': <CertIssuePage go={go} back={back} />,
    'cert-done': <DonePage text="예금잔액증명서가 발급되었습니다. (데모 화면)" home={home} />,
  };

  return (
    <main className="layout">
      <div className="phone">
        <div className="screen">{pages[page]}</div>

        <GuideOverlay guide={guide} page={page} />
        {guide && guide.type !== 'DONE' && (
          <div className="guide-actions">
            <button className="guide-stop" onClick={() => setChatOpen(true)}>
              {t(lang, 'ask')}
            </button>
            <button className="guide-stop" onClick={stopGuide}>
              {t(lang, 'stop')}
            </button>
          </div>
        )}

        {!chatOpen && !guide && (
          <button className="ask-fab" onClick={() => setChatOpen(true)}>
            {ticketNo ? t(lang, 'waiting') : FAB_LABEL}
          </button>
        )}
        <ChatPanel
          open={chatOpen}
          onClose={() => setChatOpen(false)}
          lang={lang}
          busy={busy}
          messages={messages}
          pendingIntent={pendingIntent}
          options={options}
          offer={offer}
          waiting={Boolean(ticketNo)}
          onAsk={onAsk}
          onConfirm={onConfirm}
          onChoose={onChoose}
          onAcceptOffer={acceptOffer}
          onDeclineOffer={declineOffer}
        />
      </div>

      <aside className="dev-panel">
        <h2>서버 통신 로그</h2>
        <p className="dev-hint">
          화면 ↔ Spring Boot 서버(8080)가 주고받은 내용입니다. 멈춤 판단: {STUCK_SECONDS}초
        </p>
        <p className="dev-state">
          현재 화면 <code>{page}</code> · 고객 언어 <code>{lang}</code>
          {sessionId && (
            <>
              {' '}
              · 세션 <code>{sessionId.slice(0, 8)}</code>
            </>
          )}
        </p>
        <ol className="event-list">
          {traffic.map((e, i) => (
            <li key={i} className={`traffic ${e.dir}`}>
              <span className="traffic-head">
                {e.t} {e.dir === 'out' ? '→' : e.dir === 'in' ? '←' : '✕'} {e.label}
              </span>
              <code>{typeof e.data === 'string' ? e.data : JSON.stringify(e.data)}</code>
            </li>
          ))}
          {traffic.length === 0 && <li className="dev-hint">아직 통신 내역이 없습니다.</li>}
        </ol>

        {lastSessionId && (
          <div className="db-box">
            <button className="pill-btn ghost" onClick={loadDbEvents}>
              DB에 저장된 행동 기록 보기
            </button>
            {dbEvents && (
              <table className="db-table">
                <thead>
                  <tr>
                    <th>시각</th>
                    <th>유형</th>
                    <th>화면</th>
                    <th>내용</th>
                  </tr>
                </thead>
                <tbody>
                  {dbEvents.map((e, i) => (
                    <tr key={i}>
                      <td>{e.time}</td>
                      <td>{e.type}</td>
                      <td>{e.page ?? '-'}</td>
                      <td>{e.detail ?? '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        )}

        {ticketNo && <TicketPreview ticketNo={ticketNo} />}
      </aside>
    </main>
  );
}

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
  CardPage,
  CardApplyPage,
  JoinPage,
  JoinTermPage,
  JoinConfirmPage,
} from './pages/Pages';
import ChatPanel from './components/ChatPanel';
import GuideOverlay from './components/GuideOverlay';
import TicketPreview from './components/TicketPreview';
import * as api from './api/guideApi';
import { t, months, FAB_LABEL } from './i18n';

const now = () => new Date().toTimeString().slice(0, 8);
const newId = () => (crypto.randomUUID ? crypto.randomUUID() : `${Date.now()}-${Math.random().toString(16).slice(2)}`);

const params = new URLSearchParams(window.location.search);

// 같은 화면에서 이 시간(초) 동안 아무것도 안 누르면 '멈춤'으로 본다.
// 시연할 때는 주소 뒤에 ?stuck=5 를 붙이면 5초로 줄어든다.
const STUCK_SECONDS = Number(params.get('stuck')) || 30;

// 데모용 가상 고객 (A: Nguyen, B: Li Wei, C: Aziz). 주소에 ?customer=B 를 붙이거나 오른쪽 패널에서 바꾼다.
const INITIAL_CUSTOMER = (params.get('customer') || 'A').toUpperCase();

export default function App() {
  // 화면 이동: history 배열의 마지막이 현재 화면
  const [history, setHistory] = useState(['home']);
  const page = history.at(-1);

  // 시연 고객. 고객 정보(체류자격·만료일)는 서버에만 있고, 화면은 id만 들고 있다.
  const [customerId, setCustomerId] = useState(INITIAL_CUSTOMER);
  const [customers, setCustomers] = useState([]);

  // 이 화면을 연 고객의 대화 전체를 묶는 id (4단계: 서버가 채팅 원문을 이 id로 저장). 고객을 바꾸면 새로 만든다.
  const [conversationId, setConversationId] = useState(newId);

  // 고객 언어. 처음엔 서버가 메뉴를 고객 선호 언어로 보내고, 고객이 말하면 서버(LLM)가 알려준다.
  const [lang, setLang] = useState('ko');

  // 안내 상태. 몇 번째 단계인지는 서버가 기억하고, 화면은 sessionId만 들고 있다.
  const [sessionId, setSessionId] = useState(null);
  const [guide, setGuide] = useState(null);
  const [currentIntent, setCurrentIntent] = useState(null);

  // 채팅
  const [chatOpen, setChatOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  const [messages, setMessages] = useState([]); // 첫 메시지는 챗봇을 열 때 서버 메뉴로 채운다
  const [pendingIntent, setPendingIntent] = useState(null); // 네/아니요를 기다리는 업무
  const [options, setOptions] = useState([]); // 애매할 때 보여줄 선택지, 또는 첫 메뉴 버튼
  const [offer, setOffer] = useState(null); // 상담원 연결 제안 { reason, intent }
  const [ticketNo, setTicketNo] = useState(null); // 상담원에게 넘어간 티켓
  const [restartOffered, setRestartOffered] = useState(false); // "다른 업무도 도와드릴까요?" 뒤의 [처음으로] 버튼
  const say = (...msgs) => setMessages((m) => [...m, ...msgs]);

  // 안내가 끝났거나 혼자 계속하기로 했을 때: 다른 업무로 넘어갈 수 있게 [처음으로]를 한 번 더 보여준다
  const offerRestart = () => {
    say({ from: 'ai', text: t(lang, 'anotherTask') });
    setRestartOffered(true);
  };

  // 가입 자격 판단 (데모용 가상 규칙)
  const [verifyCategory, setVerifyCategory] = useState(null); // 고객 정보 확인 [네]/[아니요]를 기다리는 메뉴 (CARD, JOIN)
  const [eligibility, setEligibility] = useState(null); // 서버가 판단한 상품별 결과
  const [joinDraft, setJoinDraft] = useState({ kind: null, months: null }); // 앱 화면에서 고른 예금·적금과 기간
  const menuRequested = useRef(null); // 이 대화에서 메뉴를 이미 불렀는지 (conversationId)

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

  // 상담원 연결 제안을 띄운다 (판단 질문·멈춤·이탈 반복·고객 정보 불일치)
  // intent는 연결할 때 /api/handoff에 함께 보낼 업무 (기본은 안내 중인 업무)
  const showOffer = (text, reason, intent = currentIntent) => {
    setPendingIntent(null);
    setOptions([]);
    setVerifyCategory(null);
    setEligibility(null);
    setRestartOffered(false);
    setOffer({ reason, intent });
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

  // ---------- 챗봇 메뉴 + 가입 자격 판단 ----------
  // 시연 고객 목록 (오른쪽 패널)
  useEffect(() => {
    api.fetchCustomers().then(setCustomers).catch(() => {});
  }, []);

  // 챗봇을 처음 열면 "도움이 필요하세요?" + 메뉴 버튼을 서버에서 받아온다. 대화마다 한 번만.
  useEffect(() => {
    if (!chatOpen || menuRequested.current === conversationId) return;
    menuRequested.current = conversationId;
    (async () => {
      try {
        log('out', 'POST /api/chat/menu', { customerId });
        const res = await api.menu(conversationId, customerId);
        log('in', 'menu', res);
        showChatResult(res);
      } catch (e) {
        fail(e);
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chatOpen, conversationId]);

  // 서버의 분류 결과를 채팅창에 반영한다
  const showChatResult = (res) => {
    setLang(res.lang);
    if (res.escalation === 'FRAUD') {
      // 사기 의심: 경고를 보여주고 묻지 않고 바로 연결한다
      say({ from: 'ai', text: res.message, warn: true });
      doHandoff('FRAUD', res.lang);
    } else if (res.escalation === 'ADVICE') {
      showOffer(res.message, 'ADVICE');
    } else if (res.intent === 'AMBIGUOUS' || res.intent === 'MENU') {
      setOptions(res.options);
      say({ from: 'ai', text: res.message });
    } else if (res.intent === 'VERIFY') {
      // 카드 발급·상품 가입: 서버의 고객 정보로 확인 질문 → [네]/[아니요]
      setVerifyCategory(res.category);
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
      if (res.guide.type === 'DONE') {
        setSessionId(null);
        offerRestart();
        setChatOpen(true);
      }
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

  // 앱 화면의 예금·적금 기간 버튼 → 가입 내용 확인 화면
  const pickTerm = (kind, m) => {
    setJoinDraft({ kind, months: m });
    go('join-confirm');
  };

  // 채팅 질문 → 서버(LLM)가 업무·언어·이관 필요 여부를 판단
  const onAsk = async (text) => {
    say({ from: 'user', text });
    setOptions([]);
    setPendingIntent(null);
    setOffer(null);
    setVerifyCategory(null);
    setEligibility(null);
    setRestartOffered(false);
    setBusy(true);
    try {
      log('out', 'POST /api/chat', { text, customerId });
      const res = await api.chat(conversationId, text, customerId);
      log('in', `chat (${res.source})`, res);
      showChatResult(res);
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  };

  // 되묻기 선택지 또는 첫 메뉴 버튼을 골랐을 때
  const onChoose = async (intent) => {
    const chosen = options.find((o) => o.intent === intent);
    setOptions([]);
    setEligibility(null);
    setRestartOffered(false);
    say({ from: 'user', text: chosen?.name ?? intent });
    try {
      log('out', 'POST /api/chat/choose', { intent, lang, customerId });
      const res = await api.choose(conversationId, intent, lang, customerId);
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
    beginGuide(intent);
  };

  // 고객 정보 확인 [네]/[아니요] → 서버가 규칙으로 가입 자격 판단
  const onVerify = async (yes) => {
    const category = verifyCategory;
    setVerifyCategory(null);
    say({ from: 'user', text: t(lang, yes ? 'yes' : 'noChanged') });
    setBusy(true);
    try {
      const body = { conversationId, customerId, category, lang, confirmed: yes };
      log('out', 'POST /api/eligibility', body);
      const res = await api.checkEligibility(body);
      log('in', `eligibility (${res.status})`, res);
      if (res.status === 'MISMATCH') {
        // 은행 정보가 오래됨: 체류정보 갱신 안내 + "상담원을 연결해 드릴까요?"
        say({ from: 'ai', text: res.message });
        showOffer(res.offer, 'CUSTOMER_REQUEST', null);
      } else {
        say({ from: 'ai', text: res.message });
        setEligibility(res);
      }
    } catch (e) {
      fail(e);
    } finally {
      setBusy(false);
    }
  };

  // 가입 자격 결과에서 상품(·기간)을 고름 → 화면 안내 시작. 마지막 신청 버튼은 고객이 직접 누른다.
  const onPickProduct = (intent, name, term = null) => {
    setEligibility(null);
    say({ from: 'user', text: term ? `${name} · ${months(lang, term)}` : name });
    beginGuide(intent, term);
  };

  // 가입 불가 상품의 [상담원 연결]
  const onProductHandoff = (intent) => {
    setEligibility(null);
    say({ from: 'user', text: t(lang, 'connect') });
    doHandoff('CUSTOMER_REQUEST', lang, intent);
  };

  const beginGuide = async (intent, term = null) => {
    try {
      log('out', 'POST /api/guide/start', { intent, page, lang, term });
      const res = await api.startGuide(intent, page, lang, term);
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
  // 요청 모양은 develop의 /api/handoff 그대로. 가입 자격 쪽 사유·결과는 서버가 eligibility_check에 저장해 둔다.
  async function doHandoff(reason, l = lang, intent = currentIntent) {
    setOffer(null);
    setBusy(true);
    try {
      const body = { conversationId, sessionId, reason, page, lang: l, intent };
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
    doHandoff(offer.reason, lang, offer.intent);
  };
  const declineOffer = () => {
    say({ from: 'user', text: t(lang, 'continueSelf') });
    setOffer(null);
    offerRestart();
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

  // 챗봇 [↺ 처음으로]: 안내를 멈추고 기다리던 버튼을 정리한 뒤 메뉴를 다시 띄운다.
  // 지난 대화와 conversationId는 그대로 둔다 (상담원 요약 재료). 은행 앱 화면도 그대로.
  const restartChat = async () => {
    if (sessionId) stopGuide();
    setGuide(null);
    setPendingIntent(null);
    setOptions([]);
    setOffer(null);
    setVerifyCategory(null);
    setEligibility(null);
    setRestartOffered(false);
    try {
      log('out', 'POST /api/chat/menu', { customerId, lang });
      const res = await api.menu(conversationId, customerId, lang);
      log('in', 'menu', res);
      showChatResult(res);
    } catch (e) {
      fail(e);
    }
  };

  // 시연 고객 바꾸기: 다른 사람이 앱을 연 것처럼 대화와 안내를 처음부터 다시 시작한다
  const switchCustomer = (id) => {
    if (sessionId) api.stopGuide(sessionId).catch(() => {});
    const url = new URL(window.location.href);
    url.searchParams.set('customer', id);
    window.history.replaceState(null, '', url);

    setCustomerId(id);
    setConversationId(newId());
    setLang(customers.find((c) => c.customerId === id)?.lang ?? 'ko');
    setHistory(['home']);
    setSessionId(null);
    setGuide(null);
    setCurrentIntent(null);
    setChatOpen(false);
    setMessages([]);
    setPendingIntent(null);
    setOptions([]);
    setOffer(null);
    setTicketNo(null);
    setRestartOffered(false);
    setVerifyCategory(null);
    setEligibility(null);
    setTraffic([]);
    setDbEvents(null);
    setLastSessionId(null);
  };
  const customer = customers.find((c) => c.customerId === customerId);

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
    card: <CardPage go={go} back={back} />,
    'card-check-apply': <CardApplyPage kind="check" go={go} back={back} />,
    'card-credit-apply': <CardApplyPage kind="credit" go={go} back={back} />,
    'card-done': <DonePage text="카드 신청이 접수되었습니다. (데모 화면)" home={home} />,
    join: <JoinPage go={go} back={back} />,
    'deposit-join': <JoinTermPage kind="deposit" onPick={pickTerm} back={back} />,
    'savings-join': <JoinTermPage kind="savings" onPick={pickTerm} back={back} />,
    'join-confirm': <JoinConfirmPage draft={joinDraft} go={go} back={back} />,
    'join-done': <DonePage text="상품 가입 신청이 접수되었습니다. (데모 화면)" home={home} />,
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
          verifying={Boolean(verifyCategory)}
          eligibility={eligibility}
          restartOffered={restartOffered}
          onAsk={onAsk}
          onConfirm={onConfirm}
          onChoose={onChoose}
          onAcceptOffer={acceptOffer}
          onDeclineOffer={declineOffer}
          onVerify={onVerify}
          onPickProduct={onPickProduct}
          onProductHandoff={onProductHandoff}
          onRestart={restartChat}
        />
      </div>

      <aside className="dev-panel">
        <div className="customer-box">
          <label htmlFor="customer-select">시연 고객 (데모용 가상 고객)</label>
          <select id="customer-select" value={customerId} onChange={(e) => switchCustomer(e.target.value)}>
            {customers.length === 0 && <option value={customerId}>{customerId}</option>}
            {customers.map((c) => (
              <option key={c.customerId} value={c.customerId}>
                {c.customerId}. {c.name} ({c.nationality}, {c.visaType})
              </option>
            ))}
          </select>
          <p className="dev-hint">
            {customer && <>체류 만료 {customer.stayExpiry ?? '없음'} · 선호 언어 {customer.lang} · </>}
            바꾸면 대화를 처음부터 다시 시작합니다. 주소에 <code>?customer=B</code>로도 고를 수 있어요.
          </p>
        </div>

        <h2>서버 통신 로그</h2>
        <p className="dev-hint">
          화면 ↔ Spring Boot 서버(8080)가 주고받은 내용입니다. 멈춤 판단: {STUCK_SECONDS}초
        </p>
        <p className="dev-state">
          고객 <code>{customerId}</code> · 현재 화면 <code>{page}</code> · 고객 언어 <code>{lang}</code>
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

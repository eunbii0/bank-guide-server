import { useEffect, useRef, useState } from 'react';
import './App.css';
import {
  LanguagePage,
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
import { t, tp, months, LANGUAGES } from './i18n';

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
  const [customerOpen, setCustomerOpen] = useState(false);

  // 이 화면을 연 고객의 대화 전체를 묶는 id (4단계: 서버가 채팅 원문을 이 id로 저장). 고객을 바꾸면 새로 만든다.
  const [conversationId, setConversationId] = useState(newId);

  // 고객 언어. 앱을 켜면 맨 먼저 언어 선택 화면이 나오고(null = 아직 안 고름),
  // 고른 언어가 은행 앱 화면·챗봇·서버 안내 문구 전체에 똑같이 적용된다.
  const [lang, setLang] = useState(null);
  const langChosen = lang !== null;
  const [pickingLang, setPickingLang] = useState(false); // 홈에서 언어를 다시 고르는 중

  // 안내 상태. 몇 번째 단계인지는 서버가 기억하고, 화면은 sessionId만 들고 있다.
  const [sessionId, setSessionId] = useState(null);
  const [guide, setGuide] = useState(null);
  const [currentIntent, setCurrentIntent] = useState(null);

  // 채팅
  const [chatOpen, setChatOpen] = useState(false);
  const [busy, setBusy] = useState(false);
  // 첫 인사. 그 아래 메뉴 버튼은 챗봇을 열 때 서버(/api/chat/menu)에서 받아온다.
  const [messages, setMessages] = useState([{ from: 'ai', text: t('ko', 'welcome') }]);
  const [pendingIntent, setPendingIntent] = useState(null); // 네/아니요를 기다리는 업무
  const [options, setOptions] = useState([]); // 애매할 때 보여줄 선택지, 또는 첫 메뉴 버튼
  const [offer, setOffer] = useState(null); // 상담원 연결 제안 { reason, intent }
  const [ticketNo, setTicketNo] = useState(null); // 상담원에게 넘어간 티켓
  const [restartOffered, setRestartOffered] = useState(false); // "다른 업무도 도와드릴까요?" 뒤의 [처음으로] 버튼
  const [ticketClosed, setTicketClosed] = useState(false); // 상담원이 상담을 끝냈는지
  const [agentJoined, setAgentJoined] = useState(false); // 상담원이 상담을 시작했는지 (대기 → 연결됨)
  const [aiSuggestions, setAiSuggestions] = useState([]); // 대화 맥락으로 AI가 만든 추천 문장 (없으면 고정 예시)
  const talkingToAgent = Boolean(ticketNo) && !ticketClosed; // true면 입력창이 상담원에게 바로 간다
  const say = (...msgs) => setMessages((m) => [...m, ...msgs]);

  // 가입 자격 판단 (데모용 가상 규칙)
  const [verifyCategory, setVerifyCategory] = useState(null); // 고객 정보 확인 [네]/[아니요]를 기다리는 메뉴 (CARD, JOIN)
  const [eligibility, setEligibility] = useState(null); // 서버가 판단한 상품별 결과
  const [joinDraft, setJoinDraft] = useState({ kind: null, months: null }); // 앱 화면에서 고른 예금·적금과 기간
  const menuRequested = useRef(null); // 이 대화에서 메뉴를 이미 불렀는지 (conversationId)

  // 채팅을 닫았다가 다시 열면 구분선을 넣어, 그 위 대화가 '이전 대화'로 보이게 한다.
  // 인사는 고객이 직접 열 때만 하고, 상담원 답장·연결 제안처럼 저절로 열릴 때는 구분선만 넣는다.
  const closedBefore = useRef(false);
  const closeChat = () => {
    closedBefore.current = true;
    setChatOpen(false);
  };
  const beginNewSession = (greet) => {
    if (!closedBefore.current) return;
    closedBefore.current = false;
    const hello = talkingToAgent
      ? t(lang, 'greetingAgent')
      : messages.some((m) => m.from === 'user')
        ? t(lang, 'greeting')
        : t(lang, 'welcome');
    setMessages((m) => [...m, { divider: true }, ...(greet ? [{ from: 'ai', text: hello }] : [])]);
  };
  // 안내가 끝났거나 혼자 계속하기로 했을 때: 다른 업무로 넘어갈 수 있게 [처음으로]를 한 번 더 보여준다
  const offerRestart = () => {
    beginNewSession(false);
    say({ from: 'ai', text: t(lang, 'anotherTask') });
    setRestartOffered(true);
  };

  // 언어 선택. 아직 대화가 없으면 첫 인사를 그 언어로 바꾸고, 대화가 있으면 그 언어로 인사를 덧붙인다.
  // 메뉴 버튼을 이미 받아 둔 대화라면 그 언어로 다시 받아온다 (기다리던 버튼은 정리).
  // 안내 중이거나 상담원과 연결된 뒤에는 언어가 이미 정해져 있어서 홈의 언어 변경 버튼을 숨긴다.
  const langLocked = talkingToAgent || Boolean(sessionId);
  const changeLang = (code) => {
    setPickingLang(false);
    if (code === lang) return;
    setLang(code);
    setMessages((m) =>
      m.length === 1 && m[0].from === 'ai'
        ? [{ from: 'ai', text: t(code, 'welcome') }]
        : [...m, { from: 'ai', text: t(code, 'greeting') }],
    );
    clearPending();
    // 챗봇을 아직 안 열었다면 처음 열 때 새 언어로 메뉴를 받아오므로 여기서는 부르지 않는다
    if (menuRequested.current === conversationId) loadMenu(code);
  };

  const openChat = () => {
    beginNewSession(true);
    setChatOpen(true);
  };

  // ---------- 5단계: 상담원 답장 받기 ----------
  // 티켓이 생기면 3초마다 새 답장(고객 언어로 번역된 것)과 티켓 상태를 가져온다.
  const lastAgentId = useRef(0);
  const closedNotified = useRef(false); // 종료 안내를 한 번만 보여주기 위한 표시
  useEffect(() => {
    if (!ticketNo) return undefined;
    let alive = true;
    const timer = setInterval(async () => {
      try {
        const res = await api.fetchAgentMessages(ticketNo, lastAgentId.current);
        if (!alive) return;
        if (res.status === 'IN_PROGRESS') setAgentJoined(true);
        if (res.messages.length > 0) {
          lastAgentId.current = res.messages.at(-1).id;
          beginNewSession(false);
          say(...res.messages.map((m) => ({ from: 'agent', text: m.text })));
          setChatOpen(true);
          // 상담원의 새 말에 이어 보낼 만한 문장을 AI에게 받는다 (실패하면 고정 예시가 그대로 보인다)
          api.fetchSuggestions(ticketNo, 'customer').then((r) => alive && setAiSuggestions(r.suggestions));
        }
        if (res.status === 'CLOSED') {
          clearInterval(timer);
          setTicketClosed(true);
          if (!closedNotified.current) {
            closedNotified.current = true;
            say({ from: 'ai', text: t(lang, 'closed') });
          }
        }
      } catch {
        // 서버가 잠깐 안 될 수 있으니 조용히 다음 주기에 다시 시도한다
      }
    }, 3000);
    return () => {
      alive = false;
      clearInterval(timer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ticketNo]);

  // 서버와 주고받은 내용 (오른쪽 패널)
  const [traffic, setTraffic] = useState([]);
  const [logsOpen, setLogsOpen] = useState(false);
  const [dbEvents, setDbEvents] = useState(null);
  const [lastSessionId, setLastSessionId] = useState(null);
  const log = (dir, label, data) => setTraffic((prev) => [...prev, { t: now(), dir, label, data }]);

  const fail = (error, l = lang) => {
    const text = error instanceof api.ServerDownError ? t(l, 'serverDown') : error.message;
    log('err', 'ERROR', text);
    beginNewSession(false);
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
    beginNewSession(false);
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

  // 메뉴 버튼(카드 발급 / 상품 해지 / 상품 가입)을 서버에서 l 언어로 받아온다.
  // announce=false면 버튼만 바꾸고(첫 인사가 이미 있으므로), true면 "도움이 필요하세요?"도 말한다.
  const loadMenu = async (l, announce = false) => {
    try {
      log('out', 'POST /api/chat/menu', { customerId, lang: l });
      const res = await api.menu(conversationId, customerId, l);
      log('in', 'menu', res);
      if (announce) {
        showChatResult(res);
      } else {
        setOptions(res.options);
      }
    } catch (e) {
      fail(e);
    }
  };

  // 기다리던 네/아니요·선택지·연결 제안·상품 목록 버튼을 정리한다
  const clearPending = () => {
    setPendingIntent(null);
    setOptions([]);
    setOffer(null);
    setVerifyCategory(null);
    setEligibility(null);
    setRestartOffered(false);
  };

  // 챗봇을 처음 열면 첫 인사 아래에 메뉴 버튼을 띄운다. 대화마다 한 번만.
  useEffect(() => {
    if (!chatOpen || talkingToAgent || menuRequested.current === conversationId) return;
    menuRequested.current = conversationId;
    loadMenu(lang);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chatOpen, conversationId]);

  // 서버의 분류 결과를 채팅창에 반영한다
  const showChatResult = (res) => {
    if (res.escalation === 'FRAUD') {
      // 사기 의심: 경고를 보여주고 묻지 않고 바로 연결한다
      say({ from: 'ai', text: res.message, warn: true });
      doHandoff('FRAUD');
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
    // 상담원과 연결된 동안에는 AI 분류를 거치지 않고 상담원에게 바로 보낸다
    if (talkingToAgent) {
      setAiSuggestions([]);
      say({ from: 'user', text });
      try {
        log('out', `POST /api/tickets/${ticketNo}/customer-message`, { text });
        const res = await api.sendCustomerMessage(ticketNo, text);
        log('in', 'customer-message', res);
      } catch (e) {
        fail(e);
      }
      return;
    }
    say({ from: 'user', text });
    setOptions([]);
    setPendingIntent(null);
    setOffer(null);
    setVerifyCategory(null);
    setEligibility(null);
    setRestartOffered(false);
    setBusy(true);
    try {
      const hint = langChosen ? lang : undefined; // 직접 고른 언어가 있으면 서버에 알린다
      log('out', 'POST /api/chat', { text, customerId, lang: hint });
      const res = await api.chat(conversationId, text, customerId, hint);
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
      log('out', 'POST /api/guide/start', { conversationId, intent, page, lang, term });
      const res = await api.startGuide(conversationId, intent, page, lang, term);
      log('in', 'start', res);
      setSessionId(res.sessionId);
      setLastSessionId(res.sessionId);
      setCurrentIntent(intent);
      setGuide(res.guide);
      setDbEvents(null);
      say({ from: 'ai', text: t(lang, 'intro') });
      closeChat();
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
      setTicketClosed(false);
      setAgentJoined(false);
      lastAgentId.current = 0;
      closedNotified.current = false;
      say({ from: 'ai', text: res.message });
      setChatOpen(true);
    } catch (e) {
      fail(e, l);
    } finally {
      setBusy(false);
    }
  }

  // 고객이 직접 상담을 끝낸다 → 서버 티켓이 CLOSED가 되고 상담원 화면에도 종료로 보인다
  const endChat = async () => {
    if (!window.confirm(t(lang, 'endConfirm'))) return;
    try {
      log('out', `POST /api/tickets/${ticketNo}/customer-close`, {});
      const res = await api.closeTicketByCustomer(ticketNo);
      log('in', 'customer-close', { status: res.status });
      setTicketClosed(true);
      if (!closedNotified.current) {
        closedNotified.current = true;
        say({ from: 'ai', text: t(lang, 'closed') });
      }
    } catch (e) {
      fail(e);
    }
  };

  const acceptOffer = () => {
    say({ from: 'user', text: t(lang, 'connect') });
    doHandoff(offer.reason, lang, offer.intent);
  };
  const declineOffer = () => {
    say({ from: 'user', text: t(lang, 'continueSelf') });
    setOffer(null);
    offerRestart();
    if (sessionId) closeChat();
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
  const restartChat = () => {
    if (sessionId) stopGuide();
    setGuide(null);
    clearPending();
    loadMenu(lang, true);
  };

  // 시연 고객 바꾸기: 다른 사람이 앱을 연 것처럼 대화와 안내를 처음부터 다시 시작한다.
  // 언어도 다시 고르게 언어 선택 화면으로 돌아간다.
  const switchCustomer = (id) => {
    if (sessionId) api.stopGuide(sessionId).catch(() => {});
    const url = new URL(window.location.href);
    url.searchParams.set('customer', id);
    window.history.replaceState(null, '', url);

    setCustomerId(id);
    setConversationId(newId());
    setLang(null);
    setPickingLang(false);
    setHistory(['home']);
    setSessionId(null);
    setGuide(null);
    setCurrentIntent(null);
    setChatOpen(false);
    closedBefore.current = false;
    setMessages([{ from: 'ai', text: t('ko', 'welcome') }]);
    clearPending();
    setTicketNo(null);
    setTicketClosed(false);
    setAgentJoined(false);
    setAiSuggestions([]);
    lastAgentId.current = 0;
    closedNotified.current = false;
    setTraffic([]);
    setDbEvents(null);
    setLastSessionId(null);
  };
  const customer = customers.find((c) => c.customerId === customerId);

  // 언어를 고르기 전(또는 다시 고르는 중)에는 메인 화면 대신 언어 선택 화면만 보여준다
  const selectingLang = !langChosen || pickingLang;

  const pages = {
    home: <HomePage lang={lang} go={go} onChangeLang={langLocked ? undefined : () => setPickingLang(true)} />,
    product: <ProductPage lang={lang} go={go} back={back} />,
    loan: <LoanPage lang={lang} back={back} />,
    'deposit-list': <DepositListPage lang={lang} go={go} back={back} />,
    'deposit-cancel': <DepositCancelPage lang={lang} go={go} back={back} />,
    'deposit-done': <DonePage lang={lang} text={tp(lang, 'doneDeposit')} home={home} />,
    transfer: <TransferPage lang={lang} go={go} back={back} />,
    'transfer-done': <DonePage lang={lang} text={tp(lang, 'doneTransfer')} home={home} />,
    certificate: <CertificatePage lang={lang} go={go} back={back} />,
    'cert-issue': <CertIssuePage lang={lang} go={go} back={back} />,
    'cert-done': <DonePage lang={lang} text={tp(lang, 'doneCert')} home={home} />,
    card: <CardPage lang={lang} go={go} back={back} />,
    'card-check-apply': <CardApplyPage lang={lang} kind="check" go={go} back={back} />,
    'card-credit-apply': <CardApplyPage lang={lang} kind="credit" go={go} back={back} />,
    'card-done': <DonePage lang={lang} text={tp(lang, 'doneCard')} home={home} />,
    join: <JoinPage lang={lang} go={go} back={back} />,
    'deposit-join': <JoinTermPage lang={lang} kind="deposit" onPick={pickTerm} back={back} />,
    'savings-join': <JoinTermPage lang={lang} kind="savings" onPick={pickTerm} back={back} />,
    'join-confirm': <JoinConfirmPage lang={lang} draft={joinDraft} go={go} back={back} />,
    'join-done': <DonePage lang={lang} text={tp(lang, 'doneJoin')} home={home} />,
  };

  return (
    <main className="layout">
      <div className="phone">
        {selectingLang ? (
          <div className="screen">
            <LanguagePage languages={LANGUAGES} onPick={changeLang} title={tp(lang, 'pickLang')} />
          </div>
        ) : (
          <>
        <div className="screen">{pages[page]}</div>

        <GuideOverlay guide={guide} page={page} />
        {guide && guide.type !== 'DONE' && (
          <div className="guide-actions">
            <button className="guide-stop" onClick={openChat}>
              {t(lang, 'ask')}
            </button>
            <button className="guide-stop" onClick={stopGuide}>
              {t(lang, 'stop')}
            </button>
          </div>
        )}

        {!chatOpen && !guide && (
          <button className="ask-fab" onClick={openChat}>
            {ticketNo ? t(lang, 'waiting') : tp(lang, 'fab')}
          </button>
        )}
        <ChatPanel
          open={chatOpen}
          onClose={closeChat}
          lang={lang}
          busy={busy}
          messages={messages}
          pendingIntent={pendingIntent}
          options={options}
          offer={offer}
          waiting={talkingToAgent}
          agentJoined={agentJoined}
          suggestions={talkingToAgent ? aiSuggestions : []}
          onEndChat={endChat}
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
          </>
        )}
      </div>

      <aside className="dev-panel" aria-label="시연 설정">
        <section className="customer-box" aria-labelledby="customer-heading">
          <h2 id="customer-heading" className="customer-toggle-heading">
            <button
              type="button"
              className="customer-toggle"
              aria-expanded={customerOpen}
              aria-controls="customer-details"
              onClick={() => setCustomerOpen((open) => !open)}
            >
              <span className="customer-toggle-summary">
                <span>시연 고객</span>
                <span className="customer-current">{customer?.name ?? `고객 ${customerId}`}</span>
              </span>
              <span className="customer-toggle-action">{customerOpen ? '접기 −' : '펼치기 +'}</span>
            </button>
          </h2>
          <div id="customer-details" className="customer-details" hidden={!customerOpen}>
          {customer ? (
            <div className="customer-profile">
              <span className="profile-caption">선택한 고객 정보</span>
              <h3>{customer.name}</h3>
              <dl className="customer-facts">
                <div><dt>국적</dt><dd>{customer.nationality}</dd></div>
                <div><dt>체류자격</dt><dd>{customer.visaType}</dd></div>
                <div><dt>체류 만료일</dt><dd>{customer.stayExpiry ?? '만료 없음'}</dd></div>
                <div><dt>선호 언어</dt><dd>{LANGUAGES.find((l) => l.code === customer.lang)?.label ?? customer.lang}</dd></div>
              </dl>
            </div>
          ) : (
            <p className="customer-empty" role="status">고객 {customerId} · 고객 정보를 불러오지 못했습니다. 서버 연결을 확인해 주세요.</p>
          )}
          <p className="customer-intro">고객을 선택하고 앱에서 시연을 시작하세요.</p>
          <div className="customer-options" role="group" aria-label="시연 고객 선택">
            {customers.map((c) => (
              <button
                key={c.customerId}
                type="button"
                className={`customer-option${c.customerId === customerId ? ' selected' : ''}`}
                aria-pressed={c.customerId === customerId}
                onClick={() => c.customerId !== customerId && switchCustomer(c.customerId)}
              >
                <span className="customer-avatar" aria-hidden="true">{c.customerId}</span>
                <span className="customer-option-copy">
                  <strong>{c.name}</strong>
                  <span>{c.nationality} · {c.visaType}</span>
                </span>
                <span className="customer-selection">{c.customerId === customerId ? '선택됨' : '선택'}</span>
              </button>
            ))}
          </div>
          <p className="customer-note">
            고객을 변경하면 대화와 앱 화면이 처음부터 시작됩니다.
          </p>
          </div>
        </section>

        <button
          type="button"
          className="log-toggle"
          aria-expanded={logsOpen}
          aria-controls="server-logs"
          onClick={() => setLogsOpen((open) => !open)}
        >
          <span>서버 통신 로그 <span className="log-count">{traffic.length}</span></span>
          <span>{logsOpen ? '접기 −' : '펼치기 +'}</span>
        </button>
        <section id="server-logs" className="log-content" hidden={!logsOpen} aria-label="서버 통신 로그">
        <p className="dev-hint">
          화면 ↔ Spring Boot 서버(8080)가 주고받은 내용입니다. 멈춤 판단: {STUCK_SECONDS}초
        </p>
        <p className="dev-state">
          고객 <code>{customerId}</code> · 현재 화면 <code>{page}</code> · 고객 언어 <code>{lang ?? '미선택'}</code>
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

        </section>

        {ticketNo && <TicketPreview ticketNo={ticketNo} />}
      </aside>
    </main>
  );
}

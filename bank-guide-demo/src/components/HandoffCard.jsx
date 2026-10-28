import { useEffect, useRef, useState } from 'react';
import * as api from '../api/guideApi';
import { Icon } from './HomeArt';
import RoutePath from './RoutePath';
import ScreenMirror from './ScreenMirror';

// 서버가 주는 아이콘 이름 → 선 아이콘 (색은 줄의 tone으로 정한다)
const ICONS = {
  chat: 'chat',
  start: 'play-circle',
  step: 'check-circle',
  done: 'flag',
  'off-path': 'alert-circle',
  stuck: 'pause-circle',
  offer: 'help-circle',
  stop: 'stop-circle',
  handoff: 'headset',
  page: 'page',
};

const STATUS_BADGES = { WAITING: '처리 필요', IN_PROGRESS: '상담 중', CLOSED: '종료' };
const RECENT = 8; // 이력이 길면 최근 몇 줄만 먼저 보여준다

// 상담원 화면 '업무 인계' 카드: 제목 한 줄 → 핵심 정보 4칸 → 업무 진행 이력(선 아이콘) → 요약 → 권장 다음 행동.
// 상담원 연결 뒤의 화면 이동은 아래 대화창에 실시간으로 쌓인다 (현재 화면 칸도 함께 바뀐다).
// 이력은 챗봇 시작부터 상담원 연결까지 이 대화의 모든 기록으로, 요약의 재료와 같다.
// 티켓을 고를 때 한 번 읽어 온다 (연결 뒤에는 바뀌지 않으므로 3초마다 새로 읽지 않는다).
export default function HandoffCard({ ticket }) {
  const [timeline, setTimeline] = useState([]);
  const [showAll, setShowAll] = useState(false);

  useEffect(() => {
    let alive = true;
    setTimeline([]);
    setShowAll(false);
    api
      .fetchTicketEvents(ticket.ticketNo)
      .then((list) => alive && setTimeline(list))
      .catch(() => {});
    return () => {
      alive = false;
    };
  }, [ticket.ticketNo]);

  // 같은 티켓에서 현재 화면이 바뀌면 그 칸을 잠깐 강조한다 (key를 바꿔 애니메이션을 다시 시작)
  const [moved, setMoved] = useState(0);
  const last = useRef({ ticketNo: null, page: null });
  useEffect(() => {
    const prev = last.current;
    if (prev.ticketNo === ticket.ticketNo && prev.page !== ticket.currentPageId) setMoved((n) => n + 1);
    if (prev.ticketNo !== ticket.ticketNo) setMoved(0);
    last.current = { ticketNo: ticket.ticketNo, page: ticket.currentPageId };
  }, [ticket.ticketNo, ticket.currentPageId]);

  const route = ticket.route;
  const joinKind = route?.steps.some((s) => s.page === 'deposit-join') ? 'deposit' : 'savings';

  const hidden = showAll ? 0 : Math.max(0, timeline.length - RECENT);
  const shown = timeline.slice(hidden);

  return (
    <section className="handoff-card">
      <header className="hc-head">
        <div>
          <p className="hc-kicker">상담원 업무 인계 · 접수 {ticket.createdAt}</p>
          <h3 className="hc-title">{ticket.headline}</h3>
        </div>
        <span className={`hc-badge ${ticket.status.toLowerCase()}`}>{STATUS_BADGES[ticket.status] ?? ticket.status}</span>
      </header>

      <dl className="hc-grid">
        <div>
          <dt>고객 의도</dt>
          <dd>{ticket.intentName}</dd>
        </div>
        <div key={moved} className={moved > 0 ? 'hc-moved' : undefined}>
          <dt>
            현재 화면 {moved > 0 && <span className="hc-moved-badge">방금 이동</span>}
          </dt>
          <dd>{ticket.currentPage}</dd>
        </div>
        <div>
          <dt>연결 사유</dt>
          <dd>{ticket.reason}</dd>
        </div>
        <div>
          <dt>고객 언어</dt>
          <dd>{ticket.lang}</dd>
        </div>
      </dl>

      {ticket.currentPageId && (
        <div className="hc-section">
          <h4>고객 화면 {route && '· 경로 예측'}</h4>
          <div className="hc-live">
            <ScreenMirror page={ticket.currentPageId} target={route?.nextTarget} joinKind={joinKind} />
            {route ? (
              <RoutePath route={route} />
            ) : (
              <p className="hc-empty">업무가 정해지지 않아 예상 경로가 없습니다. 고객이 지금 보고 있는 화면만 보여줍니다.</p>
            )}
          </div>
          <p className="hc-note">고객 앱과 같은 화면을 다시 그린 것으로, 고객이 입력한 값은 보이지 않고 금액·계좌번호는 가립니다.</p>
        </div>
      )}

      <div className="hc-section">
        <h4>업무 진행 이력</h4>
        {timeline.length === 0 ? (
          <p className="hc-empty">화면 안내 없이 채팅으로만 상담했습니다.</p>
        ) : (
          <>
            {hidden > 0 && (
              <button type="button" className="hc-more" onClick={() => setShowAll(true)}>
                이전 기록 {hidden}개 더 보기
              </button>
            )}
            <ol className="hc-timeline">
              {shown.map((e, i) => (
                <li key={hidden + i} className={e.tone}>
                  <span className="hc-icon">
                    <Icon name={ICONS[e.icon] ?? 'page'} size={20} />
                  </span>
                  <span className="hc-text">{e.text}</span>
                  <time>{e.time}</time>
                </li>
              ))}
            </ol>
          </>
        )}
      </div>

      <div className="hc-section">
        <h4 className="hc-ai">
          <Icon name="sparkle" size={20} />
          {ticket.summarySource === 'LLM' ? 'AI 생성 상담 요약' : '규칙 기반 상담 요약'}
        </h4>
        <p className="hc-summary">{ticket.summary.join(' ')}</p>
        {ticket.checkpoints.length > 0 && (
          <div className="hc-next">
            <p>상담원 권장 다음 행동</p>
            <ul>
              {ticket.checkpoints.map((c) => (
                <li key={c}>{c}</li>
              ))}
            </ul>
          </div>
        )}
      </div>
    </section>
  );
}

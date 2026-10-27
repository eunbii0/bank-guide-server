import { useEffect, useState } from 'react';
import { fetchTicket } from '../api/guideApi';

const QUEUE_NAMES = { FOREIGN: '외국인 전담', FRAUD: '금융사기 대응', GENERAL: '일반' };
const SENDER_NAMES = { CUSTOMER: '고객', AI: 'AI', AGENT: '상담원' };

// 4단계: 상담원에게 넘어간 티켓 미리보기.
// 5단계에서 만들 상담원 화면이 보여줄 내용과 같다 (GET /api/tickets/{ticketNo}).
export default function TicketPreview({ ticketNo }) {
  const [ticket, setTicket] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    let alive = true;
    fetchTicket(ticketNo)
      .then((t) => alive && setTicket(t))
      .catch((e) => alive && setError(e.message));
    return () => {
      alive = false;
    };
  }, [ticketNo]);

  if (error) return <p className="dev-hint">티켓을 불러오지 못했습니다: {error}</p>;
  if (!ticket) return <p className="dev-hint">티켓 불러오는 중…</p>;

  return (
    <section className="ticket">
      <div className="ticket-head">
        <h2>상담 티켓 {ticket.ticketNo}</h2>
        <span className={`queue-badge ${ticket.queue.toLowerCase()}`}>{QUEUE_NAMES[ticket.queue] ?? ticket.queue}</span>
      </div>
      <p className="dev-hint">상담원 화면에 이렇게 전달됩니다 (요약: {ticket.summarySource === 'LLM' ? 'LLM' : '규칙 기반'})</p>

      <dl className="ticket-meta">
        <dt>고객 언어</dt>
        <dd>{ticket.lang}</dd>
        <dt>업무</dt>
        <dd>{ticket.intentName}</dd>
        <dt>연결 사유</dt>
        <dd>{ticket.reason}</dd>
        <dt>현재 화면</dt>
        <dd>{ticket.currentPage}</dd>
        {ticket.riskFlag !== 'NONE' && (
          <>
            <dt>위험</dt>
            <dd className="risk">금융사기 의심</dd>
          </>
        )}
      </dl>

      <h3>요약</h3>
      <ol className="ticket-list">
        {ticket.summary.map((s, i) => (
          <li key={i}>{s}</li>
        ))}
      </ol>

      <h3>확인할 것</h3>
      <ul className="ticket-list">
        {ticket.checkpoints.map((c, i) => (
          <li key={i}>{c}</li>
        ))}
      </ul>

      <details>
        <summary>원문 대화 {ticket.messages.length}건</summary>
        <ul className="ticket-list original">
          {ticket.messages.map((m, i) => (
            <li key={i}>
              <span className="dev-hint">
                {m.time} {SENDER_NAMES[m.sender] ?? m.sender} ({m.lang})
              </span>{' '}
              {m.content}
            </li>
          ))}
        </ul>
      </details>
    </section>
  );
}

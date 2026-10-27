import { useEffect, useState } from 'react';
import * as api from '../api/guideApi';

const TYPE_NAMES = {
  GUIDE_START: '안내 시작',
  PAGE_ENTER: '화면 진입',
  OFFER: '연결 제안',
  STUCK: '멈춤',
  GUIDE_STOP: '안내 중단',
  HANDOFF: '상담원 연결',
};

// 상담원 화면: 요약이 무엇을 보고 만들어졌는지 (기본은 접힘).
// 요약의 재료는 ① 연결 당시 진행 중이던 안내의 화면 이동 기록(DB guide_event)과 ② 아래 '대화' 원문이다.
// 펼칠 때 서버에서 읽어 온다 (3초마다 새로 읽지 않는다).
export default function SummarySources({ ticketNo }) {
  const [open, setOpen] = useState(false);
  const [events, setEvents] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    setOpen(false);
    setEvents(null);
    setError(null);
  }, [ticketNo]);

  const toggle = async () => {
    const next = !open;
    setOpen(next);
    if (!next) return;
    try {
      setEvents(await api.fetchTicketEvents(ticketNo));
      setError(null);
    } catch (e) {
      setError(e.message);
    }
  };

  return (
    <section className="sources">
      <button type="button" className="sources-toggle" aria-expanded={open} onClick={toggle}>
        요약에 쓰인 기록 {open ? '▾' : '▸'}
      </button>
      {open && (
        <div className="sources-body">
          <p className="sources-hint">
            요약은 아래 화면 이동 기록(DB guide_event)과 '대화' 원문으로 만들었습니다. 고객 정보·자격 판단 결과는 요약에 넣지 않습니다.
          </p>
          {error && <p className="sources-hint">기록을 불러오지 못했습니다: {error}</p>}
          {!error && events === null && <p className="sources-hint">불러오는 중…</p>}
          {events?.length === 0 && <p className="sources-hint">화면 안내 없이 채팅으로 연결된 상담이라 화면 이동 기록이 없습니다.</p>}
          {events?.length > 0 && (
            <table className="sources-table">
              <thead>
                <tr>
                  <th>시각</th>
                  <th>유형</th>
                  <th>화면</th>
                  <th>내용</th>
                </tr>
              </thead>
              <tbody>
                {events.map((e, i) => (
                  <tr key={i}>
                    <td>{e.time}</td>
                    <td>{TYPE_NAMES[e.type] ?? e.type}</td>
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
  );
}

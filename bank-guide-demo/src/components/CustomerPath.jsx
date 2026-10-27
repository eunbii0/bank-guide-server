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

// 상담원 화면 '고객 이동 경로': 연결 당시 진행 중이던 안내에서 고객이 지나온 화면 (기본은 접힘).
// 티켓을 고를 때 한 번 읽어 오고 (3초마다 새로 읽지 않는다), 기록이 없으면(채팅으로만 연결) 아무것도 보여주지 않는다.
export default function CustomerPath({ ticketNo }) {
  const [open, setOpen] = useState(false);
  const [events, setEvents] = useState([]);

  useEffect(() => {
    let alive = true;
    setOpen(false);
    setEvents([]);
    api
      .fetchTicketEvents(ticketNo)
      .then((list) => alive && setEvents(list))
      .catch(() => {});
    return () => {
      alive = false;
    };
  }, [ticketNo]);

  if (events.length === 0) return null;

  return (
    <section className="sources">
      <button type="button" className="sources-toggle" aria-expanded={open} onClick={() => setOpen((v) => !v)}>
        고객 이동 경로 {open ? '▾' : '▸'}
      </button>
      {open && (
        <div className="sources-body">
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
                  <td>{e.page}</td>
                  <td>{e.detail ?? '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

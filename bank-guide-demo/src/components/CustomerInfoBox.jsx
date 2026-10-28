import { useEffect, useState } from 'react';
import * as api from '../api/guideApi';

const CATEGORY_NAMES = { CARD: '카드 발급', JOIN: '상품 가입' };
const yesNo = (v) => (v ? '있음' : '없음');

// 상담원 화면의 '고객 정보' 칸 (상담 중 동의).
//  - 처음에는 아무 정보도 없다. [고객 정보 확인 요청]을 누르면 고객 채팅에 [허용]/[거절]이 뜬다
//  - 고객이 허용하면 [고객 정보 보기]로 체류자격·자격 판단 결과를 본다. 볼 때마다 서버에 조회 기록이 남는다
//  - 상담이 끝나면 다시 볼 수 없다
export default function CustomerInfoBox({ ticket, onChanged, onError }) {
  const [info, setInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const { ticketNo, status, infoConsent, hasCustomer } = ticket;
  const closed = status === 'CLOSED';

  // 다른 티켓을 고르거나, 허용이 풀리거나(상담 종료) 하면 보고 있던 정보를 지운다
  useEffect(() => {
    setInfo(null);
  }, [ticketNo]);
  useEffect(() => {
    if (closed || infoConsent !== 'GRANTED') setInfo(null);
  }, [closed, infoConsent]);

  const request = async () => {
    try {
      onChanged(await api.requestCustomerInfo(ticketNo));
    } catch (e) {
      onError(e);
    }
  };

  const view = async () => {
    setLoading(true);
    try {
      setInfo(await api.fetchCustomerInfo(ticketNo));
    } catch (e) {
      onError(e);
    } finally {
      setLoading(false);
    }
  };

  return (
    <section className="info-box" aria-labelledby="info-box-title">
      <div className="info-box-head">
        <h3 id="info-box-title">고객 정보</h3>
        <ConsentBadge consent={infoConsent} />
      </div>

      {!hasCustomer && <p className="info-hint">고객이 연결되지 않은 상담이라 정보를 요청할 수 없습니다.</p>}
      {hasCustomer && closed && <p className="info-hint">종료된 상담에서는 고객 정보를 볼 수 없습니다.</p>}

      {hasCustomer && !closed && (infoConsent === 'NONE' || infoConsent === 'DENIED') && (
        <>
          <p className="info-hint">
            {infoConsent === 'DENIED'
              ? '고객이 정보 확인을 거절했습니다. 필요한 내용은 직접 여쭤보세요.'
              : '체류자격·만료일, 가입 자격 판단 결과는 고객이 허용해야 볼 수 있습니다.'}
          </p>
          <button type="button" onClick={request}>
            {infoConsent === 'DENIED' ? '다시 요청' : '고객 정보 확인 요청'}
          </button>
        </>
      )}

      {hasCustomer && !closed && infoConsent === 'REQUESTED' && (
        <p className="info-hint">고객 화면에 [허용]/[거절]을 띄웠습니다. 고객의 답을 기다리는 중…</p>
      )}

      {hasCustomer && !closed && infoConsent === 'GRANTED' && (
        <>
          <div className="info-actions">
            <button type="button" onClick={view} disabled={loading}>
              {loading ? '불러오는 중…' : info ? '다시 조회' : '고객 정보 보기'}
            </button>
            <span className="info-hint">조회할 때마다 조회 기록이 남습니다.</span>
          </div>
          {info && <InfoDetail info={info} />}
        </>
      )}
    </section>
  );
}

function ConsentBadge({ consent }) {
  const names = { NONE: '요청 전', REQUESTED: '동의 대기', GRANTED: '고객 허용', DENIED: '고객 거절' };
  return <span className={`consent ${consent.toLowerCase()}`}>{names[consent] ?? consent}</span>;
}

function InfoDetail({ info }) {
  return (
    <div className="info-detail">
      <dl className="meta">
        <dt>이름</dt>
        <dd>
          {info.name} ({info.customerId})
        </dd>
        <dt>국적</dt>
        <dd>{info.nationalityKo}</dd>
        <dt>체류자격</dt>
        <dd>{info.visaType}</dd>
        <dt>체류 만료일</dt>
        <dd>{info.stayExpiry ?? '없음 (영주 등)'}</dd>
        <dt>외국인등록증</dt>
        <dd>{yesNo(info.hasArc)}</dd>
        <dt>입출금 계좌</dt>
        <dd>{yesNo(info.hasAccount)}</dd>
        <dt>소득 확인</dt>
        <dd>{info.incomeVerified ? '제출함' : '미제출'}</dd>
      </dl>

      <h4>가입 자격 판단 결과</h4>
      {info.eligibility.length === 0 && <p className="info-hint">이 대화에서 자격 판단을 하지 않았습니다.</p>}
      {info.eligibility.map((e, i) => (
        <div key={i} className="info-check">
          <p className="info-check-head">
            {e.time} · {CATEGORY_NAMES[e.category] ?? e.category} · 고객 정보 {e.confirmed ? '맞음' : '다르다고 답함'}
          </p>
          <ul>
            {e.result.map((line, j) => (
              <li key={j}>{line}</li>
            ))}
          </ul>
        </div>
      ))}

      <h4>조회 기록</h4>
      <ul className="info-log">
        {info.accessLog.map((a, i) => (
          <li key={i}>
            {a.time} · {a.agentId} · {a.items}
          </li>
        ))}
      </ul>
    </div>
  );
}

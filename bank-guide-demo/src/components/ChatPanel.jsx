import { useEffect, useRef, useState } from 'react';
import { t } from '../i18n';
import EligibilityList from './EligibilityList';

// AI 채팅창. 고객 질문을 받아 onAsk로 넘기고, 상황에 따라 버튼을 보여준다.
//  - 확인 버튼(네/아니요): 업무를 맞게 알아들었는지
//  - 선택지 버튼: 애매할 때 후보 업무, 챗봇 첫 메뉴(카드 발급 / 상품 해지 / 상품 가입)
//  - 고객 정보 확인 버튼(네/아니요, 바뀌었어요): 가입 자격 판단 전에
//  - 가입 자격 결과: 상품별 가능/불가 + 기간 선택 + 불가 상품의 [상담원 연결]
//  - 연결 제안 버튼(상담원 연결 / 혼자 계속): 판단 질문, 멈춤, 이탈 반복 (4단계), 고객 정보 불일치
// 제목줄: [↺ 처음으로] [×]. 언어는 시작 화면에서 고른다. 상담원과 대화 중(waiting)에는 ↺ 대신 [연결 상태] [상담 종료] [×]
export default function ChatPanel({
  open, onClose, lang, busy, messages, pendingIntent, options, offer, waiting, agentJoined, suggestions,
  onEndChat, onAsk, onConfirm, onChoose, onAcceptOffer, onDeclineOffer,
  verifying, eligibility, restartOffered, onVerify, onPickProduct, onProductHandoff, onRestart,
  infoRequest, onAnswerInfo,
}) {
  const [text, setText] = useState('');
  const bodyRef = useRef(null);

  // 새 메시지·버튼이 생기면 맨 아래로 스크롤
  useEffect(() => {
    const el = bodyRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [open, messages, busy, pendingIntent, options, offer, verifying, eligibility, restartOffered, infoRequest]);

  if (!open) return null;

  // 마지막 구분선 위의 말풍선은 '이전 대화'로 흐리게 보여준다
  const lastDivider = messages.map((m) => m.divider === true).lastIndexOf(true);

  const submit = (e) => {
    e.preventDefault();
    if (!text.trim() || busy) return;
    onAsk(text.trim());
    setText('');
  };

  return (
    <div className="chat-panel" role="dialog" aria-label={t(lang, 'chatTitle')}>
      <div className="chat-head">
        {/* 연결 상태는 제목 아래에 둔다: 한 줄에 다 넣으면 좁은 화면·긴 언어에서 [상담 종료]가 잘린다 */}
        <div className="chat-title">
          <strong>{t(lang, 'chatTitle')}</strong>
          {waiting && (
            <span className={`waiting-badge${agentJoined ? '' : ' pending'}`}>
              {t(lang, agentJoined ? 'connected' : 'waiting')}
            </span>
          )}
        </div>
        <div className="chat-head-actions">
          {!waiting && (
            <button className="chat-restart-icon" onClick={onRestart} disabled={busy} title={t(lang, 'restart')} aria-label={t(lang, 'restart')}>
              ↺
            </button>
          )}
          {waiting && (
            <button className="end-chat" onClick={onEndChat}>
              {t(lang, 'endChat')}
            </button>
          )}
          <button className="chat-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
      </div>
      <div className="chat-body" ref={bodyRef}>
        {messages.map((m, i) =>
          m.divider ? (
            <div key={i} className="chat-divider">
              <span>{t(lang, 'prevChat')}</span>
            </div>
          ) : (
            <div key={i} className={`bubble-wrap ${m.from}`}>
              <p className={`bubble ${m.from}${m.warn ? ' warn' : ''}${i < lastDivider ? ' old' : ''}`}>{m.text}</p>
              {m.note && <p className="bubble-note">{m.note}</p>}
            </div>
          ),
        )}
        {busy && <p className="bubble ai typing">…</p>}
        {pendingIntent && (
          <div className="confirm-row">
            <button className="pill-btn" onClick={() => onConfirm(true)}>
              {t(lang, 'yes')}
            </button>
            <button className="pill-btn ghost" onClick={() => onConfirm(false)}>
              {t(lang, 'no')}
            </button>
          </div>
        )}
        {options.length > 0 && (
          <div className="option-list">
            {options.map((o) => (
              <button key={o.intent} className="pill-btn ghost" onClick={() => onChoose(o.intent)}>
                {o.name}
              </button>
            ))}
          </div>
        )}
        {verifying && (
          <div className="confirm-row">
            <button className="pill-btn" onClick={() => onVerify(true)}>
              {t(lang, 'yes')}
            </button>
            <button className="pill-btn ghost" onClick={() => onVerify(false)}>
              {t(lang, 'noChanged')}
            </button>
          </div>
        )}
        {eligibility && (
          <EligibilityList lang={lang} result={eligibility} onPick={onPickProduct} onHandoff={onProductHandoff} />
        )}
        {restartOffered && (
          <div className="confirm-row">
            <button className="pill-btn" onClick={onRestart} disabled={busy}>
              ↺ {t(lang, 'restart')}
            </button>
          </div>
        )}
        {offer && (
          <div className="option-list">
            <button className="pill-btn" onClick={onAcceptOffer}>
              {t(lang, 'connect')}
            </button>
            <button className="pill-btn ghost" onClick={onDeclineOffer}>
              {t(lang, 'continueSelf')}
            </button>
            <p className="handoff-notice">{t(lang, 'handoffNotice')}</p>
          </div>
        )}
        {infoRequest && (
          <div className="info-request" role="group" aria-label={t(lang, 'infoRequestTitle')}>
            <strong>{t(lang, 'infoRequestTitle')}</strong>
            <p>{t(lang, 'infoRequest')}</p>
            <div className="confirm-row">
              <button className="pill-btn" onClick={() => onAnswerInfo(true)}>
                {t(lang, 'infoAllow')}
              </button>
              <button className="pill-btn ghost" onClick={() => onAnswerInfo(false)}>
                {t(lang, 'infoDeny')}
              </button>
            </div>
          </div>
        )}
      </div>
      {!busy && !pendingIntent && options.length === 0 && !offer && !verifying && !eligibility && (
        <div className="suggest-row">
          {(suggestions?.length > 0 ? suggestions : t(lang, waiting ? 'examplesAgent' : 'examples')).map((s) => (
            <button
              key={s}
              type="button"
              className={`suggest-chip${suggestions?.length > 0 ? ' ai' : ''}`}
              onClick={() => onAsk(s)}
            >
              {s}
            </button>
          ))}
        </div>
      )}
      <form className="chat-input" onSubmit={submit}>
        <input
          id="chat-text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder={t(lang, waiting ? 'placeholderAgent' : 'placeholder')}
        />
        <button type="submit" disabled={busy}>
          {t(lang, 'send')}
        </button>
      </form>
    </div>
  );
}

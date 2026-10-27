import { useEffect, useRef, useState } from 'react';
import { t } from '../i18n';
import EligibilityList from './EligibilityList';

// AI 채팅창. 고객 질문을 받아 onAsk로 넘기고, 상황에 따라 버튼을 보여준다.
//  - 확인 버튼(네/아니요): 업무를 맞게 알아들었는지
//  - 선택지 버튼: 애매할 때 후보 업무, 챗봇 첫 메뉴(카드 발급 / 상품 해지 / 상품 가입)
//  - 고객 정보 확인 버튼(네/아니요, 바뀌었어요): 가입 자격 판단 전에
//  - 가입 자격 결과: 상품별 가능/불가 + 기간 선택 + 불가 상품의 [상담원 연결]
//  - 연결 제안 버튼(상담원 연결 / 혼자 계속): 판단 질문, 멈춤, 이탈 반복 (4단계), 고객 정보 불일치
export default function ChatPanel({
  open, onClose, lang, busy, messages, pendingIntent, options, offer, waiting, verifying, eligibility, restartOffered,
  onAsk, onConfirm, onChoose, onAcceptOffer, onDeclineOffer, onVerify, onPickProduct, onProductHandoff, onRestart,
}) {
  const [text, setText] = useState('');
  const bodyRef = useRef(null);

  // 새 메시지·버튼이 생기면 맨 아래로 스크롤
  useEffect(() => {
    const el = bodyRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [open, messages, busy, pendingIntent, options, offer, verifying, eligibility, restartOffered]);

  if (!open) return null;

  const submit = (e) => {
    e.preventDefault();
    if (!text.trim() || busy) return;
    onAsk(text.trim());
    setText('');
  };

  return (
    <div className="chat-panel" role="dialog" aria-label={t(lang, 'chatTitle')}>
      <div className="chat-head">
        <strong>{t(lang, 'chatTitle')}</strong>
        <div className="chat-head-actions">
          {waiting && <span className="waiting-badge">{t(lang, 'waiting')}</span>}
          <button className="chat-restart-icon" onClick={onRestart} disabled={busy} title="처음으로" aria-label="처음으로">
            ↺
          </button>
          <button className="chat-close" onClick={onClose} aria-label="Close">
            ×
          </button>
        </div>
      </div>
      <div className="chat-body" ref={bodyRef}>
        {messages.map((m, i) => (
          <p key={i} className={`bubble ${m.from}${m.warn ? ' warn' : ''}`}>
            {m.text}
          </p>
        ))}
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
              ↺ 처음으로
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
          </div>
        )}
      </div>
      <form className="chat-input" onSubmit={submit}>
        <input
          id="chat-text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          placeholder={t(lang, 'placeholder')}
        />
        <button type="submit" disabled={busy}>
          {t(lang, 'send')}
        </button>
      </form>
    </div>
  );
}

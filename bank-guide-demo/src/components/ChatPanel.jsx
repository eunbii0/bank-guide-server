import { useEffect, useRef, useState } from 'react';
import { t } from '../i18n';

// AI 채팅창. 고객 질문을 받아 onAsk로 넘기고, 상황에 따라 버튼을 보여준다.
//  - 확인 버튼(네/아니요): 업무를 맞게 알아들었는지
//  - 선택지 버튼: 애매할 때 후보 업무
//  - 연결 제안 버튼(상담원 연결 / 혼자 계속): 판단 질문, 멈춤, 이탈 반복 (4단계)
export default function ChatPanel({
  open, onClose, lang, busy, messages, pendingIntent, options, offer, waiting,
  onAsk, onConfirm, onChoose, onAcceptOffer, onDeclineOffer,
}) {
  const [text, setText] = useState('');
  const bodyRef = useRef(null);

  // 새 메시지·버튼이 생기면 맨 아래로 스크롤
  useEffect(() => {
    const el = bodyRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [open, messages, busy, pendingIntent, options, offer]);

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
        {waiting && <span className="waiting-badge">{t(lang, 'waiting')}</span>}
        <button className="chat-close" onClick={onClose} aria-label="Close">
          ×
        </button>
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

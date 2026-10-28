import { useEffect } from 'react';
import { t } from '../i18n';

// 하이라이트의 전부: 대상 버튼을 id로 찾아 CSS 클래스를 붙였다 뗀다.
// 실제 강조 효과(나머지 화면 어둡게)는 App.css의 .guide-highlight에 있다.
//
// review가 true면 원래 단계의 버튼 대신 중요한 내용 박스(#info-review)를 먼저 강조하고,
// 고객이 [확인했어요]를 눌러야 원래 단계(기간·신청 버튼)로 넘어간다.
// 확인하는 동안에는 안내 바 밖의 화면을 눌러도 아무 일도 일어나지 않게 막는다.
export default function GuideOverlay({ guide, page, lang, review, onReviewDone }) {
  useEffect(() => {
    const target = review ? 'info-review' : guide?.target;
    if (!target) return;
    const el = document.getElementById(target);
    if (!el) return;
    el.classList.add('guide-highlight');
    el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    return () => el.classList.remove('guide-highlight');
  }, [guide, page, review]);

  if (!guide) return null;

  if (review) {
    return (
      <>
        <div className="guide-block" aria-hidden="true" />
        <div className="guide-bar review" role="status" aria-live="polite">
          <span>{t(lang, 'reviewPrompt')}</span>
          <button type="button" className="guide-confirm" onClick={onReviewDone}>
            {t(lang, 'reviewConfirm')}
          </button>
        </div>
      </>
    );
  }

  return (
    <div className={`guide-bar ${guide.type.toLowerCase()}`} role="status" aria-live="polite">
      {guide.type === 'STEP' && (
        <span className="guide-step">
          {guide.step}/{guide.total}
        </span>
      )}
      <span>{guide.message}</span>
    </div>
  );
}

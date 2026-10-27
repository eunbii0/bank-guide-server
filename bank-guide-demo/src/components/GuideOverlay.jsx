import { useEffect } from 'react';

// 하이라이트의 전부: 대상 버튼을 id로 찾아 CSS 클래스를 붙였다 뗀다.
// 실제 강조 효과(나머지 화면 어둡게)는 App.css의 .guide-highlight에 있다.
export default function GuideOverlay({ guide, page }) {
  useEffect(() => {
    if (!guide?.target) return;
    const el = document.getElementById(guide.target);
    if (!el) return;
    el.classList.add('guide-highlight');
    el.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    return () => el.classList.remove('guide-highlight');
  }, [guide, page]);

  if (!guide) return null;

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

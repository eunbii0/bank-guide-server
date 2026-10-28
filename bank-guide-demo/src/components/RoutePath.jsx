import { Fragment } from 'react';
import { Icon } from './HomeArt';

const STATE_LABELS = { current: '지금 여기', next: '다음 예상' };

// 경로 예측: 업무의 화면 순서를 위에서 아래로 화살표로 잇고, 지나온 화면·지금 화면·다음 예상 화면을 표시한다.
// 고객이 경로 밖 화면에 있으면 맨 위에 그 화면을 따로 보여주고, 돌아갈 화면을 '다음 예상'으로 짚는다.
export default function RoutePath({ route }) {
  return (
    <div className="route">
      {route.offPage && (
        <p className="route-off">
          <Icon name="alert-circle" size={18} />
          지금 <b>{route.offPage}</b> 화면 (안내 경로 밖)
        </p>
      )}
      <ol className="route-steps">
        {route.steps.map((s, i) => (
          <Fragment key={s.page}>
            {i > 0 && (
              <li className={`route-arrow${s.state === 'next' ? ' next' : ''}`} aria-hidden="true">
                ↓
              </li>
            )}
            <li className={`route-step ${s.state}`}>
              {s.state === 'done' && <Icon name="check-circle" size={16} />}
              <span>{s.name}</span>
              {STATE_LABELS[s.state] && <em>{STATE_LABELS[s.state]}</em>}
            </li>
          </Fragment>
        ))}
      </ol>
      <div className="route-action">
        <p>다음 예상 동작</p>
        <strong>{route.nextAction}</strong>
      </div>
    </div>
  );
}

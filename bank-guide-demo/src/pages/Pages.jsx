// 은행 앱 화면들. 안내 대상이 되는 버튼에는 반드시 id를 붙인다.
// (메뉴 지도의 target 값과 같은 id여야 하이라이트가 걸린다)
// 모든 문구는 시작 화면에서 고른 언어(lang)로 보여준다.
import { tp, months } from '../i18n';
import { Icon, QuickIcon, Sun, Bee, PromoArt, Donut } from '../components/HomeArt';

function Header({ title, onBack, backLabel, left, right }) {
  return (
    <header className="app-header">
      {onBack ? (
        <button id="btn-back" className="back-btn" onClick={onBack} aria-label={backLabel}>
          ‹
        </button>
      ) : (
        left ?? <span className="back-btn-space" />
      )}
      <h1>{title}</h1>
      {right ?? <span className="back-btn-space" />}
    </header>
  );
}

export function LanguagePage({ languages, onPick, title }) {
  return (
    <>
      <header className="app-header">
        <span className="back-btn-space" />
        <h1>🌐 {title}</h1>
        <span className="back-btn-space" />
      </header>
      <ul className="list lang-list">
        {languages.map((l) => (
          <li key={l.code}>
            <button className="list-btn" onClick={() => onPick(l.code)}>
              {l.label} <span>›</span>
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}

// 최근 거래내역 (데모용 가상 데이터). 이름·종류는 고른 언어 문구를 쓴다.
const RECENT = [
  { name: 'txShop', kind: 'txCard', time: '10. 26. 18:32', amount: -4500, color: '#0b7a55', mark: 'star' },
  { name: 'txBill', kind: 'txCard', time: '10. 26. 14:01', amount: -53000, color: '#ffc20e', mark: 'card' },
  { name: 'txPerson', kind: 'txTransfer', time: '10. 26. 11:20', amount: -20000, color: '#2f86f0', mark: 'people' },
  { name: 'txSalaryName', kind: 'txSalary', time: '10. 25. 09:00', amount: 2500000, color: '#3cc0ff', mark: 'won' },
];
const fmt = (n) => Math.abs(n).toLocaleString('en-US');

export function HomePage({ lang, go, onChangeLang, onAsk, userName, onLogout }) {
  const p = (k, v) => tp(lang, k, v);
  const quick = [
    { id: 'btn-transfer', to: 'transfer', icon: 'transfer', label: p('transfer') },
    { id: 'btn-card', to: 'card', icon: 'card', label: p('cards') },
    { id: 'btn-product', to: 'product', icon: 'bag', label: p('products') },
    { id: 'btn-join', to: 'join', icon: 'chart', label: p('join') },
    { id: 'btn-certificate', to: 'certificate', icon: 'doc', label: p('certificates') },
  ];
  return (
    <div className="home">
      <div className="status-bar" aria-hidden="true">
        <span className="status-time">9:41</span>
        <span className="status-icons">
          <svg width="18" height="12" viewBox="0 0 18 12" fill="currentColor">
            <rect x="0" y="8" width="3" height="4" rx="0.9" />
            <rect x="5" y="5.5" width="3" height="6.5" rx="0.9" />
            <rect x="10" y="3" width="3" height="9" rx="0.9" />
            <rect x="15" y="0" width="3" height="12" rx="0.9" />
          </svg>
          <svg width="17" height="12" viewBox="0 0 17 12" fill="none" stroke="currentColor" strokeWidth="1.9" strokeLinecap="round">
            <path d="M1.4 4.1a10.2 10.2 0 0 1 14.2 0" />
            <path d="M3.9 6.8a6.6 6.6 0 0 1 9.2 0" />
            <path d="M6.4 9.4a3.1 3.1 0 0 1 4.2 0" />
            <circle cx="8.5" cy="11" r="0.6" fill="currentColor" />
          </svg>
          <svg width="27" height="12" viewBox="0 0 27 12" fill="currentColor">
            <rect x="0.5" y="0.5" width="22" height="11" rx="3.4" fill="none" stroke="currentColor" opacity="0.4" />
            <rect x="2" y="2" width="19" height="8" rx="2.2" />
            <path d="M24.2 4v4a2 2 0 0 0 0-4z" opacity="0.5" />
          </svg>
        </span>
      </div>

      <header className="home-top">
        <div className="greet">
          {userName && <p className="greet-name">{p('hello', { name: userName })}</p>}
          <p className="greet-line">
            <span>{p('greetNeutral')}</span>
            <Sun />
          </p>
          <p className="greet-sub">{p('greetSub')}</p>
        </div>
        <div className="home-top-actions">
          <button
            className="icon-btn"
            onClick={onChangeLang}
            disabled={!onChangeLang}
            aria-label={onChangeLang ? p('changeLang') : p('menu')}
            title={onChangeLang ? p('changeLang') : p('menu')}
          >
            <Icon name="menu" size={24} />
          </button>
          {onLogout && (
            <button className="icon-btn" onClick={onLogout} aria-label={p('logout')} title={p('logout')}>
              {/* 문 밖으로 나가는 화살표 (HomeArt 아이콘과 같은 선 두께) */}
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                   strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <path d="M16 17l5-5-5-5" />
                <path d="M21 12H9" />
              </svg>
            </button>
          )}
        </div>
      </header>

      <button className="ai-banner" onClick={onAsk}>
        <span className="ai-banner-copy">
          <span className="ai-chip">{p('aiBadge')}</span>
          <strong>{p('aiTitle')}</strong>
          <span className="ai-desc">{p('aiDesc')}</span>
        </span>
        <span className="ai-bot">
          <Bee />
        </span>
        <span className="ai-input">
          <span>{p('aiPlaceholder')}</span>
          <Icon name="chevron" size={18} />
        </span>
      </button>

      <section className="asset-card">
        <svg className="asset-wave" viewBox="0 0 340 150" preserveAspectRatio="none" aria-hidden="true">
          <path d="M0 70C70 30 120 100 200 70S300 20 340 50V150H0z" fill="rgba(255,255,255,0.10)" />
          <path d="M0 105C80 70 150 130 230 100S310 70 340 90V150H0z" fill="rgba(255,255,255,0.08)" />
        </svg>
        <div className="asset-top">
          <span className="asset-label">
            {p('totalBalance')}
            <span className="eye">
              <Icon name="eye" size={14} />
            </span>
          </span>
          <span className="asset-pill">
            <Icon name="chart" size={13} />
            {p('viewAssets')}
          </span>
        </div>
        <p className="asset-amount">
          1,250,000{p('won')}
          <Icon name="chevron" size={18} />
        </p>
        <div className="quick-row">
          {quick.map((q) => (
            <button key={q.id} id={q.id} className="quick-btn" onClick={() => go(q.to)}>
              <span className="quick-icon">
                <QuickIcon name={q.icon} size={30} />
              </span>
              <span className="quick-label">{q.label}</span>
            </button>
          ))}
        </div>
      </section>

      <button className="promo-card" onClick={() => go('join')}>
        <span className="promo-copy">
          <small>{p('promoTag')}</small>
          <strong>
            {p('promoTitle')}
            <Icon name="chevron" size={15} />
          </strong>
        </span>
        <PromoArt />
      </button>

      <section className="recent-card">
        <div className="recent-head">
          <h2>{p('history')}</h2>
          <span>
            {p('viewAll')}
            <Icon name="chevron" size={13} />
          </span>
        </div>
        <ul>
          {RECENT.map((r) => (
            <li key={r.name}>
              <span className="recent-logo" style={{ background: r.color }}>
                <Icon name={r.mark} size={20} filled={r.mark === 'star'} />
              </span>
              <span className="recent-info">
                <strong>{p(r.name)}</strong>
                <small>
                  {r.time} <i>|</i> {p(r.kind)}
                </small>
              </span>
              <span className={`recent-amount${r.amount > 0 ? ' plus' : ''}`}>
                {r.amount > 0 ? '+' : '−'}
                {fmt(r.amount)}
                {p('won')}
              </span>
            </li>
          ))}
        </ul>
      </section>

      <div className="stat-row">
        <section className="stat-card">
          <div>
            <p className="stat-label">{p('spendMonth')}</p>
            <p className="stat-value">1,234,000{p('won')}</p>
            <span className="stat-badge">▾ -12%</span>
          </div>
          <span className="stat-bars" aria-hidden="true">
            <i style={{ height: 14 }} />
            <i style={{ height: 24 }} />
            <i style={{ height: 36 }} />
          </span>
        </section>
        <section className="stat-card">
          <div>
            <p className="stat-label">{p('budgetLeft')}</p>
            <p className="stat-value">566,000{p('won')}</p>
          </div>
          <Donut percent={68} size={60} />
        </section>
      </div>
    </div>
  );
}

// 하단 탭바. 홈 화면에서만 보인다.
export function TabBar({ lang, go }) {
  const p = (k, v) => tp(lang, k, v);
  const tabs = [
    { icon: 'home', label: p('tabHome'), active: true },
    { icon: 'pie', label: p('tabAssets') },
    { icon: 'transfer', label: p('transfer'), to: 'transfer' },
    { icon: 'bag', label: p('tabProducts'), to: 'product' },
    { icon: 'menu', label: p('tabAll') },
  ];
  return (
    <nav className="tab-bar">
      {tabs.map((t) => (
        <button key={t.icon} className={`tab${t.active ? ' on' : ''}`} onClick={t.to ? () => go(t.to) : undefined}>
          <Icon name={t.icon} size={25} filled={t.active} />
          <span>{t.label}</span>
        </button>
      ))}
      <i className="home-indicator" />
    </nav>
  );
}

export function ProductPage({ lang, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('products')} onBack={back} backLabel={p('back')} />
      <ul className="list">
        <li>
          <button id="btn-deposit" className="list-btn" onClick={() => go('deposit-list')}>
            {p('deposit')} <span>{p('count', { n: 1 })}</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            {p('savings')} <span>{p('count', { n: 0 })}</span>
          </button>
        </li>
        <li>
          <button id="btn-loan" className="list-btn" onClick={() => go('loan')}>
            {p('loan')} <span>{p('count', { n: 0 })}</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            {p('fund')} <span>{p('count', { n: 0 })}</span>
          </button>
        </li>
      </ul>
    </>
  );
}

export function LoanPage({ lang, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('loan')} onBack={back} backLabel={p('back')} />
      <p className="empty">{p('noLoan')}</p>
    </>
  );
}

export function DepositListPage({ lang, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('deposit')} onBack={back} backLabel={p('back')} />
      <section className="product-card">
        <p className="product-name">{p('timeDeposit')}</p>
        <p className="amount">10,000,000{p('won')}</p>
        <p className="meta">{p('depositMeta')}</p>
        <div className="product-actions">
          <button className="pill-btn ghost">{p('detail')}</button>
          <button id="btn-cancel" className="pill-btn" onClick={() => go('deposit-cancel')}>
            {p('earlyCancel')}
          </button>
        </div>
      </section>
    </>
  );
}

export function DepositCancelPage({ lang, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('earlyCancel')} onBack={back} backLabel={p('back')} />
      <section className="detail">
        <dl>
          <dt>{p('principal')}</dt>
          <dd>10,000,000{p('won')}</dd>
          <dt>{p('contractRate')}</dt>
          <dd>{p('rate32')}</dd>
          <dt>{p('earlyRate')}</dt>
          <dd className="warn">{p('rate05')}</dd>
          <dt>{p('expectedInterest')}</dt>
          <dd>29,000{p('won')}</dd>
        </dl>
        <p className="notice">{p('cancelNotice')}</p>
      </section>
      <button id="btn-cancel-confirm" className="primary-btn" onClick={() => go('deposit-done')}>
        {p('cancelRequest')}
      </button>
    </>
  );
}

export function TransferPage({ lang, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('transfer')} onBack={back} backLabel={p('back')} />
      <section className="detail">
        <dl>
          <dt>{p('recipient')}</dt>
          <dd>{p('recipientValue')}</dd>
          <dt>{p('sendAmount')}</dt>
          <dd>100,000{p('won')}</dd>
        </dl>
      </section>
      <button id="btn-transfer-next" className="primary-btn" onClick={() => go('transfer-done')}>
        {p('next')}
      </button>
    </>
  );
}

export function CertificatePage({ lang, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('certificates')} onBack={back} backLabel={p('back')} />
      <ul className="list">
        <li>
          <button id="btn-cert-balance" className="list-btn" onClick={() => go('cert-issue')}>
            {p('certBalance')} <span>›</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            {p('certTransaction')} <span>›</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            {p('certDebt')} <span>›</span>
          </button>
        </li>
      </ul>
    </>
  );
}

export function CertIssuePage({ lang, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('certBalance')} onBack={back} backLabel={p('back')} />
      <section className="detail">
        <dl>
          <dt>{p('targetAccount')}</dt>
          <dd>{p('targetAccountValue')}</dd>
          <dt>{p('baseDate')}</dt>
          <dd>2026.10.26</dd>
          <dt>{p('purpose')}</dt>
          <dd>{p('purposeValue')}</dd>
          <dt>{p('fee')}</dt>
          <dd>{p('feeValue')}</dd>
        </dl>
        <p className="notice">{p('certNotice')}</p>
      </section>
      <button id="btn-cert-issue" className="primary-btn" onClick={() => go('cert-done')}>
        {p('issue')}
      </button>
    </>
  );
}

// ---------- 카드 발급 ----------

export function CardPage({ lang, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('cards')} onBack={back} backLabel={p('back')} />
      <ul className="list">
        <li>
          <button id="btn-card-check" className="list-btn" onClick={() => go('card-check-apply')}>
            {p('cardCheckApply')} <span>›</span>
          </button>
        </li>
        <li>
          <button id="btn-card-credit" className="list-btn" onClick={() => go('card-credit-apply')}>
            {p('cardCreditApply')} <span>›</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            {p('ownedCards')} <span>{p('cardCount', { n: 0 })}</span>
          </button>
        </li>
      </ul>
    </>
  );
}

export function CardApplyPage({ lang, kind, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  const info =
    kind === 'check'
      ? { title: p('cardCheckApply'), name: p('cardNameCheck'), fee: p('feeNone'), pay: p('payCheck') }
      : { title: p('cardCreditApply'), name: p('cardNameCredit'), fee: p('feeCredit'), pay: p('payCredit') };
  return (
    <>
      <Header title={info.title} onBack={back} backLabel={p('back')} />
      <section id="info-review" className="detail">
        <dl>
          <dt>{p('cardProduct')}</dt>
          <dd>{info.name}</dd>
          <dt>{p('linkedAccount')}</dt>
          <dd>{p('checking')} 123-456-789012</dd>
          <dt>{p('annualFee')}</dt>
          <dd>{info.fee}</dd>
          <dt>{p('payMethod')}</dt>
          <dd>{info.pay}</dd>
          <dt>{p('delivery')}</dt>
          <dd>{p('deliveryValue')}</dd>
        </dl>
        <p className="notice">{p('cardNotice')}</p>
      </section>
      <button id="btn-card-apply" className="primary-btn" onClick={() => go('card-done')}>
        {p('apply')}
      </button>
    </>
  );
}

// ---------- 상품 가입 (정기예금·적금) ----------

export function JoinPage({ lang, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('join')} onBack={back} backLabel={p('back')} />
      <ul className="list">
        <li>
          <button id="btn-join-deposit" className="list-btn" onClick={() => go('deposit-join')}>
            {p('timeDeposit')} <span>{p('joinDepositSub')} ›</span>
          </button>
        </li>
        <li>
          <button id="btn-join-savings" className="list-btn" onClick={() => go('savings-join')}>
            {p('joinSavings')} <span>{p('joinSavingsSub')} ›</span>
          </button>
        </li>
      </ul>
    </>
  );
}

// 예금·적금 가입 안내 화면에 보여줄 상품 정보
const joinInfo = (p, kind) =>
  kind === 'deposit'
    ? { title: p('titleJoinDeposit'), name: p('nameDeposit'), amount: p('amountDeposit'), rate: p('rateDeposit') }
    : { title: p('titleJoinSavings'), name: p('nameSavings'), amount: p('amountSavings'), rate: p('rateSavings') };
const TERMS = [6, 12, 24];

// 가입 기간 버튼 id는 btn-term-6 처럼 개월 수를 붙인다 (안내 단계의 btn-term-{term})
export function JoinTermPage({ lang, kind, onPick, back }) {
  const p = (k, v) => tp(lang, k, v);
  const info = joinInfo(p, kind);
  return (
    <>
      <Header title={info.title} onBack={back} backLabel={p('back')} />
      <section id="info-review" className="detail">
        <dl>
          <dt>{p('product')}</dt>
          <dd>{info.name}</dd>
          <dt>{p('joinAmount')}</dt>
          <dd>{info.amount}</dd>
          <dt>{p('baseRate')}</dt>
          <dd>{info.rate}</dd>
        </dl>
      </section>
      <p className="section-title">{p('joinTerm')}</p>
      <div className="term-grid">
        {TERMS.map((m) => (
          <button key={m} id={`btn-term-${m}`} className="menu-tile" onClick={() => onPick(kind, m)}>
            {months(lang, m)}
          </button>
        ))}
      </div>
    </>
  );
}

export function JoinConfirmPage({ lang, draft, go, back }) {
  const p = (k, v) => tp(lang, k, v);
  const info = joinInfo(p, draft.kind === 'deposit' ? 'deposit' : 'savings');
  return (
    <>
      <Header title={p('joinConfirmTitle')} onBack={back} backLabel={p('back')} />
      <section id="info-review" className="detail">
        <dl>
          <dt>{p('product')}</dt>
          <dd>{info.name}</dd>
          <dt>{p('joinTerm')}</dt>
          <dd>{draft.months ? months(lang, draft.months) : '-'}</dd>
          <dt>{p('joinAmount')}</dt>
          <dd>{info.amount}</dd>
          <dt>{p('baseRate')}</dt>
          <dd>{info.rate}</dd>
          <dt>{p('withdrawAccount')}</dt>
          <dd>{p('checking')} 123-456-789012</dd>
        </dl>
        <p className="notice">{p('joinNotice')}</p>
      </section>
      <button id="btn-join-submit" className="primary-btn" onClick={() => go('join-done')}>
        {p('joinSubmit')}
      </button>
    </>
  );
}

export function DonePage({ lang, text, home, back }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('doneTitle')} onBack={back} backLabel={p('back')} />
      <p className="done-text">{text}</p>
      <button className="primary-btn" onClick={home}>
        {p('goHome')}
      </button>
    </>
  );
}

// 은행 앱 화면들. 안내 대상이 되는 버튼에는 반드시 id를 붙인다.
// (메뉴 지도의 target 값과 같은 id여야 하이라이트가 걸린다)
// 모든 문구는 시작 화면에서 고른 언어(lang)로 보여준다.
import { tp } from '../i18n';

function Header({ title, onBack, backLabel, right }) {
  return (
    <header className="app-header">
      {onBack ? (
        <button id="btn-back" className="back-btn" onClick={onBack} aria-label={backLabel}>
          ‹
        </button>
      ) : (
        <span className="back-btn-space" />
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

export function HomePage({ lang, go, onChangeLang }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header
        title={p('appTitle')}
        right={
          onChangeLang ? (
            <button className="back-btn" onClick={onChangeLang} aria-label={p('changeLang')} title={p('changeLang')}>
              🌐
            </button>
          ) : undefined
        }
      />
      <section className="account-card">
        <p className="account-name">{p('checking')}</p>
        <p className="account-no">123-456-789012</p>
        <p className="balance">1,250,000{p('won')}</p>
        <div className="account-actions">
          <button id="btn-transfer" className="pill-btn" onClick={() => go('transfer')}>
            {p('transfer')}
          </button>
          <button className="pill-btn ghost">{p('inquiry')}</button>
        </div>
      </section>
      <section className="menu-grid">
        <button id="btn-product" className="menu-tile" onClick={() => go('product')}>
          <span className="tile-icon">▤</span>{p('products')}
        </button>
        <button className="menu-tile">
          <span className="tile-icon">⇄</span>{p('history')}
        </button>
        <button id="btn-certificate" className="menu-tile" onClick={() => go('certificate')}>
          <span className="tile-icon">✎</span>{p('certificates')}
        </button>
        <button className="menu-tile">
          <span className="tile-icon">⚙</span>{p('settings')}
        </button>
      </section>
    </>
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

export function DonePage({ lang, text, home }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header title={p('doneTitle')} />
      <p className="done-text">{text}</p>
      <button className="primary-btn" onClick={home}>
        {p('goHome')}
      </button>
    </>
  );
}

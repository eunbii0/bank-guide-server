// 은행 앱 화면들. 안내 대상이 되는 버튼에는 반드시 id를 붙인다.
// (메뉴 지도의 target 값과 같은 id여야 하이라이트가 걸린다)
// 모든 문구는 시작 화면에서 고른 언어(lang)로 보여준다.
import { tp, months } from '../i18n';

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

// 로그인 대신 고객을 고르는 첫 화면. 언어를 고르기 전이라 한국어·영어를 함께 쓴다.
// 고객 정보(체류자격·만료일)는 보여주지 않고 이름·국적만 보여준다.
export function LoginPage({ customers, failed, onPick }) {
  return (
    <>
      <header className="app-header">
        <span className="back-btn-space" />
        <h1>ForWON</h1>
        <span className="back-btn-space" />
      </header>
      <div className="login-intro">
        <h2>로그인 · Sign in</h2>
        <p>고객을 선택하세요 · Choose your account</p>
      </div>
      {failed && <p className="login-status">서버에 연결할 수 없어요 · Cannot reach the server</p>}
      {!failed && customers.length === 0 && <p className="login-status">불러오는 중… · Loading…</p>}
      <ul className="list login-list">
        {customers.map((c) => (
          <li key={c.customerId}>
            <button className="list-btn login-btn" onClick={() => onPick(c.customerId)}>
              <span className="login-avatar" aria-hidden="true">{c.name.charAt(0)}</span>
              <span className="login-copy">
                <strong>{c.name}</strong>
                <span>{c.nationality}</span>
              </span>
              <span>›</span>
            </button>
          </li>
        ))}
      </ul>
    </>
  );
}

export function HomePage({ lang, go, onChangeLang, onLogout }) {
  const p = (k, v) => tp(lang, k, v);
  return (
    <>
      <Header
        title={p('appTitle')}
        left={
          onLogout ? (
            <button className="back-btn logout-btn" onClick={onLogout} aria-label={p('logout')} title={p('logout')}>
              {/* 문 밖으로 나가는 화살표 (글꼴에 따라 기호가 깨지지 않게 그림으로) */}
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
                   strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
                <path d="M16 17l5-5-5-5" />
                <path d="M21 12H9" />
              </svg>
            </button>
          ) : undefined
        }
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
        <button id="btn-card" className="menu-tile" onClick={() => go('card')}>
          <span className="tile-icon">▭</span>{p('cards')}
        </button>
        <button id="btn-join" className="menu-tile" onClick={() => go('join')}>
          <span className="tile-icon">＋</span>{p('join')}
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
      <section className="detail">
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
      <section className="detail">
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
      <section className="detail">
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

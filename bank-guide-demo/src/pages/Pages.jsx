// 은행 앱 화면들. 안내 대상이 되는 버튼에는 반드시 id를 붙인다.
// (메뉴 지도의 target 값과 같은 id여야 하이라이트가 걸린다)

function Header({ title, onBack }) {
  return (
    <header className="app-header">
      {onBack ? (
        <button id="btn-back" className="back-btn" onClick={onBack} aria-label="뒤로가기">
          ‹
        </button>
      ) : (
        <span className="back-btn-space" />
      )}
      <h1>{title}</h1>
      <span className="back-btn-space" />
    </header>
  );
}

export function HomePage({ go }) {
  return (
    <>
      <Header title="실습뱅킹" />
      <section className="account-card">
        <p className="account-name">입출금 통장</p>
        <p className="account-no">123-456-789012</p>
        <p className="balance">1,250,000원</p>
        <div className="account-actions">
          <button id="btn-transfer" className="pill-btn" onClick={() => go('transfer')}>
            이체
          </button>
          <button className="pill-btn ghost">조회</button>
        </div>
      </section>
      <section className="menu-grid">
        <button id="btn-product" className="menu-tile" onClick={() => go('product')}>
          <span className="tile-icon">▤</span>상품관리
        </button>
        <button className="menu-tile">
          <span className="tile-icon">⇄</span>거래내역
        </button>
        <button id="btn-card" className="menu-tile" onClick={() => go('card')}>
          <span className="tile-icon">▭</span>카드
        </button>
        <button id="btn-join" className="menu-tile" onClick={() => go('join')}>
          <span className="tile-icon">＋</span>상품가입
        </button>
        <button id="btn-certificate" className="menu-tile" onClick={() => go('certificate')}>
          <span className="tile-icon">✎</span>증명서
        </button>
        <button className="menu-tile">
          <span className="tile-icon">⚙</span>설정
        </button>
      </section>
    </>
  );
}

export function ProductPage({ go, back }) {
  return (
    <>
      <Header title="상품관리" onBack={back} />
      <ul className="list">
        <li>
          <button id="btn-deposit" className="list-btn" onClick={() => go('deposit-list')}>
            예금 <span>1건</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            적금 <span>0건</span>
          </button>
        </li>
        <li>
          <button id="btn-loan" className="list-btn" onClick={() => go('loan')}>
            대출 <span>0건</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            펀드 <span>0건</span>
          </button>
        </li>
      </ul>
    </>
  );
}

export function LoanPage({ back }) {
  return (
    <>
      <Header title="대출" onBack={back} />
      <p className="empty">보유 중인 대출 상품이 없습니다.</p>
    </>
  );
}

export function DepositListPage({ go, back }) {
  return (
    <>
      <Header title="예금" onBack={back} />
      <section className="product-card">
        <p className="product-name">정기예금</p>
        <p className="amount">10,000,000원</p>
        <p className="meta">약정이율 연 3.2% · 만기 2027.03.15</p>
        <div className="product-actions">
          <button className="pill-btn ghost">상세</button>
          <button id="btn-cancel" className="pill-btn" onClick={() => go('deposit-cancel')}>
            중도해지
          </button>
        </div>
      </section>
    </>
  );
}

export function DepositCancelPage({ go, back }) {
  return (
    <>
      <Header title="중도해지" onBack={back} />
      <section className="detail">
        <dl>
          <dt>원금</dt>
          <dd>10,000,000원</dd>
          <dt>약정이율</dt>
          <dd>연 3.2%</dd>
          <dt>중도해지이율</dt>
          <dd className="warn">연 0.5%</dd>
          <dt>예상 이자 (세전)</dt>
          <dd>29,000원</dd>
        </dl>
        <p className="notice">만기 전에 해지하면 약정이율보다 낮은 중도해지이율이 적용됩니다.</p>
      </section>
      <button id="btn-cancel-confirm" className="primary-btn" onClick={() => go('deposit-done')}>
        해지 신청
      </button>
    </>
  );
}

export function TransferPage({ go, back }) {
  return (
    <>
      <Header title="이체" onBack={back} />
      <section className="detail">
        <dl>
          <dt>받는 분</dt>
          <dd>김민준 (데모은행 111-222-333)</dd>
          <dt>보낼 금액</dt>
          <dd>100,000원</dd>
        </dl>
      </section>
      <button id="btn-transfer-next" className="primary-btn" onClick={() => go('transfer-done')}>
        다음
      </button>
    </>
  );
}

export function CertificatePage({ go, back }) {
  return (
    <>
      <Header title="증명서" onBack={back} />
      <ul className="list">
        <li>
          <button id="btn-cert-balance" className="list-btn" onClick={() => go('cert-issue')}>
            예금잔액증명서 <span>›</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            금융거래확인서 <span>›</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            부채증명서 <span>›</span>
          </button>
        </li>
      </ul>
    </>
  );
}

export function CertIssuePage({ go, back }) {
  return (
    <>
      <Header title="예금잔액증명서" onBack={back} />
      <section className="detail">
        <dl>
          <dt>대상 계좌</dt>
          <dd>입출금 통장 외 1건</dd>
          <dt>기준일</dt>
          <dd>2026.10.26</dd>
          <dt>발급 용도</dt>
          <dd>제출용</dd>
          <dt>수수료</dt>
          <dd>무료 (모바일)</dd>
        </dl>
        <p className="notice">발급된 증명서는 PDF로 저장하거나 제출처에 바로 보낼 수 있습니다.</p>
      </section>
      <button id="btn-cert-issue" className="primary-btn" onClick={() => go('cert-done')}>
        발급하기
      </button>
    </>
  );
}

// ---------- 카드 발급 ----------

export function CardPage({ go, back }) {
  return (
    <>
      <Header title="카드" onBack={back} />
      <ul className="list">
        <li>
          <button id="btn-card-check" className="list-btn" onClick={() => go('card-check-apply')}>
            체크카드 신청 <span>›</span>
          </button>
        </li>
        <li>
          <button id="btn-card-credit" className="list-btn" onClick={() => go('card-credit-apply')}>
            신용카드 신청 <span>›</span>
          </button>
        </li>
        <li>
          <button className="list-btn">
            보유 카드 <span>0장</span>
          </button>
        </li>
      </ul>
    </>
  );
}

const CARD_INFO = {
  check: { title: '체크카드 신청', name: '실습 체크카드', fee: '없음', pay: '사용 즉시 계좌에서 출금' },
  credit: { title: '신용카드 신청', name: '실습 신용카드', fee: '10,000원', pay: '매월 14일 결제' },
};

export function CardApplyPage({ kind, go, back }) {
  const info = CARD_INFO[kind];
  return (
    <>
      <Header title={info.title} onBack={back} />
      <section className="detail">
        <dl>
          <dt>카드 상품</dt>
          <dd>{info.name}</dd>
          <dt>연결 계좌</dt>
          <dd>입출금 통장 123-456-789012</dd>
          <dt>연회비</dt>
          <dd>{info.fee}</dd>
          <dt>결제 방식</dt>
          <dd>{info.pay}</dd>
          <dt>받는 방법</dt>
          <dd>등록된 주소로 우편 발송</dd>
        </dl>
        <p className="notice">외국인 고객의 카드 유효기간은 체류 만료일까지로 정해질 수 있습니다.</p>
      </section>
      <button id="btn-card-apply" className="primary-btn" onClick={() => go('card-done')}>
        신청하기
      </button>
    </>
  );
}

// ---------- 상품 가입 (정기예금·적금) ----------

export function JoinPage({ go, back }) {
  return (
    <>
      <Header title="상품가입" onBack={back} />
      <ul className="list">
        <li>
          <button id="btn-join-deposit" className="list-btn" onClick={() => go('deposit-join')}>
            정기예금 <span>목돈 맡기기 ›</span>
          </button>
        </li>
        <li>
          <button id="btn-join-savings" className="list-btn" onClick={() => go('savings-join')}>
            적금 <span>매달 모으기 ›</span>
          </button>
        </li>
      </ul>
    </>
  );
}

const JOIN_INFO = {
  deposit: { title: '정기예금 가입', name: '실습 정기예금', amount: '1,000,000원 (한 번에)', rate: '연 3.0%' },
  savings: { title: '적금 가입', name: '실습 적금', amount: '매월 100,000원', rate: '연 3.5%' },
};
const TERMS = [6, 12, 24];

// 가입 기간 버튼 id는 btn-term-6 처럼 개월 수를 붙인다 (안내 단계의 btn-term-{term})
export function JoinTermPage({ kind, onPick, back }) {
  const info = JOIN_INFO[kind];
  return (
    <>
      <Header title={info.title} onBack={back} />
      <section className="detail">
        <dl>
          <dt>상품</dt>
          <dd>{info.name}</dd>
          <dt>가입 금액</dt>
          <dd>{info.amount}</dd>
          <dt>기본 금리</dt>
          <dd>{info.rate}</dd>
        </dl>
      </section>
      <p className="section-title">가입 기간</p>
      <div className="term-grid">
        {TERMS.map((m) => (
          <button key={m} id={`btn-term-${m}`} className="menu-tile" onClick={() => onPick(kind, m)}>
            {m}개월
          </button>
        ))}
      </div>
    </>
  );
}

export function JoinConfirmPage({ draft, go, back }) {
  const info = JOIN_INFO[draft.kind] ?? JOIN_INFO.savings;
  return (
    <>
      <Header title="가입 내용 확인" onBack={back} />
      <section className="detail">
        <dl>
          <dt>상품</dt>
          <dd>{info.name}</dd>
          <dt>가입 기간</dt>
          <dd>{draft.months ?? '-'}개월</dd>
          <dt>가입 금액</dt>
          <dd>{info.amount}</dd>
          <dt>기본 금리</dt>
          <dd>{info.rate}</dd>
          <dt>출금 계좌</dt>
          <dd>입출금 통장 123-456-789012</dd>
        </dl>
        <p className="notice">만기 전에 해지하면 약정 금리보다 낮은 중도해지 금리가 적용됩니다.</p>
      </section>
      <button id="btn-join-submit" className="primary-btn" onClick={() => go('join-done')}>
        가입하기
      </button>
    </>
  );
}

export function DonePage({ text, home }) {
  return (
    <>
      <Header title="완료" />
      <p className="done-text">{text}</p>
      <button className="primary-btn" onClick={home}>
        홈으로
      </button>
    </>
  );
}

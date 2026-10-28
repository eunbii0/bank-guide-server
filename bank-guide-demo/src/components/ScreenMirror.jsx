import { useEffect, useRef } from 'react';
import {
  HomePage,
  ProductPage,
  LoanPage,
  DepositListPage,
  DepositCancelPage,
  TransferPage,
  CertificatePage,
  CertIssuePage,
  DonePage,
  CardPage,
  CardApplyPage,
  JoinPage,
  JoinTermPage,
  JoinConfirmPage,
} from '../pages/Pages';
import { tp } from '../i18n';

const noop = () => {};
// 금액(1,250,000)·계좌번호(123-456-789012)는 고객의 실제 정보로 오해하지 않게 가린다
const SENSITIVE = /\d{1,3}(?:,\d{3})+|\d{3,}-\d{2,}-\d{3,}/g;

// 고객 화면 미러: 고객이 지금 있는 화면을 고객 앱과 같은 화면 부품으로 다시 그린다 (한국어, 누를 수 없음).
// 실제 화면을 전송받는 화면 공유가 아니라 화면 ID만으로 그리므로 고객이 입력한 값은 보이지 않는다.
// target이 있으면 고객 화면 안내와 같은 하이라이트로 다음에 누를 버튼을 짚는다.
export default function ScreenMirror({ page, target, joinKind }) {
  const ref = useRef(null);
  const lang = 'ko';
  const p = (k) => tp(lang, k);

  const pages = {
    home: <HomePage lang={lang} go={noop} onAsk={noop} userName="고객" />,
    product: <ProductPage lang={lang} go={noop} back={noop} />,
    loan: <LoanPage lang={lang} back={noop} />,
    'deposit-list': <DepositListPage lang={lang} go={noop} back={noop} />,
    'deposit-cancel': <DepositCancelPage lang={lang} go={noop} back={noop} />,
    'deposit-done': <DonePage lang={lang} text={p('doneDeposit')} home={noop} back={noop} />,
    transfer: <TransferPage lang={lang} go={noop} back={noop} />,
    'transfer-done': <DonePage lang={lang} text={p('doneTransfer')} home={noop} back={noop} />,
    certificate: <CertificatePage lang={lang} go={noop} back={noop} />,
    'cert-issue': <CertIssuePage lang={lang} go={noop} back={noop} />,
    'cert-done': <DonePage lang={lang} text={p('doneCert')} home={noop} back={noop} />,
    card: <CardPage lang={lang} go={noop} back={noop} />,
    'card-check-apply': <CardApplyPage lang={lang} kind="check" go={noop} back={noop} />,
    'card-credit-apply': <CardApplyPage lang={lang} kind="credit" go={noop} back={noop} />,
    'card-done': <DonePage lang={lang} text={p('doneCard')} home={noop} back={noop} />,
    join: <JoinPage lang={lang} go={noop} back={noop} />,
    'deposit-join': <JoinTermPage lang={lang} kind="deposit" onPick={noop} back={noop} />,
    'savings-join': <JoinTermPage lang={lang} kind="savings" onPick={noop} back={noop} />,
    'join-confirm': <JoinConfirmPage lang={lang} draft={{ kind: joinKind, months: null }} go={noop} back={noop} />,
    'join-done': <DonePage lang={lang} text={p('doneJoin')} home={noop} back={noop} />,
  };

  // 화면을 그린 뒤: 민감해 보이는 숫자를 가리고, 다음에 누를 버튼을 하이라이트한다
  useEffect(() => {
    const root = ref.current;
    if (!root) return undefined;
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT);
    for (let n = walker.nextNode(); n; n = walker.nextNode()) {
      if (SENSITIVE.test(n.nodeValue)) n.nodeValue = n.nodeValue.replace(SENSITIVE, '***');
      SENSITIVE.lastIndex = 0;
    }
    const el = target ? root.querySelector(`[id="${target}"]`) : null;
    if (!el) return undefined;
    el.classList.add('guide-highlight');
    el.scrollIntoView({ block: 'nearest' });
    return () => el.classList.remove('guide-highlight');
  }, [page, target]);

  return (
    <div className="mirror-frame" aria-label="고객 화면 미러 (읽기 전용)">
      <div className="phone mirror-phone" ref={ref} inert>
        <div className="screen">{pages[page] ?? <p className="mirror-unknown">알 수 없는 화면: {page}</p>}</div>
      </div>
    </div>
  );
}

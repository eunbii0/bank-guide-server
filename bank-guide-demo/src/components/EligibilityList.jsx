import { months, t } from '../i18n';

// 가입 자격 결과 (서버 /api/eligibility, 데모용 가상 규칙).
// 판단은 서버가 하고, 화면은 받은 결과를 그대로 그린다. 문구도 모두 서버가 고객 언어로 만들어 보낸다.
//  - 가능: [이걸로 할게요] 또는 가입 기간 버튼 (만기일이 체류 만료일 뒤인 기간은 비활성 + 이유)
//  - 불가: 이유 + 대안 상품 버튼 + [상담원 연결]
export default function EligibilityList({ lang, result, onPick, onHandoff }) {
  const { products, notice, offer } = result;
  const byCode = Object.fromEntries(products.map((p) => [p.productCode, p]));

  return (
    <div className="elig-list">
      {products.map((p) => (
        <section key={p.productCode} className={`elig-item ${p.eligible ? 'ok' : 'no'}`}>
          <div className="elig-head">
            <strong>{p.name}</strong>
            <span className={`elig-badge ${p.eligible ? 'ok' : 'no'}`}>{t(lang, p.eligible ? 'eligible' : 'notEligible')}</span>
          </div>
          {p.note && <p className="elig-text">{p.note}</p>}
          {p.reasons.length > 0 && (
            <ul className="elig-reasons">
              {p.reasons.map((r) => (
                <li key={r}>{r}</li>
              ))}
            </ul>
          )}

          {p.eligible && p.terms.length === 0 && (
            <button className="pill-btn" onClick={() => onPick(p.intent, p.name)}>
              {t(lang, 'select')}
            </button>
          )}
          {p.eligible && p.terms.length > 0 && (
            <>
              <p className="elig-label">{t(lang, 'termLabel')}</p>
              <div className="term-row">
                {p.terms.map((term) => (
                  <button
                    key={term.months}
                    className="pill-btn"
                    disabled={!term.available}
                    onClick={() => onPick(p.intent, p.name, term.months)}
                  >
                    {months(lang, term.months)}
                  </button>
                ))}
              </div>
              {p.terms
                .filter((term) => !term.available)
                .map((term) => (
                  <p key={term.months} className="elig-text muted">
                    {months(lang, term.months)}: {term.reason}
                  </p>
                ))}
            </>
          )}

          {!p.eligible && (
            <div className="elig-actions">
              {p.alternative && (
                <>
                  <p className="elig-text">{p.alternative.message}</p>
                  {/* 대안이 기간을 골라야 하는 상품이면 목록에 있는 그 상품에서 고르게 한다 */}
                  {!byCode[p.alternative.productCode]?.terms.length && (
                    <button className="pill-btn" onClick={() => onPick(p.alternative.intent, p.alternative.name)}>
                      {p.alternative.name}
                    </button>
                  )}
                </>
              )}
              <button className="pill-btn ghost" onClick={() => onHandoff(p.intent)}>
                {t(lang, 'connect')}
              </button>
            </div>
          )}
        </section>
      ))}
      {offer && <p className="elig-offer">{offer}</p>}
      <p className="elig-notice">{notice}</p>
    </div>
  );
}

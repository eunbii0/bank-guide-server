// 홈 화면 전용 아이콘·일러스트 (외부 이미지 없이 SVG로 그린다)

const S = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.9, strokeLinecap: 'round', strokeLinejoin: 'round' };

const PATHS = {
  search: (
    <>
      <circle cx="11" cy="11" r="7" />
      <path d="M20 20l-4-4" />
    </>
  ),
  bell: (
    <>
      <path d="M6 16V11a6 6 0 1 1 12 0v5l1.6 2H4.4L6 16z" />
      <path d="M10 21h4" />
    </>
  ),
  menu: <path d="M4 7h16M4 12h16M4 17h16" />,
  eye: (
    <>
      <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z" />
      <circle cx="12" cy="12" r="2.8" />
    </>
  ),
  chevron: <path d="M9 5l7 7-7 7" />,
  chart: <path d="M5 20v-6M12 20V6M19 20v-10" />,
  transfer: (
    <>
      <path d="M4 8h15m0 0-3.5-3.5M19 8l-3.5 3.5" />
      <path d="M20 16H5m0 0 3.5-3.5M5 16l3.5 3.5" />
    </>
  ),
  card: (
    <>
      <rect x="3" y="5.5" width="18" height="13" rx="3" />
      <path d="M3 10h18M7 15h4" />
    </>
  ),
  bag: (
    <>
      <path d="M5 8h14l-1 12H6L5 8z" />
      <path d="M9 8V6.5a3 3 0 0 1 6 0V8" />
    </>
  ),
  grid: (
    <>
      <rect x="4" y="4" width="6.5" height="6.5" rx="1.8" />
      <rect x="13.5" y="4" width="6.5" height="6.5" rx="1.8" />
      <rect x="4" y="13.5" width="6.5" height="6.5" rx="1.8" />
      <rect x="13.5" y="13.5" width="6.5" height="6.5" rx="1.8" />
    </>
  ),
  doc: (
    <>
      <path d="M7 3.5h7l4 4V20.5H7z" />
      <path d="M14 3.5v4h4M10 12h5M10 16h5" />
    </>
  ),
  pie: (
    <>
      <circle cx="12" cy="12" r="8.5" />
      <path d="M12 3.5V12h8.5" />
    </>
  ),
  home: <path d="M4 11l8-7 8 7v8.5a1 1 0 0 1-1 1h-4.5v-6h-5v6H5a1 1 0 0 1-1-1V11z" />,
  people: (
    <>
      <circle cx="9" cy="9" r="3" />
      <path d="M3.5 19c.5-3.2 2.7-5 5.5-5s5 1.8 5.5 5" />
      <circle cx="17" cy="9.5" r="2.3" />
      <path d="M16.5 14.2c2.4 0 3.7 1.5 4 4" />
    </>
  ),
  won: <path d="M4 7l3 10 3-8 2 0 2 8 3-10M3 11h18M3 14h18" />,
  send: <path d="M4 11.5L20 4l-5.5 16-3.2-6.3z M11.3 13.7L20 4" />,
  close: <path d="M6 6l12 12M18 6L6 18" />,
  restart: (
    <>
      <path d="M4.5 12a7.5 7.5 0 1 0 2.4-5.5" />
      <path d="M4.5 4.5v4h4" />
    </>
  ),
  star: <path d="M12 3l2.6 5.6 6.1.7-4.5 4.2 1.2 6L12 16.5 6.6 19.5l1.2-6L3.3 9.3l6.1-.7L12 3z" />,
};

export function Icon({ name, size = 22, filled = false, ...rest }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" {...S} {...(filled ? { fill: 'currentColor' } : {})} {...rest} aria-hidden="true">
      {PATHS[name]}
    </svg>
  );
}

export function Sun({ size = 36 }) {
  const rays = Array.from({ length: 12 }, (_, i) => i * 30);
  return (
    <svg width={size} height={size} viewBox="0 0 40 40" aria-hidden="true">
      <defs>
        <radialGradient id="sunG" cx="40%" cy="35%" r="70%">
          <stop offset="0" stopColor="#ffe27a" />
          <stop offset="1" stopColor="#ffb703" />
        </radialGradient>
      </defs>
      {rays.map((a) => (
        <rect key={a} x="19" y="1.5" width="2.4" height="6" rx="1.2" fill="#ffc531" transform={`rotate(${a} 20.2 20)`} />
      ))}
      <circle cx="20" cy="20" r="9" fill="url(#sunG)" />
    </svg>
  );
}

// AI 챗봇 캐릭터: 꿀벌 (움직임은 App.css의 mk-* 애니메이션)
export function Bee() {
  return (
    <svg className="mk" viewBox="0 0 180 170" width="180" height="170" aria-hidden="true">
      <defs>
        <radialGradient id="mkBody" cx="0.35" cy="0.25" r="0.95">
          <stop offset="0" stopColor="#ffe98a" />
          <stop offset="0.5" stopColor="#ffc933" />
          <stop offset="1" stopColor="#f29a00" />
        </radialGradient>
        <radialGradient id="mkBlue" cx="0.35" cy="0.28" r="0.85">
          <stop offset="0" stopColor="#8fd3ff" />
          <stop offset="0.5" stopColor="#2f86f0" />
          <stop offset="1" stopColor="#1250c2" />
        </radialGradient>
        <clipPath id="mkClip">
          <ellipse cx="110" cy="98" rx="44" ry="40" />
        </clipPath>
      </defs>

      {/* 말풍선 (점 세 개가 깜빡인다) */}
      <g>
        <rect x="18" y="4" width="42" height="26" rx="13" fill="#fff" />
        <path d="M46 28l9 10 1-10z" fill="#fff" />
        <circle className="mk-dot" cx="29" cy="17" r="3.2" fill="url(#mkBlue)" />
        <circle className="mk-dot" style={{ animationDelay: '.2s' }} cx="39" cy="17" r="3.2" fill="url(#mkBlue)" />
        <circle className="mk-dot" style={{ animationDelay: '.4s' }} cx="49" cy="17" r="3.2" fill="url(#mkBlue)" />
      </g>

      <g className="mk-float">
        {/* 날개 */}
        <g className="mk-flap">
          <ellipse cx="86" cy="54" rx="14" ry="26" transform="rotate(-30 86 54)" fill="#fff" fillOpacity="0.88" stroke="#cfe6fb" strokeWidth="1.2" />
        </g>
        <g className="mk-flap" style={{ animationDelay: '.08s' }}>
          <ellipse cx="134" cy="54" rx="14" ry="26" transform="rotate(30 134 54)" fill="#fff" fillOpacity="0.88" stroke="#cfe6fb" strokeWidth="1.2" />
        </g>

        {/* 더듬이 */}
        <path d="M100 62q-4-17-13-22" fill="none" stroke="#4a3220" strokeWidth="2.6" strokeLinecap="round" />
        <path d="M120 62q4-17 13-22" fill="none" stroke="#4a3220" strokeWidth="2.6" strokeLinecap="round" />
        <circle cx="86" cy="38" r="5" fill="#4a3220" />
        <circle cx="134" cy="38" r="5" fill="#4a3220" />

        {/* 몸통 + 줄무늬 */}
        <ellipse cx="110" cy="98" rx="44" ry="40" fill="url(#mkBody)" />
        <g clipPath="url(#mkClip)">
          <path d="M60 112q50 8 100 0v7q-50 8-100 0z" fill="#4a3220" />
          <path d="M60 126q50 8 100 0v7q-50 8-100 0z" fill="#4a3220" />
        </g>
        <ellipse cx="90" cy="76" rx="15" ry="7" fill="#fff" opacity="0.45" transform="rotate(-24 90 76)" />

        {/* 얼굴 */}
        <g className="mk-eyes">
          <ellipse cx="92" cy="95" rx="5.2" ry="7.2" fill="#2a1a12" />
          <ellipse cx="128" cy="95" rx="5.2" ry="7.2" fill="#2a1a12" />
          <circle cx="93.6" cy="92" r="1.9" fill="#fff" />
          <circle cx="129.6" cy="92" r="1.9" fill="#fff" />
        </g>
        <ellipse cx="81" cy="106" rx="6.5" ry="4" fill="#ff8f9c" opacity="0.6" />
        <ellipse cx="139" cy="106" rx="6.5" ry="4" fill="#ff8f9c" opacity="0.6" />
        <path d="M103 104q7 7.5 14 0" fill="none" stroke="#2a1a12" strokeWidth="2.6" strokeLinecap="round" />

        {/* 팔: 왼쪽은 손을 흔든다 */}
        <g className="mk-greet">
          <ellipse cx="64" cy="108" rx="8.5" ry="13" fill="url(#mkBody)" transform="rotate(24 64 108)" />
        </g>
        <ellipse cx="156" cy="112" rx="8.5" ry="12" fill="url(#mkBody)" transform="rotate(-18 156 112)" />
      </g>
    </svg>
  );
}

// 자산 카드 빠른 메뉴용 컬러 아이콘 (두 가지 파랑으로 입체감을 낸다)
export function QuickIcon({ name, size = 30 }) {
  const body = {
    transfer: (
      <>
        <path d="M6 11h18m0 0-5-5M24 11l-5 5" fill="none" stroke="url(#qBlue)" strokeWidth="3.6" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M26 22H8m0 0 5-5M8 22l5 5" fill="none" stroke="url(#qLight)" strokeWidth="3.6" strokeLinecap="round" strokeLinejoin="round" />
      </>
    ),
    card: (
      <>
        <rect x="3.5" y="7.5" width="25" height="18" rx="5" fill="url(#qBlue)" />
        <rect x="3.5" y="12" width="25" height="4.2" fill="#0d49ad" opacity="0.5" />
        <rect x="8" y="19.4" width="7.5" height="3.4" rx="1.7" fill="#d4ecff" />
        <path d="M7 9.4h10" stroke="#fff" strokeWidth="1.4" strokeLinecap="round" opacity="0.55" />
      </>
    ),
    bag: (
      <>
        <path d="M11.5 12V9.8a4.5 4.5 0 0 1 9 0V12" fill="none" stroke="url(#qBlue)" strokeWidth="2.6" strokeLinecap="round" />
        <path d="M6 12h20l-1.5 14.2a2.4 2.4 0 0 1-2.4 2.1H9.9a2.4 2.4 0 0 1-2.4-2.1z" fill="url(#qBlue)" />
        <path d="M12.4 18.2q3.6 3.2 7.2 0" fill="none" stroke="#fff" strokeWidth="2" strokeLinecap="round" opacity="0.85" />
      </>
    ),
    chart: (
      <>
        <rect x="4.5" y="17" width="6" height="11" rx="2.3" fill="url(#qLight)" />
        <rect x="13" y="10.5" width="6" height="17.5" rx="2.3" fill="url(#qBlue)" />
        <rect x="21.5" y="4" width="6" height="24" rx="2.3" fill="url(#qBlue)" />
      </>
    ),
    doc: (
      <>
        <path d="M9 3h10l7 7v16a3 3 0 0 1-3 3H9a3 3 0 0 1-3-3V6a3 3 0 0 1 3-3z" fill="url(#qBlue)" />
        <path d="M19 3v4.6A2.4 2.4 0 0 0 21.4 10H26z" fill="url(#qLight)" />
        <rect x="10.5" y="15" width="11" height="2.4" rx="1.2" fill="#fff" opacity="0.92" />
        <rect x="10.5" y="20.2" width="8" height="2.4" rx="1.2" fill="#fff" opacity="0.92" />
      </>
    ),
  };
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <defs>
        <linearGradient id="qBlue" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="32">
          <stop offset="0" stopColor="#4fb0ff" />
          <stop offset="1" stopColor="#1a62dc" />
        </linearGradient>
        <linearGradient id="qLight" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="32">
          <stop offset="0" stopColor="#c6e8ff" />
          <stop offset="1" stopColor="#7cc0ff" />
        </linearGradient>
      </defs>
      {body[name]}
    </svg>
  );
}

// 금융상품 찾기 배너 일러스트 (문서 + 돋보기, 그림자로 입체감)
export function PromoArt() {
  return (
    <svg viewBox="0 0 150 100" width="150" height="100" aria-hidden="true">
      <defs>
        <linearGradient id="pmBand" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#6cc0ff" />
          <stop offset="1" stopColor="#1a6fe8" />
        </linearGradient>
        <linearGradient id="pmPage" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor="#ffffff" />
          <stop offset="1" stopColor="#dbe9fb" />
        </linearGradient>
        <radialGradient id="pmLens" cx="0.35" cy="0.3" r="0.85">
          <stop offset="0" stopColor="#ffffff" stopOpacity="0.95" />
          <stop offset="1" stopColor="#bfe0ff" stopOpacity="0.75" />
        </radialGradient>
        <linearGradient id="pmRim" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#58b1ff" />
          <stop offset="1" stopColor="#1b66dc" />
        </linearGradient>
        <radialGradient id="pmCoin" cx="0.35" cy="0.3" r="0.8">
          <stop offset="0" stopColor="#fff0a8" />
          <stop offset="1" stopColor="#ffb31a" />
        </radialGradient>
        <filter id="pmSh" x="-30%" y="-30%" width="170%" height="180%">
          <feDropShadow dx="0" dy="4" stdDeviation="3.2" floodColor="#0b4fa8" floodOpacity="0.38" />
        </filter>
        <filter id="pmBlur" x="-20%" y="-100%" width="140%" height="300%">
          <feGaussianBlur stdDeviation="3" />
        </filter>
      </defs>

      {/* 바닥 그림자 */}
      <ellipse cx="78" cy="92" rx="58" ry="6" fill="#0b4fa8" opacity="0.22" filter="url(#pmBlur)" />

      {/* 파란 띠 + 원화 동전 */}
      <g filter="url(#pmSh)">
        <path d="M8 72c4-14 30-12 50-6s52 4 72-12l8 16c-24 20-60 24-86 18S12 88 8 72z" fill="url(#pmBand)" />
      </g>
      <path d="M14 70c10-8 28-6 44-1" fill="none" stroke="#fff" strokeWidth="1.6" strokeLinecap="round" opacity="0.5" />
      <g filter="url(#pmSh)">
        <circle cx="34" cy="76" r="9.5" fill="url(#pmCoin)" />
      </g>
      <text x="34" y="80" textAnchor="middle" fontSize="11.5" fontWeight="800" fill="#b86e00">₩</text>

      {/* 문서 */}
      <g filter="url(#pmSh)" transform="rotate(-6 76 42)">
        <rect x="50" y="10" width="52" height="64" rx="8" fill="url(#pmPage)" />
      </g>
      <g transform="rotate(-6 76 42)">
        <rect x="58" y="22" width="32" height="6" rx="3" fill="#bcd8f8" />
        <rect x="58" y="36" width="32" height="6" rx="3" fill="#bcd8f8" />
        <rect x="58" y="50" width="22" height="6" rx="3" fill="#bcd8f8" />
      </g>

      {/* 돋보기 */}
      <g filter="url(#pmSh)">
        <path d="M121 67l13 13" stroke="url(#pmRim)" strokeWidth="9" strokeLinecap="round" />
        <circle cx="108" cy="52" r="20" fill="url(#pmLens)" stroke="url(#pmRim)" strokeWidth="7" />
      </g>
      <path d="M97 45q4-8 12-9" fill="none" stroke="#fff" strokeWidth="3" strokeLinecap="round" opacity="0.9" />
    </svg>
  );
}

// 이번 달 남은 예산 도넛 (percent: 0~100)
export function Donut({ percent, size = 68 }) {
  const r = 28;
  const c = 2 * Math.PI * r;
  return (
    <svg width={size} height={size} viewBox="0 0 70 70" aria-hidden="true">
      <circle cx="35" cy="35" r={r} fill="none" stroke="#dcecfb" strokeWidth="7" />
      <circle
        cx="35"
        cy="35"
        r={r}
        fill="none"
        stroke="#2b9bf0"
        strokeWidth="7"
        strokeLinecap="round"
        strokeDasharray={`${(c * percent) / 100} ${c}`}
        transform="rotate(-90 35 35)"
      />
      <text x="35" y="39.5" textAnchor="middle" fontSize="13" fontWeight="800" fill="#1a73e8">
        {percent}%
      </text>
    </svg>
  );
}

// 챗봇 창 제목줄의 꿀벌 아바타
export function BeeAvatar({ size = 40 }) {
  return (
    <svg width={size} height={size} viewBox="44 22 132 132" aria-hidden="true">
      <defs>
        <radialGradient id="avBody" cx="0.35" cy="0.25" r="0.95">
          <stop offset="0" stopColor="#ffe98a" />
          <stop offset="0.5" stopColor="#ffc933" />
          <stop offset="1" stopColor="#f29a00" />
        </radialGradient>
        <clipPath id="avClip">
          <ellipse cx="110" cy="98" rx="44" ry="40" />
        </clipPath>
      </defs>
      <circle cx="110" cy="88" r="66" fill="#e3f1ff" />

      {/* 날개 */}
      <ellipse cx="86" cy="54" rx="14" ry="26" transform="rotate(-30 86 54)" fill="#fff" fillOpacity="0.95" stroke="#cfe6fb" strokeWidth="1.2" />
      <ellipse cx="134" cy="54" rx="14" ry="26" transform="rotate(30 134 54)" fill="#fff" fillOpacity="0.95" stroke="#cfe6fb" strokeWidth="1.2" />

      {/* 더듬이 */}
      <path d="M100 62q-4-17-13-22" fill="none" stroke="#4a3220" strokeWidth="3" strokeLinecap="round" />
      <path d="M120 62q4-17 13-22" fill="none" stroke="#4a3220" strokeWidth="3" strokeLinecap="round" />
      <circle cx="86" cy="38" r="5.5" fill="#4a3220" />
      <circle cx="134" cy="38" r="5.5" fill="#4a3220" />

      {/* 몸통 + 검은 띠 두 줄 */}
      <ellipse cx="110" cy="98" rx="44" ry="40" fill="url(#avBody)" />
      <g clipPath="url(#avClip)">
        <path d="M60 112q50 8 100 0v7q-50 8-100 0z" fill="#4a3220" />
        <path d="M60 126q50 8 100 0v7q-50 8-100 0z" fill="#4a3220" />
      </g>
      <ellipse cx="90" cy="76" rx="15" ry="7" fill="#fff" opacity="0.45" transform="rotate(-24 90 76)" />

      {/* 얼굴 */}
      <ellipse cx="92" cy="95" rx="5.2" ry="7.2" fill="#2a1a12" />
      <ellipse cx="128" cy="95" rx="5.2" ry="7.2" fill="#2a1a12" />
      <circle cx="93.6" cy="92" r="1.9" fill="#fff" />
      <circle cx="129.6" cy="92" r="1.9" fill="#fff" />
      <ellipse cx="81" cy="106" rx="6.5" ry="4" fill="#ff8f9c" opacity="0.6" />
      <ellipse cx="139" cy="106" rx="6.5" ry="4" fill="#ff8f9c" opacity="0.6" />
      <path d="M103 104q7 7.5 14 0" fill="none" stroke="#2a1a12" strokeWidth="2.6" strokeLinecap="round" />

      {/* 팔 */}
      <ellipse cx="64" cy="108" rx="8.5" ry="13" fill="url(#avBody)" transform="rotate(24 64 108)" />
      <ellipse cx="156" cy="112" rx="8.5" ry="12" fill="url(#avBody)" transform="rotate(-18 156 112)" />
    </svg>
  );
}

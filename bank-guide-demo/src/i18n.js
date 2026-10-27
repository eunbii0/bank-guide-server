// 채팅창 같은 "안내 UI" 문구만 다국어로 둔다.
// 은행 앱 화면(상품관리, 예금 ...)은 실제 앱처럼 한국어 그대로 두고,
// 서버가 보내는 안내 문구가 "'상품관리' (My Products)"처럼 버튼 이름과 뜻을 함께 알려준다.

const TEXT = {
  ko: {
    chatTitle: 'AI 길안내',
    placeholder: '예) 예금 깨서 돈 찾고 싶어',
    yes: '네, 맞아요',
    no: '아니요',
    retry: '다시 한 번 말씀해 주세요.',
    intro: '버튼을 하나씩 알려드릴게요. 밝게 표시된 곳을 눌러주세요.',
    stop: '안내 그만하기',
    send: '전송',
    serverDown: '서버에 연결할 수 없어요. Spring Boot 서버(8080)가 켜져 있는지 확인해 주세요.',
    ask: '질문하기',
    connect: '상담원 연결',
    continueSelf: '혼자 계속할게요',
    waiting: '상담원 연결 대기 중',
  },
  en: {
    chatTitle: 'AI Guide',
    placeholder: 'e.g. I want to close my deposit early',
    yes: "Yes, that's right",
    no: 'No',
    retry: 'Please tell me again.',
    intro: "I'll show you each button. Tap the highlighted one.",
    stop: 'Stop guide',
    send: 'Send',
    serverDown: 'Cannot reach the server. Check that the Spring Boot server (8080) is running.',
    ask: 'Ask',
    connect: 'Connect to an agent',
    continueSelf: "I'll continue myself",
    waiting: 'Waiting for an agent',
  },
  vi: {
    chatTitle: 'Hướng dẫn AI',
    placeholder: 'VD: Tôi muốn tất toán tiền gửi',
    yes: 'Vâng, đúng rồi',
    no: 'Không',
    retry: 'Bạn vui lòng nói lại nhé.',
    intro: 'Tôi sẽ chỉ từng nút cho bạn. Hãy nhấn vào chỗ được làm sáng.',
    stop: 'Dừng hướng dẫn',
    send: 'Gửi',
    serverDown: 'Không kết nối được máy chủ. Hãy kiểm tra máy chủ Spring Boot (8080).',
    ask: 'Hỏi',
    connect: 'Kết nối nhân viên',
    continueSelf: 'Tôi sẽ tự làm tiếp',
    waiting: 'Đang chờ nhân viên tư vấn',
  },
  zh: {
    chatTitle: 'AI 指引',
    placeholder: '例如：我想提前解约存款',
    yes: '是的，没错',
    no: '不是',
    retry: '请再说一次。',
    intro: '我会逐一告诉您要点哪个按钮。请点击高亮的位置。',
    stop: '停止指引',
    send: '发送',
    serverDown: '无法连接服务器。请确认 Spring Boot 服务器（8080）已启动。',
    ask: '提问',
    connect: '转接客服',
    continueSelf: '我自己继续',
    waiting: '正在等待客服',
  },
};

// 처음엔 고객 언어를 모르므로 네 언어로 함께 인사한다.
export const GREETING = [
  '안녕하세요! 하고 싶은 업무를 편한 언어로 말씀해 주세요.',
  'Hello! Tell me what you want to do, in any language.',
  'Xin chào! Hãy cho tôi biết bạn muốn làm gì, bằng bất kỳ ngôn ngữ nào.',
  '你好！请用您习惯的语言告诉我您想办理的业务。',
].join('\n');

export const FAB_LABEL = 'Need help? · 도움이 필요하세요?';

export const t = (lang, key) => (TEXT[lang] ?? TEXT.en)[key] ?? TEXT.en[key];

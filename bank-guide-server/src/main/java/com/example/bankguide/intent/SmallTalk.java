package com.example.bankguide.intent;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * "안녕하세요", "고마워요", "no that's all, thank you" 같은 인사·감사·작별 말을 알아본다.
 * 업무가 아니라서 "무슨 업무인지 모르겠어요"로 떨어지므로, 챗봇이 대화하듯 인사·감사·작별로 답하게 한다.
 *
 * 알아보는 방법이 두 가지다.
 *  - match: 말 전체가 인사·감사·작별 하나일 때만 (LLM을 부르기 전에 먼저 걸러서 빠르고 키가 없어도 된다)
 *  - guess: 말 속에 그런 표현이 들어 있는지 (LLM이 안 될 때의 키워드 분류에서, 업무 말이 하나도 없을 때만 쓴다)
 * 평소에는 LLM이 대화 흐름까지 보고 판단한다 (LlmIntentClassifier의 talk).
 */
final class SmallTalk {

    enum Kind { GREETING, THANKS, GOODBYE }

    record Match(Kind kind, String lang) {}

    // ---------- 말 전체가 일치할 때 (match) ----------
    // 정규화한 말 → 언어. 정규화: 소문자 + 글자·숫자만 남기기 (공백·문장부호·이모지 제거)
    private static final Map<String, String> GREETINGS = Map.ofEntries(
            // 한국어
            Map.entry("안녕", "ko"), Map.entry("안녕하세요", "ko"), Map.entry("안녕하십니까", "ko"),
            Map.entry("안뇽", "ko"), Map.entry("하이", "ko"), Map.entry("헬로", "ko"), Map.entry("헬로우", "ko"),
            Map.entry("반갑습니다", "ko"), Map.entry("반가워요", "ko"),
            // 영어
            Map.entry("hi", "en"), Map.entry("hello", "en"), Map.entry("hey", "en"), Map.entry("hiya", "en"),
            Map.entry("heya", "en"), Map.entry("yo", "en"), Map.entry("hithere", "en"), Map.entry("hellothere", "en"),
            Map.entry("heythere", "en"), Map.entry("greetings", "en"), Map.entry("goodmorning", "en"),
            Map.entry("goodafternoon", "en"), Map.entry("goodevening", "en"),
            // 베트남어 (성조 없이 쓴 것도 받는다)
            Map.entry("xinchào", "vi"), Map.entry("chào", "vi"), Map.entry("chàobạn", "vi"),
            Map.entry("xinchao", "vi"), Map.entry("chao", "vi"),
            // 중국어
            Map.entry("你好", "zh"), Map.entry("您好", "zh"), Map.entry("你好啊", "zh"), Map.entry("嗨", "zh"),
            Map.entry("哈喽", "zh"), Map.entry("哈囉", "zh"), Map.entry("大家好", "zh"),
            // 우즈베크어
            Map.entry("salom", "uz"), Map.entry("assalomualaykum", "uz"), Map.entry("assalomalaykum", "uz"),
            // 네팔어
            Map.entry("नमस्ते", "ne"), Map.entry("नमस्कार", "ne"), Map.entry("हेलो", "ne"),
            // 크메르어
            Map.entry("សួស្តី", "km"), Map.entry("សួស្ដី", "km"), Map.entry("ជំរាបសួរ", "km"));

    private static final Map<String, String> THANKS = Map.ofEntries(
            // 한국어
            Map.entry("고마워", "ko"), Map.entry("고마워요", "ko"), Map.entry("고맙습니다", "ko"),
            Map.entry("감사", "ko"), Map.entry("감사해요", "ko"), Map.entry("감사합니다", "ko"),
            Map.entry("땡큐", "ko"), Map.entry("ㄳ", "ko"), Map.entry("ㄱㅅ", "ko"),
            // 영어
            Map.entry("thanks", "en"), Map.entry("thankyou", "en"), Map.entry("thx", "en"), Map.entry("ty", "en"),
            Map.entry("thanksalot", "en"), Map.entry("thankssomuch", "en"), Map.entry("thankyouverymuch", "en"),
            Map.entry("thankyousomuch", "en"), Map.entry("manythanks", "en"),
            // 베트남어 (성조 없이 쓴 것도 받는다)
            Map.entry("cảmơn", "vi"), Map.entry("cámơn", "vi"), Map.entry("cảmơnbạn", "vi"),
            Map.entry("cảmơnnhiều", "vi"), Map.entry("camon", "vi"), Map.entry("camonban", "vi"),
            // 중국어
            Map.entry("谢谢", "zh"), Map.entry("謝謝", "zh"), Map.entry("谢谢你", "zh"), Map.entry("谢谢您", "zh"),
            Map.entry("多谢", "zh"), Map.entry("感谢", "zh"), Map.entry("感谢您", "zh"), Map.entry("谢了", "zh"),
            // 우즈베크어
            Map.entry("rahmat", "uz"), Map.entry("kattarahmat", "uz"),
            // 네팔어
            Map.entry("धन्यवाद", "ne"), Map.entry("धेरैधन्यवाद", "ne"),
            // 크메르어
            Map.entry("អរគុណ", "km"), Map.entry("អរគុណច្រើន", "km"));

    private static final Map<String, String> GOODBYES = Map.ofEntries(
            Map.entry("bye", "en"), Map.entry("goodbye", "en"), Map.entry("byebye", "en"), Map.entry("seeyou", "en"),
            Map.entry("thatsall", "en"), Map.entry("안녕히계세요", "ko"), Map.entry("안녕히가세요", "ko"),
            Map.entry("잘가", "ko"), Map.entry("잘있어", "ko"), Map.entry("tạmbiệt", "vi"),
            Map.entry("再见", "zh"), Map.entry("拜拜", "zh"), Map.entry("xayr", "uz"),
            Map.entry("अलविदा", "ne"), Map.entry("លាហើយ", "km"));

    // ---------- 말 속에 들어 있을 때 (guess) ----------
    // 확인 순서: 작별 → 감사 → 인사. "no that's all, thank you"는 감사가 들어 있어도 작별이다.
    private static final List<Map.Entry<String, String>> GOODBYE_PARTS = List.of(
            Map.entry("that's all", "en"), Map.entry("thats all", "en"), Map.entry("that is all", "en"),
            Map.entry("no thank", "en"), Map.entry("nothing else", "en"), Map.entry("goodbye", "en"),
            Map.entry("bye", "en"), Map.entry("see you", "en"),
            Map.entry("이제 됐", "ko"), Map.entry("괜찮아요", "ko"), Map.entry("괜찮습니다", "ko"),
            Map.entry("그만할", "ko"), Map.entry("안녕히", "ko"), Map.entry("잘 가", "ko"), Map.entry("잘가", "ko"),
            Map.entry("tạm biệt", "vi"), Map.entry("再见", "zh"), Map.entry("拜拜", "zh"), Map.entry("不用了", "zh"),
            Map.entry("没有了", "zh"), Map.entry("xayr", "uz"), Map.entry("अलविदा", "ne"), Map.entry("លាហើយ", "km"));

    private static final List<Map.Entry<String, String>> THANKS_PARTS = List.of(
            Map.entry("thank", "en"), Map.entry("thx", "en"),
            Map.entry("고마", "ko"), Map.entry("고맙", "ko"), Map.entry("감사", "ko"),
            Map.entry("땡큐", "ko"), Map.entry("땡스", "ko"),
            Map.entry("cảm ơn", "vi"), Map.entry("cám ơn", "vi"), Map.entry("cam on", "vi"),
            Map.entry("谢谢", "zh"), Map.entry("謝謝", "zh"), Map.entry("多谢", "zh"), Map.entry("感谢", "zh"),
            Map.entry("rahmat", "uz"), Map.entry("धन्यवाद", "ne"), Map.entry("អរគុណ", "km"));

    private static final List<Map.Entry<String, String>> GREETING_PARTS = List.of(
            Map.entry("안녕", "ko"), Map.entry("하이", "ko"), Map.entry("헬로", "ko"),
            Map.entry("xin chào", "vi"), Map.entry("你好", "zh"), Map.entry("您好", "zh"),
            Map.entry("salom", "uz"), Map.entry("नमस्ते", "ne"), Map.entry("សួស្តី", "km"));
    // 영어 인사는 hi가 this, which 안에 들어 있을 수 있어서 낱말 단위로만 본다
    private static final Pattern ENGLISH_GREETING = Pattern.compile("\\b(hi|hello|hey)\\b");

    private SmallTalk() {}

    /** 말 전체가 인사·감사·작별 하나이면 그 종류와 언어, 아니면 빈 값 */
    static Optional<Match> match(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String key = Normalizer.normalize(text, Normalizer.Form.NFC)
                .toLowerCase()
                .replaceAll("[^\\p{L}\\p{M}\\p{N}]", "")
                .replaceAll("(.)\\1{2,}", "$1"); // "hiii", "thanksss"처럼 늘여 쓴 것
        String greeting = GREETINGS.get(key);
        if (greeting != null) {
            return Optional.of(new Match(Kind.GREETING, greeting));
        }
        String thanks = THANKS.get(key);
        if (thanks != null) {
            return Optional.of(new Match(Kind.THANKS, thanks));
        }
        String goodbye = GOODBYES.get(key);
        return goodbye == null ? Optional.empty() : Optional.of(new Match(Kind.GOODBYE, goodbye));
    }

    /**
     * 말 속에 인사·감사·작별 표현이 들어 있으면 그 종류와 언어, 아니면 빈 값.
     * 업무 말도 같이 있는지는 보지 않는다. 호출하는 쪽이 업무 키워드가 하나도 없을 때만 부른다.
     */
    static Optional<Match> guess(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String lower = Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase().replace('’', '\'');
        Optional<String> goodbye = firstLang(lower, GOODBYE_PARTS);
        if (goodbye.isPresent()) {
            return Optional.of(new Match(Kind.GOODBYE, goodbye.get()));
        }
        Optional<String> thanks = firstLang(lower, THANKS_PARTS);
        if (thanks.isPresent()) {
            return Optional.of(new Match(Kind.THANKS, thanks.get()));
        }
        Optional<String> greeting = firstLang(lower, GREETING_PARTS);
        if (greeting.isPresent()) {
            return Optional.of(new Match(Kind.GREETING, greeting.get()));
        }
        return ENGLISH_GREETING.matcher(lower).find()
                ? Optional.of(new Match(Kind.GREETING, "en"))
                : Optional.empty();
    }

    private static Optional<String> firstLang(String lower, List<Map.Entry<String, String>> parts) {
        return parts.stream().filter(p -> lower.contains(p.getKey())).map(Map.Entry::getValue).findFirst();
    }
}

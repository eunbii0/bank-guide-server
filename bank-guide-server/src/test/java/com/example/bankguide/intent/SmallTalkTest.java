package com.example.bankguide.intent;

import com.example.bankguide.intent.SmallTalk.Kind;
import com.example.bankguide.intent.SmallTalk.Match;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmallTalkTest {

    private static Optional<Match> greeting(String lang) {
        return Optional.of(new Match(Kind.GREETING, lang));
    }

    private static Optional<Match> thanks(String lang) {
        return Optional.of(new Match(Kind.THANKS, lang));
    }

    private static Optional<Match> goodbye(String lang) {
        return Optional.of(new Match(Kind.GOODBYE, lang));
    }

    @Test
    void 언어별_인사말은_그_언어의_인사로_알아본다() {
        assertEquals(greeting("ko"), SmallTalk.match("안녕"));
        assertEquals(greeting("ko"), SmallTalk.match("안녕하세요"));
        assertEquals(greeting("en"), SmallTalk.match("hi"));
        assertEquals(greeting("en"), SmallTalk.match("Hello"));
        assertEquals(greeting("vi"), SmallTalk.match("Xin chào"));
        assertEquals(greeting("zh"), SmallTalk.match("你好"));
        assertEquals(greeting("uz"), SmallTalk.match("Salom"));
        assertEquals(greeting("ne"), SmallTalk.match("नमस्ते"));
        assertEquals(greeting("km"), SmallTalk.match("សួស្តី"));
    }

    @Test
    void 언어별_감사_인사는_그_언어의_감사로_알아본다() {
        assertEquals(thanks("ko"), SmallTalk.match("고마워"));
        assertEquals(thanks("ko"), SmallTalk.match("감사합니다"));
        assertEquals(thanks("en"), SmallTalk.match("thanks"));
        assertEquals(thanks("en"), SmallTalk.match("Thank you!"));
        assertEquals(thanks("vi"), SmallTalk.match("Cảm ơn bạn"));
        assertEquals(thanks("zh"), SmallTalk.match("谢谢"));
        assertEquals(thanks("uz"), SmallTalk.match("Rahmat"));
        assertEquals(thanks("ne"), SmallTalk.match("धन्यवाद"));
        assertEquals(thanks("km"), SmallTalk.match("អរគុណ"));
    }

    @Test
    void 문장부호_공백_이모지_늘여쓰기가_있어도_알아본다() {
        assertEquals(greeting("ko"), SmallTalk.match(" 안녕하세요! "));
        assertEquals(greeting("en"), SmallTalk.match("Hi there :)"));
        assertEquals(greeting("en"), SmallTalk.match("hiii"));
        assertEquals(greeting("en"), SmallTalk.match("Good morning!"));
        assertEquals(thanks("ko"), SmallTalk.match("고마워요~"));
        assertEquals(thanks("en"), SmallTalk.match("thanksss!!"));
    }

    @Test
    void 업무_말이_섞이면_인사나_감사가_아니다() {
        assertEquals(Optional.empty(), SmallTalk.match("hi, I want a debit card"));
        assertEquals(Optional.empty(), SmallTalk.match("안녕하세요 카드 발급하고 싶어요"));
        assertEquals(Optional.empty(), SmallTalk.match("고마워 근데 예금 해지는 어떻게 해?"));
        assertEquals(Optional.empty(), SmallTalk.match("예금 깨서 돈 찾고 싶어"));
    }

    @Test
    void 작별_인사는_말_전체가_일치하면_알아본다() {
        assertEquals(goodbye("en"), SmallTalk.match("bye"));
        assertEquals(goodbye("en"), SmallTalk.match("That's all!"));
        assertEquals(goodbye("ko"), SmallTalk.match("안녕히 계세요"));
        assertEquals(goodbye("zh"), SmallTalk.match("再见"));
    }

    @Test
    void 말_속에_표현이_들어_있으면_종류와_언어를_짐작한다() {
        // 감사가 들어 있어도 더 필요 없다는 말이면 작별이다
        assertEquals(goodbye("en"), SmallTalk.guess("no that's all thank you"));
        assertEquals(goodbye("en"), SmallTalk.guess("No, that’s all. Thank you!"));
        assertEquals(goodbye("ko"), SmallTalk.guess("고마워요 이제 됐어요"));
        assertEquals(goodbye("ko"), SmallTalk.guess("안녕히 계세요"));
        assertEquals(thanks("ko"), SmallTalk.guess("정말 고마워요 덕분에 알았어요"));
        assertEquals(thanks("en"), SmallTalk.guess("ok thank you so much"));
        assertEquals(thanks("zh"), SmallTalk.guess("谢谢你的帮助"));
        assertEquals(greeting("en"), SmallTalk.guess("hi bot"));
        assertEquals(greeting("ko"), SmallTalk.guess("안녕 챗봇아"));
    }

    @Test
    void 짐작은_낱말_속에_우연히_들어간_글자로는_인사로_보지_않는다() {
        assertEquals(Optional.empty(), SmallTalk.guess("this is which"));
        assertEquals(Optional.empty(), SmallTalk.guess("예금 해지하고 싶어요"));
        assertEquals(Optional.empty(), SmallTalk.guess(null));
    }

    @Test
    void 비어_있거나_null이면_해당하지_않는다() {
        assertEquals(Optional.empty(), SmallTalk.match(null));
        assertEquals(Optional.empty(), SmallTalk.match("  "));
        assertEquals(Optional.empty(), SmallTalk.match("?!"));
    }
}

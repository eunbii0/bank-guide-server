package com.example.bankguide.eligibility;

import com.example.bankguide.api.ApiDtos.AltView;
import com.example.bankguide.api.ApiDtos.CustomerView;
import com.example.bankguide.api.ApiDtos.EligibilityRequest;
import com.example.bankguide.api.ApiDtos.EligibilityResponse;
import com.example.bankguide.api.ApiDtos.ProductView;
import com.example.bankguide.api.ApiDtos.TermView;
import com.example.bankguide.domain.CustomerProfile;
import com.example.bankguide.domain.CustomerProfileRepository;
import com.example.bankguide.domain.EligibilityCheck;
import com.example.bankguide.domain.EligibilityCheckRepository;
import com.example.bankguide.domain.Product;
import com.example.bankguide.domain.ProductRepository;
import com.example.bankguide.domain.ProductRuleRepository;
import com.example.bankguide.eligibility.EligibilityEngine.Customer;
import com.example.bankguide.eligibility.EligibilityEngine.Failure;
import com.example.bankguide.eligibility.EligibilityEngine.Result;
import com.example.bankguide.eligibility.EligibilityEngine.Rule;
import com.example.bankguide.eligibility.EligibilityEngine.Term;
import com.example.bankguide.service.ConversationService;
import com.example.bankguide.service.TextService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 고객 정보 확인 → 가입 자격 판단 (※ 데모용 가상 규칙).
 *
 * 1. 서버에 있는 가상 고객 정보로 확인 질문을 만든다
 * 2. 고객이 [네]라고 하면 product_rule로 상품별 가능/불가를 판단하고, 고객 언어 템플릿(ui_text)으로 문구를 만든다.
 *    불가 상품이 있으면 상담원 연결 제안 문구(offer)를 붙인다
 * 3. [아니요]라고 하면 체류정보 갱신 안내 + 상담원 연결 제안
 * 4. 판단 결과와 원래 연결 사유는 eligibility_check에 저장만 한다.
 *    티켓 생성·요약은 상담원 연결 쪽(팀원 담당)이 conversationId로 찾아 쓴다
 *
 * 판단은 규칙으로만 하고 LLM에 맡기지 않는다. 체류자격·만료일도 LLM에 보내지 않는다.
 * 그래서 채팅 기록(chat_message)에는 자리표시만 남긴다 (상담원 연결 때 채팅 기록이 LLM 요약의 재료가 되기 때문).
 */
@Service
public class EligibilityService {

    public static final String DEFAULT_CUSTOMER = "A";
    public static final List<String> CATEGORIES = List.of("CARD", "JOIN");

    /** 연결 제안을 띄운 원래 사유 (eligibility_check.handoff_reason). /api/handoff에는 CUSTOMER_REQUEST로 보낸다 */
    public static final String PROFILE_MISMATCH = "PROFILE_MISMATCH";
    public static final String NOT_ELIGIBLE = "NOT_ELIGIBLE";

    /** 채팅 기록에 남기는 자리표시 (실제 문구에는 개인정보가 들어 있다) */
    public static final String VERIFY_PLACEHOLDER = "[고객 정보 확인 질문]";
    private static final String RESULT_PLACEHOLDER = "[가입 자격 안내]";

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);
    private static final DateTimeFormatter SHOWN = DateTimeFormatter.ofPattern("yyyy.MM.dd");

    /** result_json의 한 줄. 상담원 연결 쪽이 프로그램으로 읽을 때 쓴다 */
    record ResultRow(String productCode, String intent, boolean eligible, List<String> reasonKeys,
                     List<Integer> availableTerms, String validUntil, String altProduct) {}

    private final CustomerProfileRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProductRuleRepository ruleRepository;
    private final EligibilityCheckRepository checkRepository;
    private final TextService textService;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public EligibilityService(CustomerProfileRepository customerRepository,
                              ProductRepository productRepository,
                              ProductRuleRepository ruleRepository,
                              EligibilityCheckRepository checkRepository,
                              TextService textService,
                              ConversationService conversationService,
                              ObjectMapper objectMapper,
                              Clock clock) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.ruleRepository = ruleRepository;
        this.checkRepository = checkRepository;
        this.textService = textService;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    // ---------- 고객 ----------

    public List<CustomerView> customers() {
        return customerRepository.findAllByOrderByCustomerIdAsc().stream()
                .map(c -> new CustomerView(c.getCustomerId(), c.getName(), c.getNationalityKo(), c.getVisaType(),
                        c.getStayExpiry() == null ? null : c.getStayExpiry().toString(), c.getPreferredLang()))
                .toList();
    }

    /** customerId가 없으면 데모 기본 고객(A). 모르는 id는 400 */
    public CustomerProfile customer(String customerId) {
        String id = customerId == null || customerId.isBlank() ? DEFAULT_CUSTOMER : customerId.trim();
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown customer: " + id));
    }

    /** 이 업무가 자격 판단이 필요한 상품이면 그 분류(CARD/JOIN) */
    public Optional<String> categoryOfTask(String taskCode) {
        return taskCode == null ? Optional.empty() : productRepository.findByTaskCode(taskCode).map(Product::getCategory);
    }

    /** "Nguyen님, E-9 비자, 체류기간 2027.05.31까지로 확인돼요. 맞으세요?" (고객 언어) */
    public String verifyQuestion(CustomerProfile c, String lang) {
        String key = c.getStayExpiry() == null ? "VERIFY_PROFILE_NO_EXPIRY" : "VERIFY_PROFILE";
        return fill(textService.ui(key, lang), Map.of(
                "name", c.getName(),
                "visa", c.getVisaType(),
                "expiry", shown(c.getStayExpiry())));
    }

    // ---------- 자격 판단 ----------

    public EligibilityResponse check(EligibilityRequest req) {
        if (req.conversationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversationId is required");
        }
        if (!CATEGORIES.contains(req.category())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category must be CARD or JOIN");
        }
        CustomerProfile c = customer(req.customerId());
        String lang = textService.normalize(req.lang());
        String categoryKo = textService.ui("MENU_" + req.category(), "ko");

        if (!req.confirmed()) {
            conversationService.customer(req.conversationId(), lang, "[아니요] 고객 정보가 다르다고 답함");
            String message = textService.ui("PROFILE_MISMATCH", lang);
            String offer = textService.ui("OFFER_HANDOFF", lang);
            conversationService.ai(req.conversationId(), lang, message + " " + offer);
            checkRepository.save(new EligibilityCheck(req.conversationId(), c.getCustomerId(), req.category(), false,
                    PROFILE_MISMATCH,
                    "[" + categoryKo + "] 고객 답변: 은행 정보와 다름 → 체류정보 갱신 필요 (은행 기록 체류 만료 "
                            + (c.getStayExpiry() == null ? "없음" : c.getStayExpiry()) + ")",
                    null));
            log.info("[자격 판단] customer={} category={} → MISMATCH", c.getCustomerId(), req.category());
            return new EligibilityResponse("MISMATCH", lang, message, null, List.of(), offer);
        }

        conversationService.customer(req.conversationId(), lang, "[네] 고객 정보가 맞다고 답함");
        LocalDate today = LocalDate.now(clock);
        Customer customer = toEngine(c);

        List<ProductView> products = new ArrayList<>();
        List<ResultRow> rows = new ArrayList<>();
        List<String> linesKo = new ArrayList<>();
        linesKo.add("[" + categoryKo + "] 고객 정보 확인: 맞음 (판단 기준일 " + today + ", 데모용 가상 규칙)");
        for (Product p : productRepository.findByCategoryOrderBySortAsc(req.category())) {
            Result r = judge(p.getCode(), customer, today);
            ProductView view = view(p, r, c, customer, today, lang);
            products.add(view);
            rows.add(row(p, r, view));
            linesKo.add(summaryKo(p, r, c, customer, today));
        }

        boolean anyIneligible = products.stream().anyMatch(v -> !v.eligible());
        String offer = anyIneligible
                ? textService.ui("OFFER_NOT_ELIGIBLE", lang) + " " + textService.ui("OFFER_HANDOFF", lang)
                : null;

        checkRepository.save(new EligibilityCheck(req.conversationId(), c.getCustomerId(), req.category(), true,
                anyIneligible ? NOT_ELIGIBLE : null, String.join("\n", linesKo), toJson(rows)));
        conversationService.ai(req.conversationId(), lang, offer == null ? RESULT_PLACEHOLDER : RESULT_PLACEHOLDER + " " + offer);
        log.info("[자격 판단] customer={} category={} → {}", c.getCustomerId(), req.category(),
                products.stream().map(v -> v.productCode() + (v.eligible() ? " O" : " X")).collect(Collectors.joining(", ")));

        return new EligibilityResponse("CHECKED", lang, textService.ui("ELIGIBLE_HEAD", lang),
                textService.ui("DEMO_RULE_NOTICE", lang), products, offer);
    }

    // ---------- 내부 ----------

    private Result judge(String productCode, Customer customer, LocalDate today) {
        List<Rule> rules = ruleRepository.findByProductCodeOrderByIdAsc(productCode).stream()
                .map(r -> new Rule(r.getRuleType(), r.getParam(), r.getReasonKey(), r.getAltProduct()))
                .toList();
        return EligibilityEngine.judge(productCode, rules, customer, today);
    }

    private ProductView view(Product p, Result r, CustomerProfile c, Customer customer, LocalDate today, String lang) {
        String expiry = shown(c.getStayExpiry());
        List<String> reasons = r.failures().stream().map(f -> reason(f, c, lang)).toList();
        String note = r.noteKey() == null ? null
                : fill(textService.ui(r.noteKey(), lang), Map.of("expiry", shown(r.validUntil())));
        List<TermView> terms = r.terms().stream()
                .map(t -> new TermView(t.months(), t.available(), t.available() ? null
                        : fill(textService.ui("RULE_TERM", lang), Map.of("maturity", shown(t.maturity()), "expiry", expiry))))
                .toList();
        return new ProductView(p.getCode(), p.getTaskCode(), textService.task(p.getTaskCode(), lang).getName(),
                r.eligible(), reasons, note, terms, alternative(r, customer, today, lang));
    }

    private static ResultRow row(Product p, Result r, ProductView view) {
        return new ResultRow(p.getCode(), p.getTaskCode(), r.eligible(),
                r.failures().stream().map(Failure::reasonKey).toList(),
                r.terms().stream().filter(Term::available).map(Term::months).toList(),
                r.validUntil() == null ? null : r.validUntil().toString(),
                view.alternative() == null ? null : view.alternative().productCode());
    }

    /** 추천 상품은 그 상품이 실제로 가능할 때만 보여준다 */
    private AltView alternative(Result r, Customer customer, LocalDate today, String lang) {
        if (r.altProduct() == null) {
            return null;
        }
        Optional<Product> alt = productRepository.findById(r.altProduct());
        if (alt.isEmpty() || !judge(alt.get().getCode(), customer, today).eligible()) {
            return null;
        }
        String name = textService.task(alt.get().getTaskCode(), lang).getName();
        return new AltView(alt.get().getCode(), alt.get().getTaskCode(), name,
                fill(textService.ui("ALT_SUGGEST", lang), Map.of("product", name)));
    }

    private String reason(Failure f, CustomerProfile c, String lang) {
        String param = f.rule().param() == null ? "" : f.rule().param();
        return fill(textService.ui(f.reasonKey(), lang), Map.of(
                "visa", c.getVisaType(),
                "allowed", param.replace(",", ", "),
                "months", param,
                "expiry", shown(c.getStayExpiry())));
    }

    /** 상담원용 한 줄. 예) "신용카드 발급: 불가 - 체류자격(E-9)으로는 ... → 대안 체크카드 발급" */
    private String summaryKo(Product p, Result r, CustomerProfile c, Customer customer, LocalDate today) {
        String name = textService.task(p.getTaskCode(), "ko").getName();
        if (!r.eligible()) {
            String why = r.failures().stream().map(f -> reason(f, c, "ko")).collect(Collectors.joining(" / "));
            AltView alt = alternative(r, customer, today, "ko");
            return name + ": 불가 - " + why + (alt == null ? "" : " → 대안 " + alt.name());
        }
        StringBuilder sb = new StringBuilder(name + ": 가능");
        if (r.validUntil() != null) {
            sb.append(" (카드 유효기간 ").append(r.validUntil()).append("까지)");
        }
        if (!r.terms().isEmpty()) {
            sb.append(" (선택 가능 기간 ").append(r.terms().stream().filter(Term::available)
                    .map(t -> t.months() + "개월").collect(Collectors.joining(", "))).append(")");
        }
        return sb.toString();
    }

    private String toJson(List<ResultRow> rows) {
        try {
            return objectMapper.writeValueAsString(rows);
        } catch (JsonProcessingException e) {
            log.warn("[자격 판단] 결과 JSON 변환 실패: {}", e.getMessage());
            return null;
        }
    }

    private static Customer toEngine(CustomerProfile c) {
        return new Customer(c.getVisaType(), c.getStayExpiry(), c.isHasArc(), c.isHasAccount(), c.isIncomeVerified());
    }

    private static String fill(String template, Map<String, String> values) {
        String out = template;
        for (Map.Entry<String, String> e : values.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue());
        }
        return out;
    }

    private static String shown(LocalDate date) {
        return date == null ? "-" : date.format(SHOWN);
    }
}

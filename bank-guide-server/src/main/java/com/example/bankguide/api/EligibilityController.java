package com.example.bankguide.api;

import com.example.bankguide.api.ApiDtos.CustomerView;
import com.example.bankguide.api.ApiDtos.EligibilityRequest;
import com.example.bankguide.api.ApiDtos.EligibilityResponse;
import com.example.bankguide.eligibility.EligibilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class EligibilityController {

    private final EligibilityService eligibilityService;

    public EligibilityController(EligibilityService eligibilityService) {
        this.eligibilityService = eligibilityService;
    }

    /** 데모용 가상 고객 목록 (시연 패널에서 고객 바꾸기) */
    @GetMapping("/customers")
    public List<CustomerView> customers() {
        return eligibilityService.customers();
    }

    /** 고객 정보 확인 [네]/[아니요] → 가입 자격 결과 (데모용 가상 규칙) */
    @PostMapping("/eligibility")
    public EligibilityResponse check(@RequestBody EligibilityRequest request) {
        return eligibilityService.check(request);
    }
}

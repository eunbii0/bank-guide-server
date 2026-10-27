package com.example.bankguide.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 가입 자격 판단에 쓰는 '오늘'. 기본은 실제 오늘 날짜이고,
 * 시연 결과를 고정하고 싶으면 application.yml의 demo.today(또는 환경변수 DEMO_TODAY)에 2026-10-27처럼 넣는다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${demo.today:}") String today) {
        ZoneId zone = ZoneId.of("Asia/Seoul");
        if (today == null || today.isBlank()) {
            return Clock.system(zone);
        }
        return Clock.fixed(LocalDate.parse(today.trim()).atStartOfDay(zone).toInstant(), zone);
    }
}

package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GuideTaskTextRepository extends JpaRepository<GuideTaskText, Long> {

    Optional<GuideTaskText> findByTaskCodeAndLang(String taskCode, String lang);
}

package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UiTextRepository extends JpaRepository<UiText, Long> {

    Optional<UiText> findByTextKeyAndLang(String textKey, String lang);
}

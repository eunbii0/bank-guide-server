package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EligibilityCheckRepository extends JpaRepository<EligibilityCheck, Long> {

    List<EligibilityCheck> findByConversationIdOrderByIdAsc(String conversationId);
}

package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuideStepRepository extends JpaRepository<GuideStep, Long> {

    List<GuideStep> findByTaskCodeOrderByStepNoAsc(String taskCode);
}

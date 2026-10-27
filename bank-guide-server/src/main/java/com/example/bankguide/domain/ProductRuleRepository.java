package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRuleRepository extends JpaRepository<ProductRule, Long> {

    List<ProductRule> findByProductCodeOrderByIdAsc(String productCode);
}

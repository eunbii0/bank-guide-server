package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByCategoryOrderBySortAsc(String category);

    Optional<Product> findByTaskCode(String taskCode);
}

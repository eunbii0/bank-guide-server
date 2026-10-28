package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, String> {

    List<CustomerProfile> findAllByOrderByCustomerIdAsc();
}

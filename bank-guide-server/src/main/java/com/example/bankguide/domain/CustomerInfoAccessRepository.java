package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerInfoAccessRepository extends JpaRepository<CustomerInfoAccess, Long> {

    List<CustomerInfoAccess> findByTicketNoOrderByIdAsc(String ticketNo);
}

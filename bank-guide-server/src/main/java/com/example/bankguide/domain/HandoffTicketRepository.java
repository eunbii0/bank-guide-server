package com.example.bankguide.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HandoffTicketRepository extends JpaRepository<HandoffTicket, Long> {

    List<HandoffTicket> findAllByOrderByIdDesc();

    Optional<HandoffTicket> findByTicketNo(String ticketNo);
}

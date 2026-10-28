package com.example.bankguide.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** customer_info_access 테이블: 상담원이 고객 정보를 언제 조회했는지. 쌓기만 하고 고치지 않는다. */
@Entity
@Table(name = "customer_info_access")
public class CustomerInfoAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String ticketNo;
    private String customerId;
    private String agentId;
    private String items;
    private LocalDateTime accessedAt;

    protected CustomerInfoAccess() {}

    public CustomerInfoAccess(String ticketNo, String customerId, String agentId, String items) {
        this.ticketNo = ticketNo;
        this.customerId = customerId;
        this.agentId = agentId;
        this.items = items;
        this.accessedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTicketNo() { return ticketNo; }
    public String getCustomerId() { return customerId; }
    public String getAgentId() { return agentId; }
    public String getItems() { return items; }
    public LocalDateTime getAccessedAt() { return accessedAt; }
}

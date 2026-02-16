package com.velocity.carservice.infrastructure.repository;

import com.velocity.carservice.domain.model.ProcessedPaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProcessedPaymentEventRepository extends JpaRepository<ProcessedPaymentEvent, UUID> {
    
    boolean existsByPaymentId(String paymentId);
}


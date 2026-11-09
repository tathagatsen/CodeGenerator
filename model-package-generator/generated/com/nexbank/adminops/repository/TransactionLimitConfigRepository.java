package com.nexbank.adminops.repository;

import com.nexbank.adminops.entity.TransactionLimitConfig;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionLimitConfigRepository extends JpaRepository<TransactionLimitConfig, UUID> {
}

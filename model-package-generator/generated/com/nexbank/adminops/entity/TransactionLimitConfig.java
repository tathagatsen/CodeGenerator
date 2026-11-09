package com.nexbank.adminops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.Boolean;
import java.lang.String;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "transaction_limit_configs"
)
public class TransactionLimitConfig {
  @Id
  @Column(
      name = "config_id"
  )
  private UUID configId;

  @Column(
      name = "config_name",
      length = 100
  )
  private String configName;

  @Column(
      name = "account_type",
      length = 30
  )
  private String accountType;

  @Column(
      name = "transfer_type",
      length = 30
  )
  private String transferType;

  @Column(
      name = "currency",
      length = 3
  )
  private String currency;

  @Column(
      name = "per_transaction_limit",
      precision = 19,
      scale = 2
  )
  private BigDecimal perTransactionLimit;

  @Column(
      name = "daily_limit",
      precision = 19,
      scale = 2
  )
  private BigDecimal dailyLimit;

  @Column(
      name = "effective_from"
  )
  private OffsetDateTime effectiveFrom;

  @Column(
      name = "effective_to"
  )
  private OffsetDateTime effectiveTo;

  @Column(
      name = "enabled"
  )
  private Boolean enabled;

  @Column(
      name = "created_by"
  )
  private UUID createdBy;

  @Column(
      name = "updated_by"
  )
  private UUID updatedBy;

  @Column(
      name = "created_at"
  )
  private OffsetDateTime createdAt;

  @Column(
      name = "updated_at"
  )
  private OffsetDateTime updatedAt;
}

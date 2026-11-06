package com.nexbank.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.lang.String;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "account_transactions"
)
public class AccountTransaction {
  @Column(
      name = "amount",
      nullable = false,
      precision = 15,
      scale = 2
  )
  private BigDecimal amount;

  @Column(
      name = "transaction_type",
      nullable = false,
      length = 30
  )
  private String transactionType;

  @Column(
      name = "transaction_date",
      nullable = false
  )
  private LocalDateTime transactionDate;

  @EmbeddedId
  private AccountTransactionId id;
}

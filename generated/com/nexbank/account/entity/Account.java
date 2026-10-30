package com.nexbank.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.lang.Long;
import java.lang.String;
import java.math.BigDecimal;

@Entity
@Table(
    name = "accounts"
)
public class Account {
  @Id
  @GeneratedValue(
      strategy = GenerationType.IDENTITY
  )
  @Column(
      name = "id"
  )
  private Long id;

  @Column(
      name = "account_number",
      nullable = false,
      length = 30
  )
  private String accountNumber;

  @Column(
      name = "balance",
      precision = 19,
      scale = 2
  )
  private BigDecimal balance;

  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = false
  )
  @JoinColumn(
      name = "customer_id",
      referencedColumnName = "id",
      nullable = false
  )
  private Customer customer;
}

package com.nexbank.transaction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.util.Objects;

@Embeddable
public class AccountTransactionId implements Serializable {
  private static final long serialVersionUID = 1L;

  @Column(
      name = "account_id",
      nullable = false
  )
  private Long accountId;

  @Column(
      name = "transaction_id",
      nullable = false
  )
  private Long transactionId;

  public AccountTransactionId() {
  }

  public Long getAccountId() {
    return this.accountId;
  }

  public void setAccountId(Long accountId) {
    this.accountId = accountId;
  }

  public Long getTransactionId() {
    return this.transactionId;
  }

  public void setTransactionId(Long transactionId) {
    this.transactionId = transactionId;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || getClass() != other.getClass()) {
      return false;
    }
    AccountTransactionId that = (AccountTransactionId) other;
    return Objects.deepEquals(this.accountId, that.accountId) && Objects.deepEquals(this.transactionId, that.transactionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.accountId, this.transactionId);
  }
}

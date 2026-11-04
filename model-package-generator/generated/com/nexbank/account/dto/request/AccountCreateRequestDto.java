package com.nexbank.account.dto.request;

import java.lang.Long;
import java.lang.String;
import java.math.BigDecimal;

public class AccountCreateRequestDto {
  private String accountNumber;

  private BigDecimal balance;

  private Long customerId;

  public String getAccountNumber() {
    return accountNumber;
  }

  public void setAccountNumber(String accountNumber) {
    this.accountNumber = accountNumber;
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }
}

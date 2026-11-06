package com.nexbank.transaction.repository;

import com.nexbank.transaction.entity.AccountTransaction;
import com.nexbank.transaction.entity.AccountTransactionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountTransactionRepository extends JpaRepository<AccountTransaction, AccountTransactionId> {
}

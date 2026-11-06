package com.nexbank.transaction.repository;

import com.nexbank.transaction.entity.Account;
import java.lang.Long;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
}

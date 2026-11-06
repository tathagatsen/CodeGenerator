package com.nexbank.transaction.repository;

import com.nexbank.transaction.entity.Customer;
import java.lang.Long;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
}

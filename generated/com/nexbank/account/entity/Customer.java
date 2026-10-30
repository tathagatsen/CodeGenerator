package com.nexbank.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.Long;
import java.lang.String;

@Entity
@Table(
    name = "customers"
)
public class Customer {
  @Id
  @GeneratedValue(
      strategy = GenerationType.IDENTITY
  )
  @Column(
      name = "id"
  )
  private Long id;

  @Column(
      name = "first_name",
      nullable = false,
      length = 100
  )
  private String firstName;

  @Column(
      name = "last_name",
      length = 100
  )
  private String lastName;

  @Column(
      name = "email",
      nullable = false,
      unique = true,
      length = 255
  )
  private String email;
}

package com.nexbank.adminops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "support_cases"
)
public class SupportCase {
  @Id
  @Column(
      name = "case_id"
  )
  private UUID caseId;

  @Column(
      name = "case_number",
      length = 50
  )
  private String caseNumber;

  @Column(
      name = "customer_id"
  )
  private UUID customerId;

  @Column(
      name = "created_by"
  )
  private UUID createdBy;

  @Column(
      name = "assigned_to"
  )
  private UUID assignedTo;

  @Column(
      name = "case_type",
      length = 50
  )
  private String caseType;

  @Column(
      name = "priority",
      length = 20
  )
  private String priority;

  @Column(
      name = "subject",
      length = 250
  )
  private String subject;

  @Column(
      name = "description"
  )
  private String description;

  @Column(
      name = "status",
      length = 30
  )
  private String status;

  @Column(
      name = "resolution"
  )
  private String resolution;

  @Column(
      name = "created_at"
  )
  private OffsetDateTime createdAt;

  @Column(
      name = "updated_at"
  )
  private OffsetDateTime updatedAt;

  @Column(
      name = "resolved_at"
  )
  private OffsetDateTime resolvedAt;

  @OneToMany(
      mappedBy = "supportCase",
      fetch = FetchType.LAZY
  )
  private List<SupportCaseNote> notes = new ArrayList<>();

  @OneToMany(
      mappedBy = "supportCase",
      fetch = FetchType.LAZY
  )
  private List<SupportCaseHistory> history = new ArrayList<>();

  @OneToMany(
      mappedBy = "supportCase",
      fetch = FetchType.LAZY
  )
  private List<SupportCaseAttachment> attachments = new ArrayList<>();
}

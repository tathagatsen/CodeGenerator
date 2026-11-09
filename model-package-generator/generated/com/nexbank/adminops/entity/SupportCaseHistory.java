package com.nexbank.adminops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "support_case_history"
)
public class SupportCaseHistory {
  @Id
  @Column(
      name = "history_id"
  )
  private UUID historyId;

  @Column(
      name = "old_status",
      length = 30
  )
  private String oldStatus;

  @Column(
      name = "new_status",
      length = 30
  )
  private String newStatus;

  @Column(
      name = "changed_by"
  )
  private UUID changedBy;

  @Column(
      name = "reason"
  )
  private String reason;

  @Column(
      name = "changed_at"
  )
  private OffsetDateTime changedAt;

  @ManyToOne(
      fetch = FetchType.LAZY,
      optional = true
  )
  @JoinColumn(
      name = "case_id",
      referencedColumnName = "case_id",
      nullable = true
  )
  private SupportCase supportCase;
}

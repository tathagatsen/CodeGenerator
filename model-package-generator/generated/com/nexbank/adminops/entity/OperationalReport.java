package com.nexbank.adminops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "operational_reports"
)
public class OperationalReport {
  @Id
  @Column(
      name = "report_id"
  )
  private UUID reportId;

  @Column(
      name = "report_name",
      length = 150
  )
  private String reportName;

  @Column(
      name = "report_type",
      length = 50
  )
  private String reportType;

  @Column(
      name = "parameters"
  )
  private String parameters;

  @Column(
      name = "requested_by"
  )
  private UUID requestedBy;

  @Column(
      name = "status",
      length = 30
  )
  private String status;

  @Column(
      name = "s3_object_key"
  )
  private String s3ObjectKey;

  @Column(
      name = "requested_at"
  )
  private OffsetDateTime requestedAt;

  @Column(
      name = "completed_at"
  )
  private OffsetDateTime completedAt;

  @Column(
      name = "expires_at"
  )
  private OffsetDateTime expiresAt;

  @Column(
      name = "failure_reason"
  )
  private String failureReason;
}

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
    name = "suspicious_activity_alerts"
)
public class SuspiciousActivityAlert {
  @Id
  @Column(
      name = "alert_id"
  )
  private UUID alertId;

  @Column(
      name = "customer_id"
  )
  private UUID customerId;

  @Column(
      name = "account_id"
  )
  private UUID accountId;

  @Column(
      name = "transfer_id"
  )
  private UUID transferId;

  @Column(
      name = "alert_type",
      length = 100
  )
  private String alertType;

  @Column(
      name = "severity",
      length = 20
  )
  private String severity;

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
      name = "assigned_to"
  )
  private UUID assignedTo;

  @Column(
      name = "raised_at"
  )
  private OffsetDateTime raisedAt;

  @Column(
      name = "resolved_at"
  )
  private OffsetDateTime resolvedAt;

  @Column(
      name = "resolution_notes"
  )
  private String resolutionNotes;
}

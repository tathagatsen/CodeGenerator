package com.nexbank.adminops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.lang.Integer;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "admin_ops_outbox_events"
)
public class AdminOpsOutboxEvent {
  @Id
  @Column(
      name = "event_id"
  )
  private UUID eventId;

  @Column(
      name = "aggregate_id"
  )
  private UUID aggregateId;

  @Column(
      name = "event_type",
      length = 150
  )
  private String eventType;

  @Column(
      name = "payload"
  )
  private String payload;

  @Column(
      name = "created_at"
  )
  private OffsetDateTime createdAt;

  @Column(
      name = "published_at"
  )
  private OffsetDateTime publishedAt;

  @Column(
      name = "retry_count"
  )
  private Integer retryCount;
}

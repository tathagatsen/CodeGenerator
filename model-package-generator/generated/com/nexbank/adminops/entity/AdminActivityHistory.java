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
    name = "admin_activity_history"
)
public class AdminActivityHistory {
  @Id
  @Column(
      name = "activity_id"
  )
  private UUID activityId;

  @Column(
      name = "actor_user_id"
  )
  private UUID actorUserId;

  @Column(
      name = "action",
      length = 150
  )
  private String action;

  @Column(
      name = "target_service",
      length = 100
  )
  private String targetService;

  @Column(
      name = "target_entity_type",
      length = 100
  )
  private String targetEntityType;

  @Column(
      name = "target_entity_id"
  )
  private UUID targetEntityId;

  @Column(
      name = "reason"
  )
  private String reason;

  @Column(
      name = "metadata"
  )
  private String metadata;

  @Column(
      name = "correlation_id"
  )
  private UUID correlationId;

  @Column(
      name = "created_at"
  )
  private OffsetDateTime createdAt;
}

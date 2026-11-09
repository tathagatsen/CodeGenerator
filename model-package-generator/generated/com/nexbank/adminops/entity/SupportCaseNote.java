package com.nexbank.adminops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.lang.Boolean;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "support_case_notes"
)
public class SupportCaseNote {
  @Id
  @Column(
      name = "note_id"
  )
  private UUID noteId;

  @Column(
      name = "author_user_id"
  )
  private UUID authorUserId;

  @Column(
      name = "note"
  )
  private String note;

  @Column(
      name = "is_internal"
  )
  private Boolean isInternal;

  @Column(
      name = "created_at"
  )
  private OffsetDateTime createdAt;

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

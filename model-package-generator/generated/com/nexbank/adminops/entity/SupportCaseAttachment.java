package com.nexbank.adminops.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.lang.Long;
import java.lang.String;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "support_case_attachments"
)
public class SupportCaseAttachment {
  @Id
  @Column(
      name = "attachment_id"
  )
  private UUID attachmentId;

  @Column(
      name = "s3_object_key"
  )
  private String s3ObjectKey;

  @Column(
      name = "file_name",
      length = 255
  )
  private String fileName;

  @Column(
      name = "mime_type",
      length = 100
  )
  private String mimeType;

  @Column(
      name = "file_size_bytes"
  )
  private Long fileSizeBytes;

  @Column(
      name = "uploaded_by"
  )
  private UUID uploadedBy;

  @Column(
      name = "uploaded_at"
  )
  private OffsetDateTime uploadedAt;

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

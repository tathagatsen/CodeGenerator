package com.nexbank.adminops.repository;

import com.nexbank.adminops.entity.SupportCaseAttachment;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportCaseAttachmentRepository extends JpaRepository<SupportCaseAttachment, UUID> {
}

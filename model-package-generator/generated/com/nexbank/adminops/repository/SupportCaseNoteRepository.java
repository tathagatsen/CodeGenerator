package com.nexbank.adminops.repository;

import com.nexbank.adminops.entity.SupportCaseNote;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportCaseNoteRepository extends JpaRepository<SupportCaseNote, UUID> {
}

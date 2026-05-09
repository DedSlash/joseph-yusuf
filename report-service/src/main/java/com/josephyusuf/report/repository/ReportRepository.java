package com.josephyusuf.report.repository;

import com.josephyusuf.report.entity.ReportRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<ReportRecord, UUID> {

    Page<ReportRecord> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}

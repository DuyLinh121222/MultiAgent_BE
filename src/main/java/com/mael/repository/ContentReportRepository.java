package com.mael.repository;

import com.mael.entity.ContentReport;
import com.mael.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ContentReportRepository extends JpaRepository<ContentReport, String> {

    List<ContentReport> findByStatusInOrderByCreatedAtDesc(Collection<ReportStatus> statuses);
}

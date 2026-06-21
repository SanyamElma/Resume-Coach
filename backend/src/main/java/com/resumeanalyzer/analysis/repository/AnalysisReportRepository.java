package com.resumeanalyzer.analysis.repository;

import com.resumeanalyzer.analysis.domain.AnalysisReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, UUID> {

    @Query("select r from AnalysisReport r where r.id = :id and r.resume.user.id = :userId")
    Optional<AnalysisReport> findByIdForUser(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("select r from AnalysisReport r where r.resume.user.id = :userId order by r.createdAt desc")
    Page<AnalysisReport> findAllForUser(@Param("userId") UUID userId, Pageable pageable);

    @Query("select r from AnalysisReport r where r.resume.user.id = :userId order by r.createdAt asc")
    java.util.List<AnalysisReport> findAllForUserOrderByCreatedAt(@Param("userId") UUID userId);
}

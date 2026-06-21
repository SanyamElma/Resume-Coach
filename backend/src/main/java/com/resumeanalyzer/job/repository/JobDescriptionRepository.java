package com.resumeanalyzer.job.repository;

import com.resumeanalyzer.job.domain.JobDescription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobDescriptionRepository extends JpaRepository<JobDescription, UUID> {

    Page<JobDescription> findByUserId(UUID userId, Pageable pageable);

    Optional<JobDescription> findByIdAndUserId(UUID id, UUID userId);
}

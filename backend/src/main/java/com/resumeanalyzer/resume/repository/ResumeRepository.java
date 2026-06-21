package com.resumeanalyzer.resume.repository;

import com.resumeanalyzer.resume.domain.Resume;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Page<Resume> findByUserId(UUID userId, Pageable pageable);

    Optional<Resume> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}

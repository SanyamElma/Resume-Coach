package com.resumeanalyzer.interview.repository;

import com.resumeanalyzer.interview.domain.InterviewSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, UUID> {

    Page<InterviewSession> findByUserId(UUID userId, Pageable pageable);

    long countByCreatedAtAfter(Instant since);

    java.util.List<InterviewSession> findByUserIdOrderByCreatedAtAsc(UUID userId);

    Optional<InterviewSession> findByIdAndUserId(UUID id, UUID userId);

    /** Eagerly fetches messages for the chat view to avoid N+1 selects. */
    @EntityGraph(attributePaths = "messages")
    Optional<InterviewSession> findWithMessagesByIdAndUserId(UUID id, UUID userId);
}

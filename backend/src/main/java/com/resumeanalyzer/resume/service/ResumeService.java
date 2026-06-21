package com.resumeanalyzer.resume.service;

import com.resumeanalyzer.ai.model.ParsedResume;
import com.resumeanalyzer.common.dto.PageResponse;
import com.resumeanalyzer.common.exception.BadRequestException;
import com.resumeanalyzer.common.exception.ResourceNotFoundException;
import com.resumeanalyzer.config.properties.AppProperties;
import com.resumeanalyzer.resume.domain.Resume;
import com.resumeanalyzer.resume.dto.ResumeDto;
import com.resumeanalyzer.resume.dto.ResumeSummaryDto;
import com.resumeanalyzer.resume.mapper.ResumeMapper;
import com.resumeanalyzer.resume.repository.ResumeRepository;
import com.resumeanalyzer.storage.StorageService;
import com.resumeanalyzer.user.domain.User;
import com.resumeanalyzer.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

/**
 * Application service for the resume domain: validated PDF upload, text extraction,
 * structured parsing, persistence, and ownership-scoped retrieval.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private static final Set<String> ALLOWED_TYPES = Set.of("application/pdf");

    private final ResumeRepository resumeRepository;
    private final ResumeParserService parserService;
    private final StorageService storageService;
    private final ResumeMapper resumeMapper;
    private final UserService userService;
    private final AppProperties appProperties;

    @Transactional
    public ResumeDto upload(UUID userId, MultipartFile file, String resumeName) {
        validate(file);
        User user = userService.getUser(userId);

        byte[] bytes = readBytes(file);
        String parsedText = parserService.extractText(bytes);
        ParsedResume structured = parserService.parseStructured(parsedText);
        String storageKey = storageService.store(file, "user-" + userId);

        Resume resume = Resume.builder()
                .user(user)
                .resumeName(resolveName(resumeName, file))
                .filePath(storageKey)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .parsedText(parsedText)
                .parsedData(parserService.toJson(structured))
                .version((int) (resumeRepository.countByUserId(userId) + 1))
                .build();

        resumeRepository.save(resume);
        log.info("Stored resume {} for user {}", resume.getId(), userId);
        return resumeMapper.toDto(resume);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResumeSummaryDto> list(UUID userId, Pageable pageable) {
        return PageResponse.from(resumeRepository.findByUserId(userId, pageable), resumeMapper::toSummary);
    }

    @Transactional(readOnly = true)
    public ResumeDto get(UUID userId, UUID resumeId) {
        return resumeMapper.toDto(getOwnedResume(userId, resumeId));
    }

    @Transactional
    public void delete(UUID userId, UUID resumeId) {
        Resume resume = getOwnedResume(userId, resumeId);
        storageService.delete(resume.getFilePath());
        resumeRepository.delete(resume);
    }

    /** Package-internal accessor used by the analysis module. */
    @Transactional(readOnly = true)
    public Resume getOwnedResume(UUID userId, UUID resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Resume", resumeId));
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file uploaded");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Only PDF files are accepted");
        }
        if (file.getSize() > appProperties.storage().maxFileSizeBytes()) {
            throw new BadRequestException("File exceeds the maximum allowed size of 10MB");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception e) {
            throw new BadRequestException("Failed to read uploaded file");
        }
    }

    private String resolveName(String resumeName, MultipartFile file) {
        if (resumeName != null && !resumeName.isBlank()) {
            return resumeName.trim();
        }
        return file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename();
    }
}

package com.resumeanalyzer.storage;

import com.resumeanalyzer.common.exception.BadRequestException;
import com.resumeanalyzer.config.properties.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Filesystem-backed {@link StorageService} for local/dev environments. Activated when
 * {@code app.storage.provider=local} (the default). Files are written under a configurable
 * base path with a generated, collision-free key.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path basePath;

    public LocalStorageService(AppProperties properties) {
        this.basePath = Paths.get(properties.storage().local().basePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(basePath);
            log.info("Local storage initialised at {}", basePath);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialise local storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty");
        }
        try {
            Path dir = basePath.resolve(subDirectory).normalize();
            if (!dir.startsWith(basePath)) {
                throw new BadRequestException("Invalid storage path");
            }
            Files.createDirectories(dir);
            String key = subDirectory + "/" + UUID.randomUUID() + "_" + sanitize(file.getOriginalFilename());
            Path target = basePath.resolve(key).normalize();
            file.transferTo(target);
            return key;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    @Override
    public byte[] load(String key) {
        try {
            Path target = basePath.resolve(key).normalize();
            if (!target.startsWith(basePath) || !Files.exists(target)) {
                throw new BadRequestException("File not found: " + key);
            }
            return Files.readAllBytes(target);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file: " + key, e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Path target = basePath.resolve(key).normalize();
            if (target.startsWith(basePath)) {
                Files.deleteIfExists(target);
            }
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", key, e.getMessage());
        }
    }

    private String sanitize(String filename) {
        String name = filename == null ? "file.pdf" : filename;
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}

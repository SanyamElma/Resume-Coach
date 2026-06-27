package com.resumeanalyzer.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over file storage. The local implementation backs development; the same
 * contract is satisfiable by an S3-backed implementation for production with no changes
 * to callers (see DEPLOYMENT.md for the migration plan).
 */
public interface StorageService {

    /**
     * Persists the uploaded file and returns an opaque storage key (path/object key),
     * never a public URL.
     */
    String store(MultipartFile file, String subDirectory);

    /** Loads previously stored bytes by key. */
    byte[] load(String key);

    /** Deletes a stored object. Implementations should be idempotent. */
    void delete(String key);
}

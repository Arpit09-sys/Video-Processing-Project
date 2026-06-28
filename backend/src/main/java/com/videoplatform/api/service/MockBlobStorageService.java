package com.videoplatform.api.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Mock implementation of Azure Blob Storage using local filesystem.
 * Generates HMAC-signed SAS URLs that mimic Azure SAS token behavior.
 */
@Service
@Slf4j
public class MockBlobStorageService {

    @Value("${app.storage.base-path}")
    private String basePath;

    @Value("${app.storage.base-url}")
    private String baseUrl;

    @Value("${app.storage.sas-secret}")
    private String sasSecret;

    @Value("${app.storage.sas-expiry-minutes}")
    private int sasExpiryMinutes;

    @PostConstruct
    public void init() throws IOException {
        Path storagePath = Paths.get(basePath);
        Files.createDirectories(storagePath);
        Files.createDirectories(storagePath.resolve("originals"));
        Files.createDirectories(storagePath.resolve("transcoded"));
        Files.createDirectories(storagePath.resolve("thumbnails"));
        log.info("Mock Blob Storage initialized at: {}", basePath);
    }

    /**
     * Store a file from MultipartFile upload.
     * Returns the relative blob path (e.g., originals/uuid/filename.mp4)
     */
    public String storeFile(MultipartFile file, UUID videoId) throws IOException {
        String subDir = "originals/" + videoId.toString();
        Path dirPath = Paths.get(basePath, subDir);
        Files.createDirectories(dirPath);

        String sanitizedFilename = sanitizeFilename(file.getOriginalFilename());
        Path targetPath = dirPath.resolve(sanitizedFilename);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        String blobPath = subDir + "/" + sanitizedFilename;
        log.info("Stored blob: {} ({} bytes)", blobPath, file.getSize());
        return blobPath;
    }

    /**
     * Store a file from a path (used by the worker for transcoded output).
     */
    public String storeFromPath(Path sourcePath, String targetBlobPath) throws IOException {
        Path targetPath = Paths.get(basePath, targetBlobPath);
        Files.createDirectories(targetPath.getParent());
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored blob from path: {}", targetBlobPath);
        return targetBlobPath;
    }

    /**
     * Generate a pre-signed SAS URL for a blob.
     * Mimics Azure SAS token with HMAC-SHA256 signature.
     */
    public String generateSasUrl(String blobPath) {
        if (blobPath == null || blobPath.isEmpty()) {
            return null;
        }

        long expiry = Instant.now().plusSeconds(sasExpiryMinutes * 60L).getEpochSecond();
        String stringToSign = blobPath + "\n" + expiry;
        String signature = computeHmacSha256(stringToSign);

        return String.format("%s/api/v1/blobs/%s?se=%d&sig=%s&sp=r",
                baseUrl, blobPath, expiry, signature);
    }

    /**
     * Validate a SAS token and return the file resource.
     */
    public Resource getBlob(String blobPath, long expiry, String signature) {
        // Validate expiry
        if (Instant.now().getEpochSecond() > expiry) {
            throw new SecurityException("SAS token has expired");
        }

        // Validate signature
        String stringToSign = blobPath + "\n" + expiry;
        String expectedSignature = computeHmacSha256(stringToSign);

        if (!expectedSignature.equals(signature)) {
            throw new SecurityException("Invalid SAS token signature");
        }

        Path filePath = Paths.get(basePath, blobPath);
        if (!Files.exists(filePath)) {
            throw new RuntimeException("Blob not found: " + blobPath);
        }

        return new FileSystemResource(filePath);
    }

    /**
     * Get the absolute path to a blob file.
     */
    public Path getBlobAbsolutePath(String blobPath) {
        return Paths.get(basePath, blobPath);
    }

    /**
     * Delete a blob file.
     */
    public void deleteBlob(String blobPath) throws IOException {
        Path filePath = Paths.get(basePath, blobPath);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("Deleted blob: {}", blobPath);
        }
    }

    private String computeHmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                    sasSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null) return "video.mp4";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}

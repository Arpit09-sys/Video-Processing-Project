package com.videoplatform.api.controller;

import com.videoplatform.api.dto.DashboardStats;
import com.videoplatform.api.dto.VideoResponse;
import com.videoplatform.api.dto.VideoStatusUpdate;
import com.videoplatform.api.service.MockBlobStorageService;
import com.videoplatform.api.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class VideoController {

    private final VideoService videoService;
    private final MockBlobStorageService blobStorageService;

    /**
     * Upload a video file for processing.
     * POST /api/v1/videos/upload
     */
    @PostMapping(value = "/videos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            return ResponseEntity.badRequest().build();
        }

        VideoResponse response = videoService.uploadVideo(file, title);
        return ResponseEntity.ok(response);
    }

    /**
     * Get video details by ID.
     * GET /api/v1/videos/{id}
     */
    @GetMapping("/videos/{id}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable UUID id) {
        return ResponseEntity.ok(videoService.getVideo(id));
    }

    /**
     * List all videos with pagination.
     * GET /api/v1/videos?page=0&size=20
     */
    @GetMapping("/videos")
    public ResponseEntity<Page<VideoResponse>> listVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(videoService.listVideos(page, size));
    }

    /**
     * Update video status (called by worker service).
     * PUT /api/v1/videos/status
     */
    @PutMapping("/videos/status")
    public ResponseEntity<VideoResponse> updateStatus(@RequestBody VideoStatusUpdate update) {
        return ResponseEntity.ok(videoService.updateVideoStatus(update));
    }

    /**
     * Delete a video.
     * DELETE /api/v1/videos/{id}
     */
    @DeleteMapping("/videos/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id) throws IOException {
        videoService.deleteVideo(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get dashboard statistics.
     * GET /api/v1/dashboard/stats
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<DashboardStats> getDashboardStats() {
        return ResponseEntity.ok(videoService.getDashboardStats());
    }

    /**
     * Serve blob files with SAS token validation.
     * GET /api/v1/blobs/{blobPath}?se=expiry&sig=signature&sp=r
     */
    @GetMapping("/blobs/**")
    public ResponseEntity<Resource> serveBlob(
            @RequestParam("se") long expiry,
            @RequestParam("sig") String signature,
            @RequestParam("sp") String permissions,
            jakarta.servlet.http.HttpServletRequest request) {

        // Extract blob path from request URI
        String fullPath = request.getRequestURI();
        String blobPath = fullPath.substring("/api/v1/blobs/".length());

        Resource resource = blobStorageService.getBlob(blobPath, expiry, signature);

        // Determine content type
        String contentType = "application/octet-stream";
        if (blobPath.endsWith(".mp4")) contentType = "video/mp4";
        else if (blobPath.endsWith(".webm")) contentType = "video/webm";
        else if (blobPath.endsWith(".mkv")) contentType = "video/x-matroska";
        else if (blobPath.endsWith(".jpg") || blobPath.endsWith(".jpeg")) contentType = "image/jpeg";
        else if (blobPath.endsWith(".png")) contentType = "image/png";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(resource);
    }
}

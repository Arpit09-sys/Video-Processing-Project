package com.videoplatform.api.service;

import com.videoplatform.api.dto.*;
import com.videoplatform.api.entity.Video;
import com.videoplatform.api.entity.VideoStatus;
import com.videoplatform.api.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoService {

    private final VideoRepository videoRepository;
    private final MockBlobStorageService blobStorageService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.rabbitmq.exchange}")
    private String exchange;

    @Value("${app.rabbitmq.routing-key}")
    private String routingKey;

    /**
     * Upload a video file: store in blob storage, save metadata, publish transcode job.
     */
    @Transactional
    @CacheEvict(value = "videoList", allEntries = true)
    public VideoResponse uploadVideo(MultipartFile file, String title) throws IOException {
        log.info("Uploading video: {} ({})", title, file.getOriginalFilename());

        // Create video entity
        Video video = Video.builder()
                .title(title != null ? title : file.getOriginalFilename())
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .status(VideoStatus.UPLOADED)
                .build();

        video = videoRepository.save(video);

        // Store file in mock blob storage
        String blobPath = blobStorageService.storeFile(file, video.getId());
        video.setOriginalBlobPath(blobPath);
        video.setStatus(VideoStatus.QUEUED);
        video = videoRepository.save(video);

        // Publish transcode message to RabbitMQ
        TranscodeMessage message = TranscodeMessage.builder()
                .videoId(video.getId())
                .originalBlobPath(blobPath)
                .originalFilename(file.getOriginalFilename())
                .title(video.getTitle())
                .build();

        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Published transcode job for video: {}", video.getId());

        return toResponse(video);
    }

    /**
     * Get a video by ID with signed URLs.
     */
    @Cacheable(value = "videoStatus", key = "#id")
    public VideoResponse getVideo(UUID id) {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found: " + id));
        return toResponse(video);
    }

    /**
     * List all videos with pagination.
     */
    @Cacheable(value = "videoList", key = "#page + '-' + #size")
    public Page<VideoResponse> listVideos(int page, int size) {
        return videoRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toResponse);
    }

    /**
     * Update video status (called by worker service).
     */
    @Transactional
    @CacheEvict(value = {"videoStatus", "videoList"}, allEntries = true)
    public VideoResponse updateVideoStatus(VideoStatusUpdate update) {
        Video video = videoRepository.findById(update.getVideoId())
                .orElseThrow(() -> new RuntimeException("Video not found: " + update.getVideoId()));

        if (update.getStatus() != null) {
            video.setStatus(update.getStatus());
        }
        if (update.getProgressPercent() != null) {
            video.setProgressPercent(update.getProgressPercent());
        }
        if (update.getTranscoded480pPath() != null) {
            video.setTranscoded480pPath(update.getTranscoded480pPath());
        }
        if (update.getTranscoded720pPath() != null) {
            video.setTranscoded720pPath(update.getTranscoded720pPath());
        }
        if (update.getTranscoded1080pPath() != null) {
            video.setTranscoded1080pPath(update.getTranscoded1080pPath());
        }
        if (update.getThumbnailPath() != null) {
            video.setThumbnailPath(update.getThumbnailPath());
        }
        if (update.getDuration() != null) {
            video.setDuration(update.getDuration());
        }
        if (update.getErrorMessage() != null) {
            video.setErrorMessage(update.getErrorMessage());
        }

        if (update.getStatus() == VideoStatus.PROCESSING && video.getTranscodingStartedAt() == null) {
            video.setTranscodingStartedAt(LocalDateTime.now());
        }
        if (update.getStatus() == VideoStatus.COMPLETED || update.getStatus() == VideoStatus.FAILED) {
            video.setTranscodingCompletedAt(LocalDateTime.now());
        }

        video = videoRepository.save(video);
        log.info("Updated video {} status to {}", video.getId(), video.getStatus());
        return toResponse(video);
    }

    /**
     * Delete a video and its blobs.
     */
    @Transactional
    @CacheEvict(value = {"videoStatus", "videoList"}, allEntries = true)
    public void deleteVideo(UUID id) throws IOException {
        Video video = videoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found: " + id));

        // Delete blobs
        if (video.getOriginalBlobPath() != null) blobStorageService.deleteBlob(video.getOriginalBlobPath());
        if (video.getTranscoded480pPath() != null) blobStorageService.deleteBlob(video.getTranscoded480pPath());
        if (video.getTranscoded720pPath() != null) blobStorageService.deleteBlob(video.getTranscoded720pPath());
        if (video.getTranscoded1080pPath() != null) blobStorageService.deleteBlob(video.getTranscoded1080pPath());
        if (video.getThumbnailPath() != null) blobStorageService.deleteBlob(video.getThumbnailPath());

        videoRepository.delete(video);
        log.info("Deleted video: {}", id);
    }

    /**
     * Get dashboard statistics.
     */
    public DashboardStats getDashboardStats() {
        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (VideoStatus status : VideoStatus.values()) {
            breakdown.put(status.name(), videoRepository.countByStatus(status));
        }

        return DashboardStats.builder()
                .totalVideos(videoRepository.count())
                .uploadedCount(breakdown.getOrDefault("UPLOADED", 0L))
                .queuedCount(breakdown.getOrDefault("QUEUED", 0L))
                .processingCount(breakdown.getOrDefault("PROCESSING", 0L))
                .completedCount(breakdown.getOrDefault("COMPLETED", 0L))
                .failedCount(breakdown.getOrDefault("FAILED", 0L))
                .statusBreakdown(breakdown)
                .build();
    }

    private VideoResponse toResponse(Video video) {
        return VideoResponse.builder()
                .id(video.getId())
                .title(video.getTitle())
                .originalFilename(video.getOriginalFilename())
                .contentType(video.getContentType())
                .fileSize(video.getFileSize())
                .status(video.getStatus())
                .progressPercent(video.getProgressPercent())
                .duration(video.getDuration())
                .errorMessage(video.getErrorMessage())
                .originalUrl(blobStorageService.generateSasUrl(video.getOriginalBlobPath()))
                .transcoded480pUrl(blobStorageService.generateSasUrl(video.getTranscoded480pPath()))
                .transcoded720pUrl(blobStorageService.generateSasUrl(video.getTranscoded720pPath()))
                .transcoded1080pUrl(blobStorageService.generateSasUrl(video.getTranscoded1080pPath()))
                .thumbnailUrl(blobStorageService.generateSasUrl(video.getThumbnailPath()))
                .createdAt(video.getCreatedAt())
                .updatedAt(video.getUpdatedAt())
                .transcodingStartedAt(video.getTranscodingStartedAt())
                .transcodingCompletedAt(video.getTranscodingCompletedAt())
                .build();
    }
}

package com.videoplatform.worker.service;

import com.videoplatform.worker.dto.TranscodeMessage;
import com.videoplatform.worker.dto.VideoStatusUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * RabbitMQ consumer that processes video transcoding jobs.
 * Listens to the transcoding queue, invokes FFmpeg for 480p/720p/1080p,
 * and reports status updates back to the API service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TranscodingConsumer {

    private final FFmpegService ffmpegService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.storage.base-path}")
    private String storagePath;

    @Value("${app.api.base-url}")
    private String apiBaseUrl;

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void processTranscodeJob(TranscodeMessage message) {
        log.info("Received transcode job: videoId={}, file={}", 
                message.getVideoId(), message.getOriginalFilename());

        try {
            // Update status to PROCESSING
            updateStatus(VideoStatusUpdate.builder()
                    .videoId(message.getVideoId())
                    .status("PROCESSING")
                    .progressPercent(0)
                    .build());

            Path inputPath = Paths.get(storagePath, message.getOriginalBlobPath());

            // Get duration
            double duration = ffmpegService.getVideoDuration(inputPath);

            // Generate thumbnail
            String thumbnailPath = ffmpegService.generateThumbnail(inputPath, message.getVideoId());
            updateStatus(VideoStatusUpdate.builder()
                    .videoId(message.getVideoId())
                    .status("PROCESSING")
                    .progressPercent(10)
                    .thumbnailPath(thumbnailPath)
                    .duration(duration)
                    .build());

            // Transcode to 480p
            log.info("Transcoding to 480p: {}", message.getVideoId());
            String path480p = ffmpegService.transcode(inputPath, message.getVideoId(), "480p");
            updateStatus(VideoStatusUpdate.builder()
                    .videoId(message.getVideoId())
                    .status("PROCESSING")
                    .progressPercent(35)
                    .transcoded480pPath(path480p)
                    .build());

            // Transcode to 720p
            log.info("Transcoding to 720p: {}", message.getVideoId());
            String path720p = ffmpegService.transcode(inputPath, message.getVideoId(), "720p");
            updateStatus(VideoStatusUpdate.builder()
                    .videoId(message.getVideoId())
                    .status("PROCESSING")
                    .progressPercent(65)
                    .transcoded720pPath(path720p)
                    .build());

            // Transcode to 1080p
            log.info("Transcoding to 1080p: {}", message.getVideoId());
            String path1080p = ffmpegService.transcode(inputPath, message.getVideoId(), "1080p");
            updateStatus(VideoStatusUpdate.builder()
                    .videoId(message.getVideoId())
                    .status("COMPLETED")
                    .progressPercent(100)
                    .transcoded1080pPath(path1080p)
                    .build());

            log.info("Transcode completed successfully: {}", message.getVideoId());

        } catch (Exception e) {
            log.error("Transcode failed for video {}: {}", message.getVideoId(), e.getMessage(), e);
            updateStatus(VideoStatusUpdate.builder()
                    .videoId(message.getVideoId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build());
        }
    }

    private void updateStatus(VideoStatusUpdate update) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<VideoStatusUpdate> request = new HttpEntity<>(update, headers);

            restTemplate.put(apiBaseUrl + "/api/v1/videos/status", request);
            log.debug("Status updated: videoId={}, status={}, progress={}%",
                    update.getVideoId(), update.getStatus(), update.getProgressPercent());
        } catch (Exception e) {
            log.error("Failed to update status for video {}: {}", update.getVideoId(), e.getMessage());
        }
    }
}

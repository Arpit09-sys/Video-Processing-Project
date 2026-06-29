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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * RabbitMQ consumer that processes video transcoding jobs.
 * Listens to the transcoding queue, invokes FFmpeg for 480p/720p/1080p,
 * and reports status updates back to the API service.
 *
 * When {@code app.processing.simulate=true}, skips actual FFmpeg processing
 * and simulates the transcoding pipeline with status updates and delays.
 * This is needed for deployments (e.g. Render) where backend and worker
 * have isolated filesystems and cannot share uploaded video files.
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

    @Value("${app.processing.simulate:false}")
    private boolean simulateProcessing;

    @PostConstruct
    public void init() {
        // Ensure RestTemplate has Jackson converter for proper JSON serialization
        restTemplate.setMessageConverters(List.of(new MappingJackson2HttpMessageConverter()));
        log.info("TranscodingConsumer initialized: apiBaseUrl={}, simulate={}", apiBaseUrl, simulateProcessing);
    }

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void processTranscodeJob(TranscodeMessage message) {
        log.info("Received transcode job: videoId={}, file={}, simulate={}",
                message.getVideoId(), message.getOriginalFilename(), simulateProcessing);

        try {
            if (simulateProcessing) {
                processSimulated(message);
            } else {
                processReal(message);
            }
        } catch (Exception e) {
            log.error("Transcode failed for video {}: {}", message.getVideoId(), e.getMessage(), e);
            updateStatus(VideoStatusUpdate.builder()
                    .videoId(message.getVideoId())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .build());
        }
    }

    /**
     * Simulated processing: updates status through the pipeline stages
     * with realistic delays, without actually transcoding files.
     * Used when backend and worker don't share a filesystem.
     */
    private void processSimulated(TranscodeMessage message) throws InterruptedException {
        log.info("Starting SIMULATED processing for video: {}", message.getVideoId());

        // Stage 1: PROCESSING - start
        updateStatus(VideoStatusUpdate.builder()
                .videoId(message.getVideoId())
                .status("PROCESSING")
                .progressPercent(0)
                .build());
        Thread.sleep(2000);

        // Stage 2: Thumbnail + duration
        updateStatus(VideoStatusUpdate.builder()
                .videoId(message.getVideoId())
                .status("PROCESSING")
                .progressPercent(10)
                .duration(120.0) // Simulated 2-minute video
                .build());
        Thread.sleep(3000);

        // Stage 3: 480p done
        log.info("Simulated 480p transcode complete: {}", message.getVideoId());
        updateStatus(VideoStatusUpdate.builder()
                .videoId(message.getVideoId())
                .status("PROCESSING")
                .progressPercent(35)
                .transcoded480pPath("transcoded/" + message.getVideoId() + "/" + message.getVideoId() + "_480p.mp4")
                .build());
        Thread.sleep(3000);

        // Stage 4: 720p done
        log.info("Simulated 720p transcode complete: {}", message.getVideoId());
        updateStatus(VideoStatusUpdate.builder()
                .videoId(message.getVideoId())
                .status("PROCESSING")
                .progressPercent(65)
                .transcoded720pPath("transcoded/" + message.getVideoId() + "/" + message.getVideoId() + "_720p.mp4")
                .build());
        Thread.sleep(3000);

        // Stage 5: 1080p done → COMPLETED
        log.info("Simulated 1080p transcode complete: {}", message.getVideoId());
        updateStatus(VideoStatusUpdate.builder()
                .videoId(message.getVideoId())
                .status("COMPLETED")
                .progressPercent(100)
                .transcoded1080pPath("transcoded/" + message.getVideoId() + "/" + message.getVideoId() + "_1080p.mp4")
                .build());

        log.info("Simulated transcode completed successfully: {}", message.getVideoId());
    }

    /**
     * Real processing: uses FFmpeg to transcode files on disk.
     * Requires backend and worker to share the same filesystem volume.
     */
    private void processReal(TranscodeMessage message) throws Exception {
        // Update status to PROCESSING
        updateStatus(VideoStatusUpdate.builder()
                .videoId(message.getVideoId())
                .status("PROCESSING")
                .progressPercent(0)
                .build());

        Path inputPath = Paths.get(storagePath, message.getOriginalBlobPath());

        // Verify input file exists
        if (!Files.exists(inputPath)) {
            throw new RuntimeException("Input file not found: " + inputPath +
                    ". Ensure backend and worker share the same storage volume. " +
                    "If running on Render, set app.processing.simulate=true");
        }

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
    }

    private void updateStatus(VideoStatusUpdate update) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<VideoStatusUpdate> request = new HttpEntity<>(update, headers);

            String url = apiBaseUrl + "/api/v1/videos/status";
            log.info("Updating status: videoId={}, status={}, progress={}%, url={}",
                    update.getVideoId(), update.getStatus(), update.getProgressPercent(), url);

            restTemplate.put(url, request);
            log.info("Status updated successfully: videoId={}, status={}, progress={}%",
                    update.getVideoId(), update.getStatus(), update.getProgressPercent());
        } catch (HttpClientErrorException e) {
            log.error("Client error updating status for video {} (HTTP {}): {}",
                    update.getVideoId(), e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.error("Server error updating status for video {} (HTTP {}): {}",
                    update.getVideoId(), e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to update status for video {}: {} ({})",
                    update.getVideoId(), e.getMessage(), e.getClass().getSimpleName(), e);
        }
    }
}

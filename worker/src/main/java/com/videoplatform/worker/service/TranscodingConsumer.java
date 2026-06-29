package com.videoplatform.worker.service;

import com.videoplatform.worker.dto.TranscodeMessage;
import com.videoplatform.worker.dto.VideoStatusUpdate;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class TranscodingConsumer {
    private static final Logger log = LoggerFactory.getLogger(TranscodingConsumer.class);

    private final FFmpegService ffmpegService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.storage.base-path}")
    private String storagePath;

    @Value("${app.api.base-url}")
    private String apiBaseUrl;

    @Value("${app.processing.simulate:false}")
    private boolean simulateProcessing;

    public TranscodingConsumer(FFmpegService ffmpegService) {
        this.ffmpegService = ffmpegService;
    }

    @PostConstruct
    public void init() {
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
            VideoStatusUpdate update = new VideoStatusUpdate();
            update.setVideoId(message.getVideoId());
            update.setStatus("FAILED");
            update.setErrorMessage(e.getMessage());
            updateStatus(update);
        }
    }

    private void processSimulated(TranscodeMessage message) throws InterruptedException {
        log.info("Starting SIMULATED processing for video: {}", message.getVideoId());

        VideoStatusUpdate update1 = new VideoStatusUpdate();
        update1.setVideoId(message.getVideoId());
        update1.setStatus("PROCESSING");
        update1.setProgressPercent(0);
        updateStatus(update1);
        Thread.sleep(2000);

        VideoStatusUpdate update2 = new VideoStatusUpdate();
        update2.setVideoId(message.getVideoId());
        update2.setStatus("PROCESSING");
        update2.setProgressPercent(10);
        update2.setDuration(120.0);
        updateStatus(update2);
        Thread.sleep(3000);

        log.info("Simulated 480p transcode complete: {}", message.getVideoId());
        VideoStatusUpdate update3 = new VideoStatusUpdate();
        update3.setVideoId(message.getVideoId());
        update3.setStatus("PROCESSING");
        update3.setProgressPercent(35);
        update3.setTranscoded480pPath("transcoded/" + message.getVideoId() + "/" + message.getVideoId() + "_480p.mp4");
        updateStatus(update3);
        Thread.sleep(3000);

        log.info("Simulated 720p transcode complete: {}", message.getVideoId());
        VideoStatusUpdate update4 = new VideoStatusUpdate();
        update4.setVideoId(message.getVideoId());
        update4.setStatus("PROCESSING");
        update4.setProgressPercent(65);
        update4.setTranscoded720pPath("transcoded/" + message.getVideoId() + "/" + message.getVideoId() + "_720p.mp4");
        updateStatus(update4);
        Thread.sleep(3000);

        log.info("Simulated 1080p transcode complete: {}", message.getVideoId());
        VideoStatusUpdate update5 = new VideoStatusUpdate();
        update5.setVideoId(message.getVideoId());
        update5.setStatus("COMPLETED");
        update5.setProgressPercent(100);
        update5.setTranscoded1080pPath("transcoded/" + message.getVideoId() + "/" + message.getVideoId() + "_1080p.mp4");
        updateStatus(update5);

        log.info("Simulated transcode completed successfully: {}", message.getVideoId());
    }

    private void processReal(TranscodeMessage message) throws Exception {
        VideoStatusUpdate update1 = new VideoStatusUpdate();
        update1.setVideoId(message.getVideoId());
        update1.setStatus("PROCESSING");
        update1.setProgressPercent(0);
        updateStatus(update1);

        Path inputPath = Paths.get(storagePath, message.getOriginalBlobPath());

        if (!Files.exists(inputPath)) {
            throw new RuntimeException("Input file not found: " + inputPath +
                    ". Ensure backend and worker share the same storage volume. " +
                    "If running on Render, set app.processing.simulate=true");
        }

        double duration = ffmpegService.getVideoDuration(inputPath);
        String thumbnailPath = ffmpegService.generateThumbnail(inputPath, message.getVideoId());
        
        VideoStatusUpdate update2 = new VideoStatusUpdate();
        update2.setVideoId(message.getVideoId());
        update2.setStatus("PROCESSING");
        update2.setProgressPercent(10);
        update2.setThumbnailPath(thumbnailPath);
        update2.setDuration(duration);
        updateStatus(update2);

        log.info("Transcoding to 480p: {}", message.getVideoId());
        String path480p = ffmpegService.transcode(inputPath, message.getVideoId(), "480p");
        VideoStatusUpdate update3 = new VideoStatusUpdate();
        update3.setVideoId(message.getVideoId());
        update3.setStatus("PROCESSING");
        update3.setProgressPercent(35);
        update3.setTranscoded480pPath(path480p);
        updateStatus(update3);

        log.info("Transcoding to 720p: {}", message.getVideoId());
        String path720p = ffmpegService.transcode(inputPath, message.getVideoId(), "720p");
        VideoStatusUpdate update4 = new VideoStatusUpdate();
        update4.setVideoId(message.getVideoId());
        update4.setStatus("PROCESSING");
        update4.setProgressPercent(65);
        update4.setTranscoded720pPath(path720p);
        updateStatus(update4);

        log.info("Transcoding to 1080p: {}", message.getVideoId());
        String path1080p = ffmpegService.transcode(inputPath, message.getVideoId(), "1080p");
        VideoStatusUpdate update5 = new VideoStatusUpdate();
        update5.setVideoId(message.getVideoId());
        update5.setStatus("COMPLETED");
        update5.setProgressPercent(100);
        update5.setTranscoded1080pPath(path1080p);
        updateStatus(update5);

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
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("Error updating status for video {} (HTTP {}): {}",
                    update.getVideoId(), e.getStatusCode(), e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            log.error("Failed to update status for video {}: {} ({})",
                    update.getVideoId(), e.getMessage(), e.getClass().getSimpleName(), e);
        }
    }
}

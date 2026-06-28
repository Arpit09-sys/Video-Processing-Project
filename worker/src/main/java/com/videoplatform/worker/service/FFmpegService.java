package com.videoplatform.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Wraps FFmpeg CLI for video transcoding.
 * Supports 480p, 720p, 1080p output and thumbnail generation.
 * Falls back to file-copy if FFmpeg is not available.
 */
@Service
@Slf4j
public class FFmpegService {

    @Value("${app.ffmpeg.path}")
    private String ffmpegPath;

    @Value("${app.ffmpeg.ffprobe-path}")
    private String ffprobePath;

    @Value("${app.storage.base-path}")
    private String storagePath;

    /**
     * Check if FFmpeg is available on the system.
     */
    public boolean isFFmpegAvailable() {
        try {
            Process process = new ProcessBuilder(ffmpegPath, "-version")
                    .redirectErrorStream(true)
                    .start();
            boolean completed = process.waitFor(5, TimeUnit.SECONDS);
            return completed && process.exitValue() == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get video duration in seconds using ffprobe.
     */
    public double getVideoDuration(Path inputPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    inputPath.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                process.waitFor(30, TimeUnit.SECONDS);
                if (line != null && !line.isEmpty()) {
                    return Double.parseDouble(line.trim());
                }
            }
        } catch (Exception e) {
            log.warn("Could not determine video duration: {}", e.getMessage());
        }
        return 0.0;
    }

    /**
     * Transcode a video to the specified resolution.
     * Returns the output blob path.
     */
    public String transcode(Path inputPath, UUID videoId, String resolution) throws Exception {
        String outputFilename = videoId + "_" + resolution + ".mp4";
        String outputBlobPath = "transcoded/" + videoId + "/" + outputFilename;
        Path outputPath = Paths.get(storagePath, outputBlobPath);
        Files.createDirectories(outputPath.getParent());

        if (!isFFmpegAvailable()) {
            // Fallback: copy the original file as-is
            log.warn("FFmpeg not available, falling back to file copy for {}", resolution);
            Files.copy(inputPath, outputPath);
            return outputBlobPath;
        }

        String scale = getScaleFilter(resolution);

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", inputPath.toString(),
                "-vf", scale,
                "-c:v", "libx264",
                "-preset", "fast",
                "-crf", "23",
                "-c:a", "aac",
                "-b:a", "128k",
                "-movflags", "+faststart",
                "-y",
                outputPath.toString()
        );
        pb.redirectErrorStream(true);

        log.info("Starting FFmpeg transcode: {} -> {}", inputPath, outputPath);
        Process process = pb.start();

        // Log FFmpeg output
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        boolean completed = process.waitFor(30, TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg timed out for " + resolution);
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("FFmpeg failed with exit code: " + process.exitValue());
        }

        log.info("Transcoding completed: {} ({} bytes)", outputBlobPath, Files.size(outputPath));
        return outputBlobPath;
    }

    /**
     * Generate a thumbnail from the video.
     */
    public String generateThumbnail(Path inputPath, UUID videoId) throws Exception {
        String outputFilename = videoId + "_thumb.jpg";
        String outputBlobPath = "thumbnails/" + outputFilename;
        Path outputPath = Paths.get(storagePath, outputBlobPath);
        Files.createDirectories(outputPath.getParent());

        if (!isFFmpegAvailable()) {
            log.warn("FFmpeg not available, skipping thumbnail generation");
            return null;
        }

        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", inputPath.toString(),
                "-ss", "00:00:02",
                "-vframes", "1",
                "-vf", "scale=640:360:force_original_aspect_ratio=decrease",
                "-y",
                outputPath.toString()
        );
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) { /* consume */ }
        }

        boolean completed = process.waitFor(60, TimeUnit.SECONDS);
        if (!completed || process.exitValue() != 0) {
            log.warn("Thumbnail generation failed");
            return null;
        }

        return outputBlobPath;
    }

    private String getScaleFilter(String resolution) {
        return switch (resolution) {
            case "480p" -> "scale=-2:480";
            case "720p" -> "scale=-2:720";
            case "1080p" -> "scale=-2:1080";
            default -> "scale=-2:720";
        };
    }
}

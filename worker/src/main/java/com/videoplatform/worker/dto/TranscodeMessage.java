package com.videoplatform.worker.dto;

import java.util.UUID;

public class TranscodeMessage {
    private UUID videoId;
    private String originalBlobPath;
    private String originalFilename;
    private String title;

    public TranscodeMessage() {}

    public TranscodeMessage(UUID videoId, String originalBlobPath, String originalFilename, String title) {
        this.videoId = videoId;
        this.originalBlobPath = originalBlobPath;
        this.originalFilename = originalFilename;
        this.title = title;
    }

    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }

    public String getOriginalBlobPath() { return originalBlobPath; }
    public void setOriginalBlobPath(String originalBlobPath) { this.originalBlobPath = originalBlobPath; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}

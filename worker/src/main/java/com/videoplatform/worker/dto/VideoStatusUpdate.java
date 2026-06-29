package com.videoplatform.worker.dto;

import java.util.UUID;

public class VideoStatusUpdate {
    private UUID videoId;
    private String status;
    private Integer progressPercent;
    private String transcoded480pPath;
    private String transcoded720pPath;
    private String transcoded1080pPath;
    private String thumbnailPath;
    private Double duration;
    private String errorMessage;

    public VideoStatusUpdate() {}

    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }

    public String getTranscoded480pPath() { return transcoded480pPath; }
    public void setTranscoded480pPath(String transcoded480pPath) { this.transcoded480pPath = transcoded480pPath; }

    public String getTranscoded720pPath() { return transcoded720pPath; }
    public void setTranscoded720pPath(String transcoded720pPath) { this.transcoded720pPath = transcoded720pPath; }

    public String getTranscoded1080pPath() { return transcoded1080pPath; }
    public void setTranscoded1080pPath(String transcoded1080pPath) { this.transcoded1080pPath = transcoded1080pPath; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public Double getDuration() { return duration; }
    public void setDuration(Double duration) { this.duration = duration; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

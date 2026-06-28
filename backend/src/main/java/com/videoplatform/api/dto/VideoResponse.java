package com.videoplatform.api.dto;

import com.videoplatform.api.entity.VideoStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VideoResponse implements java.io.Serializable {
    private UUID id;
    private String title;
    private String originalFilename;
    private String contentType;
    private Long fileSize;
    private VideoStatus status;
    private Integer progressPercent;
    private Double duration;
    private String errorMessage;
    private String originalUrl;
    private String transcoded480pUrl;
    private String transcoded720pUrl;
    private String transcoded1080pUrl;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime transcodingStartedAt;
    private LocalDateTime transcodingCompletedAt;
}

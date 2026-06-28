package com.videoplatform.worker.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
}

package com.videoplatform.api.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStats implements java.io.Serializable {
    private long totalVideos;
    private long uploadedCount;
    private long queuedCount;
    private long processingCount;
    private long completedCount;
    private long failedCount;
    private Map<String, Long> statusBreakdown;
}

package com.videoplatform.api.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Video implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(name = "original_filename")
    private String originalFilename;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VideoStatus status = VideoStatus.UPLOADED;

    @Column(name = "original_blob_path")
    private String originalBlobPath;

    @Column(name = "transcoded_480p_path")
    private String transcoded480pPath;

    @Column(name = "transcoded_720p_path")
    private String transcoded720pPath;

    @Column(name = "transcoded_1080p_path")
    private String transcoded1080pPath;

    @Column(name = "thumbnail_path")
    private String thumbnailPath;

    private Double duration;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "progress_percent")
    @Builder.Default
    private Integer progressPercent = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "transcoding_started_at")
    private LocalDateTime transcodingStartedAt;

    @Column(name = "transcoding_completed_at")
    private LocalDateTime transcodingCompletedAt;
}

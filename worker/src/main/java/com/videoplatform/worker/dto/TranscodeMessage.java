package com.videoplatform.worker.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscodeMessage {
    private UUID videoId;
    private String originalBlobPath;
    private String originalFilename;
    private String title;
}

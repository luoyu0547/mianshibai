package com.mianshiba.ai.model.dto.resume;

import lombok.Data;

@Data
public class ResumeImportRequest {
    private String fileName;
    private String fileType;
    private String rawText;
}

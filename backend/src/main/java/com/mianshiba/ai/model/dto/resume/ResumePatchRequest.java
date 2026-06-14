package com.mianshiba.ai.model.dto.resume;

import lombok.Data;
import java.util.Map;

@Data
public class ResumePatchRequest {
    private String sectionType;
    private String operation;
    private String reason;
    private Map<String, Object> sectionData;
}

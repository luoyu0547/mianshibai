package com.mianshiba.ai.model.vo.resume;

import lombok.Data;
import java.util.Map;

@Data
public class ResumePatchProposalVO {
    private String sectionType;
    private String operation;
    private String reason;
    private Map<String, Object> sectionData;
}

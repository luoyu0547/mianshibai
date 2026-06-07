package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ResumeImportPreviewVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String title;
    private String templateType;
    private List<SectionVO> sections;
    private List<String> warnings;
}

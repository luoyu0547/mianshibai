package com.mianshiba.ai.model.vo.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class JobKeywordVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private List<String> hardSkills;
    private List<String> softSkills;
    private List<String> bonusSkills;
    private List<String> hiddenRequirements;
}

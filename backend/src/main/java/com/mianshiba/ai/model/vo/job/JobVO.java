package com.mianshiba.ai.model.vo.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 职位 VO
 */
@Data
public class JobVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private CompanyVO company;

    private String companyName;

    private String title;

    private String sourcePlatform;

    private String sourceUrl;

    private String city;

    private String salaryRange;

    private String experienceRequirement;

    private String educationRequirement;

    private String jobDescription;

    private String jobRequirement;

    private String techStack;

    private String status;

    private JobAnalysisVO analysis;

    private JobMatchVO matchResult;

    private Boolean favorited;
}

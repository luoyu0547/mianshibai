package com.mianshiba.ai.model.dto.job;

import lombok.Data;

@Data
public class JobListQueryRequest {
    private String keyword;
    private String city;
    private String techStack;
    private String applicationStatus;
}

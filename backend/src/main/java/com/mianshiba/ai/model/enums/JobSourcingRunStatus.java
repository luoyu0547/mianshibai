package com.mianshiba.ai.model.enums;

import lombok.Getter;

/**
 * 职位采集运行状态
 */
@Getter
public enum JobSourcingRunStatus {

    RUNNING("running"),
    SUCCESS("success"),
    PARTIAL_SUCCESS("partial_success"),
    FAILED("failed"),
    AUTH_REQUIRED("auth_required");

    private final String value;

    JobSourcingRunStatus(String value) {
        this.value = value;
    }
}

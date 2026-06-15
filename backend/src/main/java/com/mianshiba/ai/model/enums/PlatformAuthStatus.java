package com.mianshiba.ai.model.enums;

import lombok.Getter;

/**
 * 平台授权会话状态
 */
@Getter
public enum PlatformAuthStatus {

    NOT_AUTHORIZED("not_authorized"),
    AUTHORIZED("authorized"),
    EXPIRED("expired"),
    AUTH_REQUIRED("auth_required"),
    ERROR("error");

    private final String value;

    PlatformAuthStatus(String value) {
        this.value = value;
    }
}

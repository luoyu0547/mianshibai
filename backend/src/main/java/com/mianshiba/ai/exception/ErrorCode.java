package com.mianshiba.ai.exception;

import lombok.Getter;

/**
 * 基础错误码
 */
@Getter
public enum ErrorCode {

    /**
     * 请求成功
     */
    SUCCESS(0, "ok"),

    /**
     * 请求参数错误
     */
    PARAMS_ERROR(40000, "请求参数错误"),

    /**
     * 未登录
     */
    NOT_LOGIN_ERROR(40100, "未登录"),

    /**
     * 无权限
     */
    NO_AUTH_ERROR(40101, "无权限"),

    /**
     * 禁止访问
     */
    FORBIDDEN_ERROR(40300, "禁止访问"),

    /**
     * 系统内部异常
     */
    SYSTEM_ERROR(50000, "系统内部异常");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}

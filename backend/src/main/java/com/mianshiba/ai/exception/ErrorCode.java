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
     * 资源不存在
     */
    NOT_FOUND_ERROR(40400, "资源不存在"),

    /**
     * 简历数量超出限制
     */
    RESUME_LIMIT_ERROR(40001, "简历数量超出限制"),

    /**
     * 模块操作异常
     */
    RESUME_SECTION_ERROR(40002, "模块操作异常"),

    /**
     * AI 服务调用失败
     */
    AI_SERVICE_ERROR(50001, "AI 服务调用失败"),

    /**
     * AI 响应解析失败
     */
    AI_RESPONSE_PARSE_ERROR(50002, "AI 响应解析失败"),

    /**
     * 面试不存在
     */
    INTERVIEW_NOT_FOUND_ERROR(40410, "面试不存在"),

    /**
     * 面试状态异常
     */
    INTERVIEW_STATUS_ERROR(40010, "面试状态异常"),

    /**
     * 面试轮次异常
     */
    INTERVIEW_TURN_ERROR(40011, "面试轮次异常"),

    /**
     * 语音服务调用失败
     */
    SPEECH_SERVICE_ERROR(50010, "语音服务调用失败"),

    /**
     * 语音识别失败
     */
    SPEECH_RECOGNITION_ERROR(50011, "语音识别失败"),

    /**
     * 语音合成失败
     */
    SPEECH_SYNTHESIS_ERROR(50012, "语音合成失败"),

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

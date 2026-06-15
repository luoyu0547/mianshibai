package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 平台授权会话实体
 * 用于持久化职位采集平台（如 Boss 直聘、实习僧）的浏览器授权状态。
 */
@Data
@TableName("platform_auth_session")
public class PlatformAuthSession implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 平台标识
     */
    private String platform;

    /**
     * 授权状态
     */
    private String status;

    /**
     * Playwright profile 路径
     */
    private String profilePath;

    /**
     * 上次校验时间
     */
    private LocalDateTime lastVerifiedAt;

    /**
     * 过期提示时间
     */
    private LocalDateTime expiresHintAt;

    /**
     * 授权失败信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}

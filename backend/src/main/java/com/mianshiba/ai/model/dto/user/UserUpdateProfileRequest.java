package com.mianshiba.ai.model.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户资料更新请求
 */
@Data
@Schema(description = "用户资料更新请求")
public class UserUpdateProfileRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 64, message = "昵称长度不能超过 64 位")
    private String userName;

    @Size(max = 512, message = "头像 URL 长度不能超过 512 位")
    private String userAvatar;

    @Size(max = 128, message = "目标岗位长度不能超过 128 位")
    private String targetPosition;

    @Size(max = 128, message = "技术方向长度不能超过 128 位")
    private String techDirection;

    @Min(value = 0, message = "工作年限不能小于 0")
    @Max(value = 60, message = "工作年限不能大于 60")
    private Integer workYears;

    @Size(max = 64, message = "城市长度不能超过 64 位")
    private String city;

    @Pattern(regexp = "^$|looking|open|not_looking", message = "求职状态不合法")
    private String jobStatus;
}

package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 简历实体
 */
@Data
@TableName(value = "resume", autoResultMap = true)
public class Resume implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String title;

    private String templateType;

    private String status;

    private String source;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> styleSettings;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}

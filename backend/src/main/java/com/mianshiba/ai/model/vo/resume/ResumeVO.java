package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 简历 VO
 */
@Data
public class ResumeVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String title;

    private String templateType;

    private String status;

    private String source;

    private Integer version;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

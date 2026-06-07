package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公司认证实体
 */
@Data
@TableName("company_certification")
public class CompanyCertification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long companyId;

    private String certificationType;

    private String status;

    private String evidenceSource;

    private String evidenceUrl;

    private String evidenceText;

    private Integer confidenceScore;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

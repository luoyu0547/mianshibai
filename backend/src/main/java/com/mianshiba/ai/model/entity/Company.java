package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 公司实体
 */
@Data
@TableName("company")
public class Company implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String normalizedName;

    private String website;

    private String industry;

    private String city;

    private String scale;

    private String description;

    private String mainBusiness;

    private String techDirection;

    private Integer isSpecializedNew;

    private Integer isLittleGiant;

    private String certificationConfidence;

    private String sourceUrl;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}

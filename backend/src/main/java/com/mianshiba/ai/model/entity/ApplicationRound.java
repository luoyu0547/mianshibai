package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("application_round")
public class ApplicationRound implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private String roundName;
    private Integer roundOrder;
    private LocalDateTime scheduledAt;
    private String result;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}

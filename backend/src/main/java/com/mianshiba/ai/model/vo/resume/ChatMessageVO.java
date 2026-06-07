package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ChatMessageVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String role;
    private String content;
    private String relatedSectionType;
    private LocalDateTime createTime;
}

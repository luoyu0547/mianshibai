package com.mianshiba.ai.model.dto.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 更新模块请求
 */
@Data
public class SectionUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Map<String, Object> sectionData;

    private Integer sortOrder;
}

package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 模块排序请求
 */
@Data
public class SectionSortRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "排序信息不能为空")
    private List<SortItem> orders;

    @Data
    public static class SortItem implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @NotNull(message = "模块 id 不能为空")
        private Long sectionId;

        @NotNull(message = "排序值不能为空")
        private Integer sortOrder;
    }
}

package com.mianshiba.ai.model.dto.job;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 职位导入请求
 */
@Data
public class JobImportRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "URL 不能为空")
    private String url;

    @NotBlank(message = "导入类型不能为空")
    @Pattern(regexp = "job|company_website|company_career_page", message = "导入类型不合法")
    private String importType;
}

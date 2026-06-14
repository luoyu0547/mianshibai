package com.mianshiba.ai.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件上传结果 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件上传结果")
public class FileUploadVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 文件访问 URL
     */
    @Schema(description = "文件访问 URL")
    private String url;

    /**
     * 原始文件名
     */
    @Schema(description = "原始文件名")
    private String originalName;

    /**
     * 文件大小
     */
    @Schema(description = "文件大小")
    private Long size;
}

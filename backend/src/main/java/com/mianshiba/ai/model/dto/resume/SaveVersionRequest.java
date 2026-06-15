package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class SaveVersionRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 256, message = "变更摘要最长 256 个字符")
    private String changeSummary;
}

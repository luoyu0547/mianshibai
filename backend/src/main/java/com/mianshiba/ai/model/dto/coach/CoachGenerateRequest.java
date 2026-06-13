package com.mianshiba.ai.model.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI 求职教练生成请求")
public class CoachGenerateRequest {

    @Size(max = 128, message = "目标岗位长度不能超过 128")
    @Schema(description = "目标岗位，空时使用用户资料目标岗位")
    private String targetPosition;

    @Size(max = 500, message = "关注点长度不能超过 500")
    @Schema(description = "本次希望教练重点关注的问题")
    private String focus;
}

package com.mianshiba.ai.service.tool;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.model.dto.resume.ResumePatchRequest;
import com.mianshiba.ai.model.vo.resume.ResumePatchProposalVO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResumePatchToolsTest {

    @Test
    void proposeResumePatchCollectsValidProposal() {
        List<ResumePatchProposalVO> proposals = new ArrayList<>();
        ResumePatchTools tools = new ResumePatchTools(proposals::add);
        ResumePatchRequest request = new ResumePatchRequest();
        request.setSectionType("summary");
        request.setOperation("replace_section");
        request.setReason("强化自我评价");
        request.setSectionData(Map.of("content", "具备扎实 Java 后端经验，关注业务结果。"));

        String result = tools.proposeResumePatch(request);

        assertThat(result).contains("等待用户确认");
        assertThat(proposals).hasSize(1);
        assertThat(proposals.get(0).getSectionType()).isEqualTo("summary");
        assertThat(proposals.get(0).getOperation()).isEqualTo("replace_section");
        assertThat(proposals.get(0).getReason()).isEqualTo("强化自我评价");
        assertThat(proposals.get(0).getSectionData()).containsEntry("content", "具备扎实 Java 后端经验，关注业务结果。");
    }

    @Test
    void proposeResumePatchMapsEducationHighlightsToActivities() {
        List<ResumePatchProposalVO> proposals = new ArrayList<>();
        ResumePatchTools tools = new ResumePatchTools(proposals::add);
        ResumePatchRequest request = new ResumePatchRequest();
        request.setSectionType("education");
        request.setOperation("replace_section");
        request.setReason("补充在校经历");
        request.setSectionData(Map.of(
                "school", "江西财经大学",
                "major", "软件工程",
                "highlights", List.of("国家励志奖学金", "课程设计：在线考试系统")));

        tools.proposeResumePatch(request);

        assertThat(proposals).hasSize(1);
        assertThat(proposals.get(0).getSectionData()).containsKey("activities");
        assertThat(proposals.get(0).getSectionData()).doesNotContainKey("highlights");
        assertThat(proposals.get(0).getSectionData().get("activities").toString())
                .contains("国家励志奖学金", "课程设计：在线考试系统");
    }

    @Test
    void proposeResumePatchRejectsUnknownSectionType() {
        List<ResumePatchProposalVO> proposals = new ArrayList<>();
        ResumePatchTools tools = new ResumePatchTools(proposals::add);
        ResumePatchRequest request = new ResumePatchRequest();
        request.setSectionType("unknown");
        request.setOperation("replace_section");
        request.setSectionData(Map.of("content", "invalid"));

        assertThatThrownBy(() -> tools.proposeResumePatch(request))
                .isInstanceOf(BusinessException.class);
        assertThat(proposals).isEmpty();
    }

    @Test
    void proposeResumePatchRejectsUnsupportedOperation() {
        List<ResumePatchProposalVO> proposals = new ArrayList<>();
        ResumePatchTools tools = new ResumePatchTools(proposals::add);
        ResumePatchRequest request = new ResumePatchRequest();
        request.setSectionType("summary");
        request.setOperation("append_item");
        request.setSectionData(Map.of("content", "invalid"));

        assertThatThrownBy(() -> tools.proposeResumePatch(request))
                .isInstanceOf(BusinessException.class);
        assertThat(proposals).isEmpty();
    }
}

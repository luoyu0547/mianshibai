package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

@Data
public class ResumeChatStreamEventVO {
    public static final String EVENT_MESSAGE = "message";
    public static final String EVENT_PROPOSAL = "resume_patch_proposal";

    private String event;
    private String content;
    private ResumePatchProposalVO proposal;

    public static ResumeChatStreamEventVO message(String content) {
        ResumeChatStreamEventVO event = new ResumeChatStreamEventVO();
        event.setEvent(EVENT_MESSAGE);
        event.setContent(content);
        return event;
    }

    public static ResumeChatStreamEventVO proposal(ResumePatchProposalVO proposal) {
        ResumeChatStreamEventVO event = new ResumeChatStreamEventVO();
        event.setEvent(EVENT_PROPOSAL);
        event.setProposal(proposal);
        return event;
    }
}

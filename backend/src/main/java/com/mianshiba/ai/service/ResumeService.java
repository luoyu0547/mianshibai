package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.ResumeUpdateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionSortRequest;
import com.mianshiba.ai.model.dto.resume.SectionUpdateRequest;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;

import java.util.List;

public interface ResumeService {

    ResumeVO createResume(String authorizationHeader, ResumeCreateRequest request);

    List<ResumeVO> listResumes(String authorizationHeader);

    ResumeDetailVO getResumeDetail(String authorizationHeader, Long resumeId);

    ResumeVO updateResume(String authorizationHeader, Long resumeId, ResumeUpdateRequest request);

    void deleteResume(String authorizationHeader, Long resumeId);

    SectionVO addSection(String authorizationHeader, Long resumeId, SectionCreateRequest request);

    SectionVO updateSection(String authorizationHeader, Long resumeId, Long sectionId, SectionUpdateRequest request);

    void deleteSection(String authorizationHeader, Long resumeId, Long sectionId);

    void sortSections(String authorizationHeader, Long resumeId, SectionSortRequest request);
}

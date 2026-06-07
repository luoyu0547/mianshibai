package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.resume.VersionVO;

import java.util.List;

public interface ResumeVersionService {

    void saveSnapshot(Long resumeId, String changeSummary);

    List<VersionVO> listVersions(Long resumeId);
}

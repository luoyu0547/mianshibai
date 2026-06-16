package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.ResumeVersionMapper;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.ResumeVersion;
import com.mianshiba.ai.model.vo.resume.VersionVO;
import com.mianshiba.ai.service.ResumeVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeVersionServiceImpl implements ResumeVersionService {

    private final ResumeVersionMapper resumeVersionMapper;
    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper resumeSectionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSnapshot(Long resumeId, String changeSummary) {
        try {
            Resume resume = resumeMapper.selectById(resumeId);
            if (resume == null) {
                log.warn("保存版本快照失败：简历 {} 不存在", resumeId);
                return;
            }

            List<ResumeSection> sections = resumeSectionMapper.selectList(
                    Wrappers.lambdaQuery(ResumeSection.class)
                            .eq(ResumeSection::getResumeId, resumeId)
                            .orderByAsc(ResumeSection::getSortOrder));

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("title", resume.getTitle());
            snapshot.put("templateType", resume.getTemplateType());

            List<Map<String, Object>> sectionList = new ArrayList<>();
            for (ResumeSection section : sections) {
                Map<String, Object> sectionMap = new HashMap<>();
                sectionMap.put("sectionType", section.getSectionType());
                sectionMap.put("sectionData", section.getSectionData());
                sectionMap.put("sortOrder", section.getSortOrder());
                sectionList.add(sectionMap);
            }
            snapshot.put("sections", sectionList);

            int nextVersion = computeNextVersion(resumeId);

            ResumeVersion version = new ResumeVersion();
            version.setResumeId(resumeId);
            version.setVersion(nextVersion);
            version.setSnapshot(snapshot);
            version.setChangeSummary(changeSummary != null ? changeSummary : "");
            version.setCreateTime(LocalDateTime.now());

            resumeVersionMapper.insert(version);
            log.info("简历 {} 版本 {} 已保存：{}", resumeId, nextVersion, changeSummary);
        } catch (Exception e) {
            log.error("保存版本快照失败：resumeId={}", resumeId, e);
        }
    }

    @Override
    public List<VersionVO> listVersions(Long resumeId) {
        List<ResumeVersion> versions = resumeVersionMapper.selectList(
                Wrappers.<ResumeVersion>query()
                        .select("id", "version", "change_summary", "create_time")
                        .eq("resume_id", resumeId)
                        .orderByDesc("version"));

        return versions.stream().map(v -> {
            VersionVO vo = new VersionVO();
            vo.setId(v.getId());
            vo.setVersion(v.getVersion());
            vo.setChangeSummary(v.getChangeSummary());
            vo.setCreateTime(v.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    private int computeNextVersion(Long resumeId) {
        List<ResumeVersion> existing = resumeVersionMapper.selectList(
                Wrappers.<ResumeVersion>query()
                        .select("version")
                        .eq("resume_id", resumeId)
                        .orderByDesc("version")
                        .last("LIMIT 1"));
        if (existing.isEmpty()) {
            return 1;
        }
        return existing.get(0).getVersion() + 1;
    }
}

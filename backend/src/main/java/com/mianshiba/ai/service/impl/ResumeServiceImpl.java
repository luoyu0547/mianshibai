package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.ResumeUpdateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionSortRequest;
import com.mianshiba.ai.model.dto.resume.SectionUpdateRequest;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {

    private static final int MAX_RESUMES_PER_USER = 10;
    private static final Set<String> VALID_SECTION_TYPES = Set.of("basic", "education", "work", "project", "skills", "summary");
    private static final Set<String> VALID_TEMPLATE_TYPES = Set.of("minimal_tech", "modern_two_col", "classic_formal");
    private static final Set<String> VALID_STATUSES = Set.of("draft", "published");

    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO createResume(String authorizationHeader, ResumeCreateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        Long count = resumeMapper.selectCount(Wrappers.lambdaQuery(Resume.class)
                .eq(Resume::getUserId, userId));
        if (count != null && count >= MAX_RESUMES_PER_USER) {
            throw new BusinessException(ErrorCode.RESUME_LIMIT_ERROR);
        }

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(request.getTitle().trim());
        resume.setTemplateType(validateOrDefault(request.getTemplateType(), VALID_TEMPLATE_TYPES, "minimal_tech"));
        resume.setStatus("draft");
        resume.setSource("scratch");
        resume.setVersion(1);
        resume.setIsDelete(0);
        resumeMapper.insert(resume);
        return toResumeVO(resume);
    }

    @Override
    public List<ResumeVO> listResumes(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        List<Resume> resumes = resumeMapper.selectList(Wrappers.lambdaQuery(Resume.class)
                .eq(Resume::getUserId, userId)
                .orderByDesc(Resume::getUpdateTime));
        return resumes.stream().map(this::toResumeVO).collect(Collectors.toList());
    }

    @Override
    public ResumeDetailVO getResumeDetail(String authorizationHeader, Long resumeId) {
        Long userId = resolveUserId(authorizationHeader);
        Resume resume = getResumeAndCheckOwner(resumeId, userId);
        List<ResumeSection> sections = resumeSectionMapper.selectList(Wrappers.lambdaQuery(ResumeSection.class)
                .eq(ResumeSection::getResumeId, resumeId)
                .orderByAsc(ResumeSection::getSortOrder));
        return toResumeDetailVO(resume, sections);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeVO updateResume(String authorizationHeader, Long resumeId, ResumeUpdateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        Resume resume = getResumeAndCheckOwner(resumeId, userId);
        setIfNotNull(resume::setTitle, request.getTitle());
        if (request.getTemplateType() != null) {
            String templateType = request.getTemplateType().trim();
            if (!VALID_TEMPLATE_TYPES.contains(templateType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "模板类型不合法");
            }
            resume.setTemplateType(templateType);
        }
        if (request.getStatus() != null) {
            String status = request.getStatus().trim();
            if (!VALID_STATUSES.contains(status)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "状态不合法");
            }
            resume.setStatus(status);
        }
        if (request.getStyleSettings() != null) {
            resume.setStyleSettings(request.getStyleSettings());
        }
        resumeMapper.updateById(resume);
        return toResumeVO(resume);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResume(String authorizationHeader, Long resumeId) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        resumeMapper.deleteById(resumeId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SectionVO addSection(String authorizationHeader, Long resumeId, SectionCreateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        String sectionType = request.getSectionType().trim();
        if (!VALID_SECTION_TYPES.contains(sectionType)) {
            throw new BusinessException(ErrorCode.RESUME_SECTION_ERROR, "模块类型不合法");
        }

        ResumeSection section = new ResumeSection();
        section.setResumeId(resumeId);
        section.setSectionType(sectionType);
        section.setSectionData(request.getSectionData());
        section.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        section.setAiGenerated(0);
        section.setIsDelete(0);
        resumeSectionMapper.insert(section);
        return toSectionVO(section);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SectionVO updateSection(String authorizationHeader, Long resumeId, Long sectionId, SectionUpdateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        ResumeSection section = resumeSectionMapper.selectById(sectionId);
        if (section == null || !section.getResumeId().equals(resumeId)) {
            throw new BusinessException(ErrorCode.RESUME_SECTION_ERROR, "模块不存在");
        }
        if (request.getSectionData() != null) {
            section.setSectionData(request.getSectionData());
        }
        if (request.getSortOrder() != null) {
            section.setSortOrder(request.getSortOrder());
        }
        resumeSectionMapper.updateById(section);
        return toSectionVO(section);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSection(String authorizationHeader, Long resumeId, Long sectionId) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        ResumeSection section = resumeSectionMapper.selectById(sectionId);
        if (section == null || !section.getResumeId().equals(resumeId)) {
            throw new BusinessException(ErrorCode.RESUME_SECTION_ERROR, "模块不存在");
        }
        resumeSectionMapper.deleteById(sectionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortSections(String authorizationHeader, Long resumeId, SectionSortRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);
        for (SectionSortRequest.SortItem item : request.getOrders()) {
            ResumeSection section = resumeSectionMapper.selectById(item.getSectionId());
            if (section == null || !section.getResumeId().equals(resumeId)) {
                throw new BusinessException(ErrorCode.RESUME_SECTION_ERROR, "模块不存在");
            }
            section.setSortOrder(item.getSortOrder());
            resumeSectionMapper.updateById(section);
        }
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        User user = userMapper.selectById(claims.userId());
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(1).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        return user.getId();
    }

    private Resume getResumeAndCheckOwner(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return resume;
    }

    private String validateOrDefault(String value, Set<String> validValues, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        String trimmed = value.trim();
        if (!validValues.contains(trimmed)) {
            return defaultValue;
        }
        return trimmed;
    }

    private void setIfNotNull(Consumer<String> setter, String value) {
        if (value != null) {
            setter.accept(value.trim());
        }
    }

    private ResumeVO toResumeVO(Resume resume) {
        ResumeVO vo = new ResumeVO();
        vo.setId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setTemplateType(resume.getTemplateType());
        vo.setStatus(resume.getStatus());
        vo.setSource(resume.getSource());
        vo.setVersion(resume.getVersion());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        vo.setStyleSettings(resume.getStyleSettings());
        return vo;
    }

    private SectionVO toSectionVO(ResumeSection section) {
        SectionVO vo = new SectionVO();
        vo.setId(section.getId());
        vo.setResumeId(section.getResumeId());
        vo.setSectionType(section.getSectionType());
        vo.setSectionData(section.getSectionData());
        vo.setSortOrder(section.getSortOrder());
        vo.setAiGenerated(section.getAiGenerated());
        vo.setCreateTime(section.getCreateTime());
        vo.setUpdateTime(section.getUpdateTime());
        return vo;
    }

    private ResumeDetailVO toResumeDetailVO(Resume resume, List<ResumeSection> sections) {
        ResumeDetailVO vo = new ResumeDetailVO();
        vo.setId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setTemplateType(resume.getTemplateType());
        vo.setStatus(resume.getStatus());
        vo.setSource(resume.getSource());
        vo.setVersion(resume.getVersion());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        vo.setStyleSettings(resume.getStyleSettings());
        vo.setSections(sections.stream().map(this::toSectionVO).collect(Collectors.toList()));
        return vo;
    }
}

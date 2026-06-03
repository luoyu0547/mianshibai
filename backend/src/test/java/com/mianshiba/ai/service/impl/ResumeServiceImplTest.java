package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionUpdateRequest;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResumeServiceImplTest {

    private static final String SECRET = "test-jwt-secret-key-must-be-at-least-32-bytes";

    @Mock
    private ResumeMapper resumeMapper;

    @Mock
    private ResumeSectionMapper resumeSectionMapper;

    @Mock
    private UserMapper userMapper;

    private JwtUtils jwtUtils;
    private ResumeServiceImpl resumeService;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(SECRET, Duration.ofHours(24));
        resumeService = new ResumeServiceImpl(resumeMapper, resumeSectionMapper, userMapper, jwtUtils);
    }

    @Test
    void createResumeSavesAndReturnsVO() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectCount(any())).thenReturn(3L);
        when(resumeMapper.insert(any(Resume.class))).thenAnswer(invocation -> {
            Resume resume = invocation.getArgument(0);
            resume.setId(2001L);
            return 1;
        });

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setTitle("我的简历");
        ResumeVO vo = resumeService.createResume(auth, request);

        assertThat(vo.getId()).isEqualTo(2001L);
        assertThat(vo.getTitle()).isEqualTo("我的简历");
        assertThat(vo.getTemplateType()).isEqualTo("minimal_tech");
        assertThat(vo.getStatus()).isEqualTo("draft");
        assertThat(vo.getSource()).isEqualTo("scratch");
        assertThat(vo.getVersion()).isEqualTo(1);
    }

    @Test
    void createResumeThrowsWhenLimitReached() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectCount(any())).thenReturn(10L);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setTitle("超限简历");

        assertThatThrownBy(() -> resumeService.createResume(auth, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.RESUME_LIMIT_ERROR.getCode());
    }

    @Test
    void listResumesReturnsOnlyOwnResumes() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        Resume r1 = new Resume();
        r1.setId(1L);
        r1.setUserId(1001L);
        r1.setTitle("简历一");
        Resume r2 = new Resume();
        r2.setId(2L);
        r2.setUserId(1001L);
        r2.setTitle("简历二");
        when(resumeMapper.selectList(any())).thenReturn(List.of(r1, r2));

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        List<ResumeVO> list = resumeService.listResumes(auth);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).getTitle()).isEqualTo("简历一");
        assertThat(list.get(1).getTitle()).isEqualTo("简历二");
    }

    @Test
    void getResumeDetailIncludesSections() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        Resume resume = ownerResume();
        when(resumeMapper.selectById(1L)).thenReturn(resume);

        ResumeSection section = new ResumeSection();
        section.setId(10L);
        section.setResumeId(1L);
        section.setSectionType("education");
        section.setSectionData(Map.of("school", "清华大学"));
        section.setSortOrder(0);
        section.setAiGenerated(0);
        when(resumeSectionMapper.selectList(any())).thenReturn(List.of(section));

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        ResumeDetailVO detail = resumeService.getResumeDetail(auth, 1L);

        assertThat(detail.getId()).isEqualTo(1L);
        assertThat(detail.getSections()).hasSize(1);
        assertThat(detail.getSections().get(0).getSectionType()).isEqualTo("education");
    }

    @Test
    void getResumeDetailThrowsWhenNotOwner() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(9999L);
        when(resumeMapper.selectById(1L)).thenReturn(resume);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");

        assertThatThrownBy(() -> resumeService.getResumeDetail(auth, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
    }

    @Test
    void addSectionInsertsAndReturnsVO() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectById(1L)).thenReturn(ownerResume());
        when(resumeSectionMapper.insert(any(ResumeSection.class))).thenAnswer(invocation -> {
            ResumeSection section = invocation.getArgument(0);
            section.setId(10L);
            return 1;
        });

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        SectionCreateRequest request = new SectionCreateRequest();
        request.setSectionType("education");
        request.setSectionData(Map.of("school", "清华大学"));
        request.setSortOrder(1);
        SectionVO vo = resumeService.addSection(auth, 1L, request);

        assertThat(vo.getId()).isEqualTo(10L);
        assertThat(vo.getSectionType()).isEqualTo("education");
        assertThat(vo.getSortOrder()).isEqualTo(1);
        verify(resumeSectionMapper).insert(any(ResumeSection.class));
    }

    @Test
    void updateSectionModifiesData() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectById(1L)).thenReturn(ownerResume());
        ResumeSection section = existingSection();
        when(resumeSectionMapper.selectById(10L)).thenReturn(section);
        when(resumeSectionMapper.updateById(any(ResumeSection.class))).thenReturn(1);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        SectionUpdateRequest request = new SectionUpdateRequest();
        request.setSectionData(Map.of("school", "北京大学"));
        SectionVO vo = resumeService.updateSection(auth, 1L, 10L, request);

        assertThat(vo.getSectionData()).containsEntry("school", "北京大学");
        verify(resumeSectionMapper).updateById(section);
    }

    @Test
    void deleteSectionRemovesFromResume() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectById(1L)).thenReturn(ownerResume());
        ResumeSection section = existingSection();
        when(resumeSectionMapper.selectById(10L)).thenReturn(section);
        when(resumeSectionMapper.deleteById(10L)).thenReturn(1);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        resumeService.deleteSection(auth, 1L, 10L);

        verify(resumeSectionMapper).deleteById(10L);
    }

    @Test
    void deleteResumeLogicallyDeletes() {
        when(userMapper.selectById(1001L)).thenReturn(normalUser());
        when(resumeMapper.selectById(1L)).thenReturn(ownerResume());
        when(resumeMapper.deleteById(1L)).thenReturn(1);

        String auth = "Bearer " + jwtUtils.generateToken(1001L, "developer_001", "user");
        resumeService.deleteResume(auth, 1L);

        verify(resumeMapper).deleteById(1L);
    }

    private User normalUser() {
        User user = new User();
        user.setId(1001L);
        user.setUserAccount("developer_001");
        user.setUserRole("user");
        user.setUserStatus(0);
        user.setIsDelete(0);
        return user;
    }

    private Resume ownerResume() {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setUserId(1001L);
        resume.setTitle("测试简历");
        resume.setTemplateType("minimal_tech");
        resume.setStatus("draft");
        resume.setSource("scratch");
        resume.setVersion(1);
        return resume;
    }

    private ResumeSection existingSection() {
        ResumeSection section = new ResumeSection();
        section.setId(10L);
        section.setResumeId(1L);
        section.setSectionType("education");
        section.setSectionData(Map.of("school", "清华大学"));
        section.setSortOrder(0);
        section.setAiGenerated(0);
        return section;
    }
}

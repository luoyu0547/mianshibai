package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.ResumeVersionMapper;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.ResumeVersion;
import com.mianshiba.ai.model.vo.resume.VersionVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ResumeVersionServiceImplTest {

    @Mock
    private ResumeVersionMapper resumeVersionMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private ResumeSectionMapper resumeSectionMapper;

    @InjectMocks
    private ResumeVersionServiceImpl service;

    @BeforeEach
    void setUp() {
        Resume resume = new Resume();
        resume.setId(1L);
        resume.setTitle("测试简历");
        resume.setTemplateType("minimal_tech");
        lenient().when(resumeMapper.selectById(1L)).thenReturn(resume);

        ResumeSection section = new ResumeSection();
        section.setSectionType("basic");
        section.setSectionData(java.util.Map.of("name", "张三"));
        section.setSortOrder(0);
        lenient().when(resumeSectionMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(section));

        lenient().when(resumeVersionMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of());
    }

    @Test
    void saveSnapshot_shouldInsertVersion() {
        service.saveSnapshot(1L, "测试变更");

        ArgumentCaptor<ResumeVersion> captor = ArgumentCaptor.forClass(ResumeVersion.class);
        verify(resumeVersionMapper).insert(captor.capture());
        ResumeVersion saved = captor.getValue();
        assertThat(saved.getResumeId()).isEqualTo(1L);
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getChangeSummary()).isEqualTo("测试变更");
        assertThat(saved.getSnapshot()).containsKey("title");
        assertThat(saved.getSnapshot()).containsKey("sections");
    }

    @Test
    void saveSnapshot_shouldIncrementVersion() {
        ResumeVersion existing = new ResumeVersion();
        existing.setVersion(3);
        when(resumeVersionMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(existing));

        service.saveSnapshot(1L, "再次变更");

        ArgumentCaptor<ResumeVersion> captor = ArgumentCaptor.forClass(ResumeVersion.class);
        verify(resumeVersionMapper).insert(captor.capture());
        assertThat(captor.getValue().getVersion()).isEqualTo(4);
    }

    @Test
    void saveSnapshot_shouldNotThrowWhenResumeNotFound() {
        when(resumeMapper.selectById(999L)).thenReturn(null);
        service.saveSnapshot(999L, "不存在");
        verify(resumeVersionMapper, never()).insert(any(ResumeVersion.class));
    }

    @Test
    void listVersions_shouldReturnVersionVOList() {
        ResumeVersion v1 = new ResumeVersion();
        v1.setId(1L);
        v1.setVersion(2);
        v1.setChangeSummary("变更A");
        v1.setCreateTime(java.time.LocalDateTime.now());

        ResumeVersion v2 = new ResumeVersion();
        v2.setId(2L);
        v2.setVersion(1);
        v2.setChangeSummary("初始创建");
        v2.setCreateTime(java.time.LocalDateTime.now().minusHours(1));

        when(resumeVersionMapper.selectList(any(QueryWrapper.class)))
                .thenReturn(List.of(v1, v2));

        List<VersionVO> result = service.listVersions(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getVersion()).isEqualTo(2);
        assertThat(result.get(1).getVersion()).isEqualTo(1);

        ArgumentCaptor<QueryWrapper<ResumeVersion>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(resumeVersionMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSelect()).doesNotContain("snapshot");
        assertThat(captor.getValue().getSqlSegment()).contains("version DESC");
    }
}

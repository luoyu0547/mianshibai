package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.ResumeUpdateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionSortRequest;
import com.mianshiba.ai.model.dto.resume.SectionUpdateRequest;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeService;
import com.mianshiba.ai.service.ResumeVersionService;
import com.mianshiba.ai.model.vo.resume.VersionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resume")
@Tag(name = "简历接口")
public class ResumeController {

    private final ResumeService resumeService;
    private final ResumeVersionService resumeVersionService;

    @PostMapping
    @Operation(summary = "创建简历")
    public BaseResponse<ResumeVO> createResume(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                               @Valid @RequestBody ResumeCreateRequest request) {
        return ResultUtils.success(resumeService.createResume(authorizationHeader, request));
    }

    @GetMapping("/list")
    @Operation(summary = "获取简历列表")
    public BaseResponse<List<ResumeVO>> listResumes(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(resumeService.listResumes(authorizationHeader));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取简历详情")
    public BaseResponse<ResumeDetailVO> getResumeDetail(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                                        @PathVariable("id") Long id) {
        return ResultUtils.success(resumeService.getResumeDetail(authorizationHeader, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新简历")
    public BaseResponse<ResumeVO> updateResume(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                               @PathVariable("id") Long id,
                                               @Valid @RequestBody ResumeUpdateRequest request) {
        ResumeVO result = resumeService.updateResume(authorizationHeader, id, request);
        resumeVersionService.saveSnapshot(id, "更新了简历基本信息");
        return ResultUtils.success(result);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除简历")
    public BaseResponse<Void> deleteResume(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                           @PathVariable("id") Long id) {
        resumeService.deleteResume(authorizationHeader, id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{resumeId}/section")
    @Operation(summary = "添加模块")
    public BaseResponse<SectionVO> addSection(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                              @PathVariable("resumeId") Long resumeId,
                                              @Valid @RequestBody SectionCreateRequest request) {
        SectionVO result = resumeService.addSection(authorizationHeader, resumeId, request);
        String sectionType = request.getSectionType() != null ? request.getSectionType() : "未知";
        resumeVersionService.saveSnapshot(resumeId, "添加了" + sectionType + "模块");
        return ResultUtils.success(result);
    }

    @PutMapping("/{resumeId}/section/{sectionId}")
    @Operation(summary = "更新模块")
    public BaseResponse<SectionVO> updateSection(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                                 @PathVariable("resumeId") Long resumeId,
                                                 @PathVariable("sectionId") Long sectionId,
                                                 @Valid @RequestBody SectionUpdateRequest request) {
        SectionVO section = resumeService.updateSection(authorizationHeader, resumeId, sectionId, request);
        String sectionType = section.getSectionType() != null ? section.getSectionType() : "未知";
        resumeVersionService.saveSnapshot(resumeId, "更新了" + sectionType + "模块");
        return ResultUtils.success(section);
    }

    @DeleteMapping("/{resumeId}/section/{sectionId}")
    @Operation(summary = "删除模块")
    public BaseResponse<Void> deleteSection(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                            @PathVariable("resumeId") Long resumeId,
                                            @PathVariable("sectionId") Long sectionId) {
        resumeService.deleteSection(authorizationHeader, resumeId, sectionId);
        resumeVersionService.saveSnapshot(resumeId, "删除了模块");
        return ResultUtils.success(null);
    }

    @PutMapping("/{resumeId}/section/sort")
    @Operation(summary = "模块排序")
    public BaseResponse<Void> sortSections(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                           @PathVariable("resumeId") Long resumeId,
                                           @Valid @RequestBody SectionSortRequest request) {
        resumeService.sortSections(authorizationHeader, resumeId, request);
        resumeVersionService.saveSnapshot(resumeId, "调整了模块排序");
        return ResultUtils.success(null);
    }

    @GetMapping("/{resumeId}/versions")
    @Operation(summary = "获取简历版本历史")
    public BaseResponse<List<VersionVO>> getVersions(@PathVariable("resumeId") Long resumeId) {
        return ResultUtils.success(resumeVersionService.listVersions(resumeId));
    }
}

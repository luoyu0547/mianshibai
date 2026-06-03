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
        return ResultUtils.success(resumeService.updateResume(authorizationHeader, id, request));
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
        return ResultUtils.success(resumeService.addSection(authorizationHeader, resumeId, request));
    }

    @PutMapping("/{resumeId}/section/{sectionId}")
    @Operation(summary = "更新模块")
    public BaseResponse<SectionVO> updateSection(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                                 @PathVariable("resumeId") Long resumeId,
                                                 @PathVariable("sectionId") Long sectionId,
                                                 @Valid @RequestBody SectionUpdateRequest request) {
        return ResultUtils.success(resumeService.updateSection(authorizationHeader, resumeId, sectionId, request));
    }

    @DeleteMapping("/{resumeId}/section/{sectionId}")
    @Operation(summary = "删除模块")
    public BaseResponse<Void> deleteSection(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                            @PathVariable("resumeId") Long resumeId,
                                            @PathVariable("sectionId") Long sectionId) {
        resumeService.deleteSection(authorizationHeader, resumeId, sectionId);
        return ResultUtils.success(null);
    }

    @PutMapping("/{resumeId}/section/sort")
    @Operation(summary = "模块排序")
    public BaseResponse<Void> sortSections(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
                                           @PathVariable("resumeId") Long resumeId,
                                           @Valid @RequestBody SectionSortRequest request) {
        resumeService.sortSections(authorizationHeader, resumeId, request);
        return ResultUtils.success(null);
    }
}

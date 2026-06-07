package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.JobAnalysis;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位分析 Mapper
 */
@Mapper
public interface JobAnalysisMapper extends BaseMapper<JobAnalysis> {
}

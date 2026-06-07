package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.JobApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 求职投递记录 Mapper
 */
@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {
}
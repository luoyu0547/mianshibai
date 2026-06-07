package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位 Mapper
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
}

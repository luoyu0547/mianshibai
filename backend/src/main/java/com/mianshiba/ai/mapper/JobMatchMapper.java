package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.JobMatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位匹配 Mapper
 */
@Mapper
public interface JobMatchMapper extends BaseMapper<JobMatch> {
}

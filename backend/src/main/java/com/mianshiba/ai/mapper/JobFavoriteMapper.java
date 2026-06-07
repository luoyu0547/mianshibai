package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.JobFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职位收藏 Mapper
 */
@Mapper
public interface JobFavoriteMapper extends BaseMapper<JobFavorite> {
}

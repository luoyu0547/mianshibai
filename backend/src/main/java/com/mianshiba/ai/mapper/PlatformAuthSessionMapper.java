package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.PlatformAuthSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * 平台授权会话 Mapper
 */
@Mapper
public interface PlatformAuthSessionMapper extends BaseMapper<PlatformAuthSession> {
}

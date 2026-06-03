package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.ResumeChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 对话记录 Mapper
 */
@Mapper
public interface ResumeChatMessageMapper extends BaseMapper<ResumeChatMessage> {
}

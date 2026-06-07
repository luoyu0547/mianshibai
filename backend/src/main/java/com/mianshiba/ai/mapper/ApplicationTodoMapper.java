package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.ApplicationTodo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 求职投递待办 Mapper
 */
@Mapper
public interface ApplicationTodoMapper extends BaseMapper<ApplicationTodo> {
}
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.Company;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公司 Mapper
 */
@Mapper
public interface CompanyMapper extends BaseMapper<Company> {
}

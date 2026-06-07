package com.mianshiba.ai.model.vo.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 公司 VO
 */
@Data
public class CompanyVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String name;

    private String normalizedName;

    private String website;

    private String industry;

    private String city;

    private String scale;

    private String description;

    private String mainBusiness;

    private String techDirection;

    private Boolean isSpecializedNew;

    private Boolean isLittleGiant;

    private String certificationConfidence;

    private List<CompanyCertificationVO> certifications;
}

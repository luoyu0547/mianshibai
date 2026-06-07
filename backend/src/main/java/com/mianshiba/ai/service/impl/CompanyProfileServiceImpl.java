package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.mapper.CompanyCertificationMapper;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.service.CompanyProfileService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyProfileServiceImpl implements CompanyProfileService {

    private final CompanyMapper companyMapper;
    private final CompanyCertificationMapper companyCertificationMapper;

    @Override
    public String resolveCertificationStatus(String evidenceSource, String evidenceText) {
        if (StringUtils.isBlank(evidenceText)) {
            return "unknown";
        }
        if ("official_list".equals(evidenceSource)) {
            return "confirmed";
        }
        if ("website".equals(evidenceSource)
                && (evidenceText.contains("专精特新") || evidenceText.contains("小巨人"))) {
            return "confirmed";
        }
        if ("news".equals(evidenceSource) || "ai_inferred".equals(evidenceSource)) {
            return "suspected";
        }
        return "unknown";
    }
}

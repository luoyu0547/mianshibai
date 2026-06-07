package com.mianshiba.ai.service;

import com.mianshiba.ai.service.impl.CompanyProfileServiceImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyProfileServiceImplTest {

    private final CompanyProfileService service = new CompanyProfileServiceImpl(null, null);

    @Test
    void officialListEvidenceIsConfirmed() {
        String status = service.resolveCertificationStatus("official_list", "入选专精特新小巨人企业名单");

        assertThat(status).isEqualTo("confirmed");
    }

    @Test
    void websiteExplicitEvidenceIsConfirmed() {
        String status = service.resolveCertificationStatus("website", "公司已获评国家级专精特新小巨人企业");

        assertThat(status).isEqualTo("confirmed");
    }

    @Test
    void newsEvidenceIsSuspected() {
        String status = service.resolveCertificationStatus("news", "据报道该公司为专精特新企业");

        assertThat(status).isEqualTo("suspected");
    }

    @Test
    void emptyEvidenceIsUnknown() {
        String status = service.resolveCertificationStatus("ai_inferred", "");

        assertThat(status).isEqualTo("unknown");
    }
}

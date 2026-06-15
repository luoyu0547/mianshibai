package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.mapper.PlatformAuthSessionMapper;
import com.mianshiba.ai.model.entity.PlatformAuthSession;
import com.mianshiba.ai.service.BrowserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaywrightBrowserSessionServiceImplTest {

    @Mock
    private PlatformAuthSessionMapper platformAuthSessionMapper;

    private JobSourcingProperties properties;
    private PlaywrightBrowserSessionServiceImpl browserSessionService;

    @BeforeEach
    void setUp() {
        properties = new JobSourcingProperties();
        browserSessionService = new PlaywrightBrowserSessionServiceImpl(platformAuthSessionMapper, properties);
    }

    @Test
    void startAuthInsertsSessionWhenNotExists() {
        when(platformAuthSessionMapper.selectOne(any())).thenReturn(null);

        BrowserSessionService.AuthStartResult result = browserSessionService.startAuth("boss");

        ArgumentCaptor<PlatformAuthSession> captor = ArgumentCaptor.forClass(PlatformAuthSession.class);
        verify(platformAuthSessionMapper).insert(captor.capture());
        PlatformAuthSession saved = captor.getValue();
        assertThat(saved.getPlatform()).isEqualTo("boss");
        assertThat(saved.getStatus()).isEqualTo("auth_required");
        assertThat(saved.getProfilePath()).endsWith("backend/browser-profiles/boss");
        assertThat(result.platform()).isEqualTo("boss");
        assertThat(result.profilePath()).endsWith("backend/browser-profiles/boss");
    }

    @Test
    void checkAuthReturnsNotAuthorizedWhenSessionNotExists() {
        when(platformAuthSessionMapper.selectOne(any())).thenReturn(null);

        BrowserSessionService.AuthCheckResult result = browserSessionService.checkAuth("boss");

        assertThat(result.platform()).isEqualTo("boss");
        assertThat(result.status()).isEqualTo("not_authorized");
        assertThat(result.message()).isEqualTo("未开始授权");
    }
}

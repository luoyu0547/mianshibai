package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.mapper.PlatformAuthSessionMapper;
import com.mianshiba.ai.model.entity.PlatformAuthSession;
import com.mianshiba.ai.model.enums.PlatformAuthStatus;
import com.mianshiba.ai.service.BrowserSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaywrightBrowserSessionServiceImplTest {

    @Mock
    private PlatformAuthSessionMapper platformAuthSessionMapper;

    private JobSourcingProperties properties;
    private TestPlaywrightBrowserSessionService browserSessionService;

    @BeforeEach
    void setUp() {
        properties = new JobSourcingProperties();
        browserSessionService = new TestPlaywrightBrowserSessionService(platformAuthSessionMapper, properties);
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
        assertThat(saved.getProfilePath()).isEqualTo(Path.of("browser-profiles", "boss").toAbsolutePath().normalize().toString().replace("\\", "/"));
        assertThat(result.platform()).isEqualTo("boss");
        assertThat(result.profilePath()).isEqualTo(saved.getProfilePath());
        assertThat(browserSessionService.openedPlatform).isEqualTo("boss");
        assertThat(browserSessionService.openedProfilePath).isEqualTo(saved.getProfilePath());
    }

    @Test
    void getProfilePathReturnsSavedSessionPath() {
        PlatformAuthSession session = new PlatformAuthSession();
        session.setPlatform("boss");
        session.setProfilePath("D:/profiles/boss");
        when(platformAuthSessionMapper.selectOne(any())).thenReturn(session);

        String profilePath = browserSessionService.getProfilePath("boss");

        assertThat(profilePath).isEqualTo("D:/profiles/boss");
    }

    @Test
    void checkAuthUpdatesSessionToAuthorizedWhenProfileIsLoggedIn() {
        PlatformAuthSession session = new PlatformAuthSession();
        session.setId(1L);
        session.setPlatform("boss");
        session.setStatus(PlatformAuthStatus.AUTH_REQUIRED.getValue());
        session.setProfilePath("backend/browser-profiles/boss");
        when(platformAuthSessionMapper.selectOne(any())).thenReturn(session);
        browserSessionService.authorized = true;

        BrowserSessionService.AuthCheckResult result = browserSessionService.checkAuth("boss");

        ArgumentCaptor<PlatformAuthSession> captor = ArgumentCaptor.forClass(PlatformAuthSession.class);
        verify(platformAuthSessionMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PlatformAuthStatus.AUTHORIZED.getValue());
        assertThat(result.status()).isEqualTo(PlatformAuthStatus.AUTHORIZED.getValue());
        assertThat(result.message()).isEqualTo("已授权");
    }

    @Test
    void checkAuthReturnsNotAuthorizedWhenSessionNotExists() {
        when(platformAuthSessionMapper.selectOne(any())).thenReturn(null);

        BrowserSessionService.AuthCheckResult result = browserSessionService.checkAuth("boss");

        assertThat(result.platform()).isEqualTo("boss");
        assertThat(result.status()).isEqualTo("not_authorized");
        assertThat(result.message()).isEqualTo("未开始授权");
    }

    private static class TestPlaywrightBrowserSessionService extends PlaywrightBrowserSessionServiceImpl {

        private String openedPlatform;
        private String openedProfilePath;
        private boolean authorized;

        private TestPlaywrightBrowserSessionService(PlatformAuthSessionMapper platformAuthSessionMapper,
                                                    JobSourcingProperties properties) {
            super(platformAuthSessionMapper, properties);
        }

        @Override
        protected void openAuthBrowser(String platform, String profilePath) {
            this.openedPlatform = platform;
            this.openedProfilePath = profilePath;
        }

        @Override
        protected boolean isProfileAuthorized(String platform, String profilePath) {
            return authorized;
        }
    }
}

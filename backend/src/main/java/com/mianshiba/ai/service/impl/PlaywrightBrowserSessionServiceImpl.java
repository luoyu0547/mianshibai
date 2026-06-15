package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.mianshiba.ai.config.JobSourcingProperties;
import com.mianshiba.ai.mapper.PlatformAuthSessionMapper;
import com.mianshiba.ai.model.entity.PlatformAuthSession;
import com.mianshiba.ai.service.BrowserSessionService;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * 基于 Playwright 的浏览器会话服务实现
 * 目前仅管理授权会话元数据，真实浏览器启动留给后续任务集成。
 */
@Service
public class PlaywrightBrowserSessionServiceImpl implements BrowserSessionService {

    private final PlatformAuthSessionMapper platformAuthSessionMapper;
    private final JobSourcingProperties properties;

    public PlaywrightBrowserSessionServiceImpl(PlatformAuthSessionMapper platformAuthSessionMapper,
                                               JobSourcingProperties properties) {
        this.platformAuthSessionMapper = platformAuthSessionMapper;
        this.properties = properties;
    }

    @Override
    public AuthStartResult startAuth(String platform) {
        String normalized = platform.toLowerCase();
        String profilePath = Path.of(properties.getBrowserProfileRoot(), normalized)
                .toString()
                .replace("\\", "/");

        QueryWrapper<PlatformAuthSession> wrapper = new QueryWrapper<>();
        wrapper.eq("platform", normalized);
        PlatformAuthSession session = platformAuthSessionMapper.selectOne(wrapper);

        if (session == null) {
            session = new PlatformAuthSession();
            session.setPlatform(normalized);
            session.setStatus("auth_required");
            session.setProfilePath(profilePath);
            session.setCreateTime(LocalDateTime.now());
            session.setUpdateTime(LocalDateTime.now());
            platformAuthSessionMapper.insert(session);
        } else {
            session.setStatus("auth_required");
            session.setProfilePath(profilePath);
            session.setUpdateTime(LocalDateTime.now());
            platformAuthSessionMapper.updateById(session);
        }

        return new AuthStartResult(normalized, profilePath);
    }

    @Override
    public AuthCheckResult checkAuth(String platform) {
        String normalized = platform.toLowerCase();
        QueryWrapper<PlatformAuthSession> wrapper = new QueryWrapper<>();
        wrapper.eq("platform", normalized);
        PlatformAuthSession session = platformAuthSessionMapper.selectOne(wrapper);

        if (session == null) {
            return new AuthCheckResult(normalized, "not_authorized", "未开始授权");
        }
        return new AuthCheckResult(normalized, session.getStatus(), session.getErrorMessage());
    }
}

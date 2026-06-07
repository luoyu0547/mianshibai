package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.InterviewTurn;
import com.mianshiba.ai.model.vo.statistics.HomeStatsVO;
import com.mianshiba.ai.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;

    @Override
    public HomeStatsVO getHomeStats(Long userId) {
        HomeStatsVO vo = new HomeStatsVO();

        vo.setCompletedInterviews(sessionMapper.selectCount(
                Wrappers.lambdaQuery(InterviewSession.class)
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "completed")));

        List<Long> sessionIds = sessionMapper.selectList(
                Wrappers.lambdaQuery(InterviewSession.class)
                        .eq(InterviewSession::getUserId, userId)
                        .select(InterviewSession::getId))
                .stream()
                .map(InterviewSession::getId)
                .toList();

        if (!sessionIds.isEmpty()) {
            vo.setTotalQuestions(turnMapper.selectCount(
                    Wrappers.lambdaQuery(InterviewTurn.class)
                            .in(InterviewTurn::getSessionId, sessionIds)
                            .eq(InterviewTurn::getTurnType, "main")));

            List<InterviewSession> sessions = sessionMapper.selectList(
                    Wrappers.lambdaQuery(InterviewSession.class)
                            .eq(InterviewSession::getUserId, userId)
                            .select(InterviewSession::getCreateTime));
            vo.setPracticeDays(sessions.stream()
                    .map(s -> s.getCreateTime().toLocalDate())
                    .distinct()
                    .count());
        }

        return vo;
    }
}

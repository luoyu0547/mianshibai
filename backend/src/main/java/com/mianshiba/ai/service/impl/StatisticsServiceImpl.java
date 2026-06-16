package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.mapper.InterviewReportMapper;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.InterviewTurnMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.InterviewTurn;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.vo.statistics.AnalyticsOverviewVO;
import com.mianshiba.ai.model.vo.statistics.HomeStatsVO;
import com.mianshiba.ai.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final InterviewSessionMapper sessionMapper;
    private final InterviewTurnMapper turnMapper;
    private final ResumeMapper resumeMapper;
    private final JobApplicationMapper applicationMapper;
    private final InterviewReportMapper interviewReportMapper;

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

    @Override
    public AnalyticsOverviewVO getAnalyticsOverview(Long userId) {
        AnalyticsOverviewVO vo = new AnalyticsOverviewVO();

        vo.setResumeCount(resumeMapper.selectCount(
                Wrappers.lambdaQuery(Resume.class)
                        .eq(Resume::getUserId, userId)));

        vo.setJobCount(applicationMapper.selectCount(
                Wrappers.lambdaQuery(JobApplication.class)
                        .eq(JobApplication::getUserId, userId)));

        vo.setInterviewCount(sessionMapper.selectCount(
                Wrappers.lambdaQuery(InterviewSession.class)
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "completed")));

        List<Long> completedSessionIds = sessionMapper.selectList(
                Wrappers.lambdaQuery(InterviewSession.class)
                        .eq(InterviewSession::getUserId, userId)
                        .eq(InterviewSession::getStatus, "completed")
                        .select(InterviewSession::getId))
                .stream().map(InterviewSession::getId).toList();

        if (!completedSessionIds.isEmpty()) {
            List<InterviewReport> reports = interviewReportMapper.selectList(
                    Wrappers.lambdaQuery(InterviewReport.class)
                            .in(InterviewReport::getSessionId, completedSessionIds));
            if (!reports.isEmpty()) {
                vo.setAverageInterviewScore((int) reports.stream()
                        .mapToInt(InterviewReport::getTotalScore)
                        .average()
                        .orElse(0));
            } else {
                vo.setAverageInterviewScore(0);
            }
        } else {
            vo.setAverageInterviewScore(0);
        }

        vo.setTopMissingSkills(List.of());

        List<String> actions = new ArrayList<>();
        if (vo.getResumeCount() == 0) {
            actions.add("上传或创建一份简历");
        }
        if (vo.getJobCount() == 0) {
            actions.add("记录一个目标投递");
        }
        if (vo.getInterviewCount() == 0) {
            actions.add("完成一次模拟面试");
        }
        if (actions.isEmpty()) {
            actions.add("继续优化简历，提升面试表现");
        }
        vo.setNextActions(actions);

        return vo;
    }
}

package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.mapper.TrainingAnswerMapper;
import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingMasteryMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.model.dto.training.TrainingMistakeQueryRequest;
import com.mianshiba.ai.model.entity.TrainingAnswer;
import com.mianshiba.ai.model.entity.TrainingAnswerReview;
import com.mianshiba.ai.model.entity.TrainingMastery;
import com.mianshiba.ai.model.entity.TrainingQuestion;
import com.mianshiba.ai.model.vo.training.TrainingMistakeVO;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingReviewServiceImplTest {

    @Mock
    JwtUtils jwtUtils;
    @Mock
    TrainingQuestionMapper questionMapper;
    @Mock
    TrainingAnswerReviewMapper reviewMapper;
    @Mock
    TrainingMasteryMapper masteryMapper;
    @Mock
    TrainingAnswerMapper answerMapper;

    @InjectMocks
    TrainingReviewServiceImpl reviewService;

    @Test
    void refreshMasteryForQuestion_upsertsTopicAndSkillTags() {
        TrainingQuestion question = new TrainingQuestion();
        question.setId(10L);
        question.setUserId(1L);
        question.setTopic("MySQL");
        question.setSkillTags(List.of("索引", "事务"));
        when(questionMapper.selectById(10L)).thenReturn(question);

        TrainingAnswerReview review = new TrainingAnswerReview();
        review.setQuestionId(10L);
        review.setTotalScore(55);
        review.setMasteryLevel("weak");
        when(reviewMapper.selectList(any())).thenReturn(List.of(review));
        when(masteryMapper.selectOne(any())).thenReturn(null);

        reviewService.refreshMasteryForQuestion(1L, 10L);

        ArgumentCaptor<TrainingMastery> captor = ArgumentCaptor.forClass(TrainingMastery.class);
        verify(masteryMapper, times(3)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(TrainingMastery::getTargetName)
                .containsExactlyInAnyOrder("MySQL", "索引", "事务");
    }

    @Test
    void listMistakes_excludesMasteredQuestionsByDefault() {
        when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
        when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "testuser", "user"));

        TrainingQuestion mastered = new TrainingQuestion();
        mastered.setId(1L);
        mastered.setUserId(1L);
        mastered.setStatus("mastered");
        mastered.setTopic("Redis");
        mastered.setTitle("Redis Q");

        TrainingQuestion weak = new TrainingQuestion();
        weak.setId(2L);
        weak.setUserId(1L);
        weak.setStatus("reviewed");
        weak.setTopic("MySQL");
        weak.setTitle("MySQL Q");

        when(questionMapper.selectList(any())).thenReturn(List.of(mastered, weak));

        TrainingAnswerReview weakReview = new TrainingAnswerReview();
        weakReview.setQuestionId(2L);
        weakReview.setTotalScore(55);
        weakReview.setMasteryLevel("weak");
        when(reviewMapper.selectOne(any())).thenReturn(weakReview);

        when(answerMapper.selectOne(any())).thenReturn(null);

        TrainingMistakeQueryRequest request = new TrainingMistakeQueryRequest();
        List<TrainingMistakeVO> mistakes = reviewService.listMistakes("Bearer test-token", request);

        assertThat(mistakes).hasSize(1);
        assertThat(mistakes.get(0).getQuestionId()).isEqualTo(2L);
    }
}

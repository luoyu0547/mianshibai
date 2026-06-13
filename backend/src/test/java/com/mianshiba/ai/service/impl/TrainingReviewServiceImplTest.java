package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingMasteryMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.model.entity.TrainingAnswerReview;
import com.mianshiba.ai.model.entity.TrainingMastery;
import com.mianshiba.ai.model.entity.TrainingQuestion;
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
}

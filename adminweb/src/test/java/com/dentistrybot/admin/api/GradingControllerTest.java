package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.SituationalAnswer;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GradingControllerTest {

    @Test
    void manualGradeStoresNullAdminIdAndNotifiesStudent() {
        ResultRepository resultRepository = mock(ResultRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        SituationalAnswer answer = new SituationalAnswer();
        answer.setId(5);
        answer.setTaskId(9);
        when(resultRepository.getSituationalAnswerById(5)).thenReturn(answer);

        var response = new GradingController(
            resultRepository,
            mock(LessonRepository.class),
            notificationService,
            null
        ).grade(5, Map.of("mode", "manual", "grade", 85, "feedback", "Yaxshi"));

        ArgumentCaptor<Integer> gradedByCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(resultRepository).gradeSituationalAnswer(eq(5), eq(85), eq("Yaxshi"), gradedByCaptor.capture());
        verify(notificationService).notifySituationalGraded(5);
        assertThat(gradedByCaptor.getValue()).isNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("passed", true);
    }

    @Test
    void manualGradeRejectsScoreOutsideRange() {
        ResultRepository resultRepository = mock(ResultRepository.class);
        when(resultRepository.getSituationalAnswerById(5)).thenReturn(new SituationalAnswer());

        var response = new GradingController(
            resultRepository,
            mock(LessonRepository.class),
            mock(NotificationService.class),
            null
        ).grade(5, Map.of("mode", "manual", "grade", 101));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(resultRepository, never()).gradeSituationalAnswer(anyInt(), anyInt(), anyString(), any());
    }
}

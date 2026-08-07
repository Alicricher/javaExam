package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.model.SituationalAnswer;
import com.dentistrybot.shared.model.SituationalTask;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.service.GradingService;
import com.dentistrybot.shared.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.util.List;
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

    @Test
    void answerNotFoundReturnsNotFound() {
        ResultRepository resultRepository = mock(ResultRepository.class);
        when(resultRepository.getSituationalAnswerById(5)).thenReturn(null);

        var response = new GradingController(
            resultRepository,
            mock(LessonRepository.class),
            mock(NotificationService.class),
            null
        ).grade(5, Map.of("mode", "ai"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void aiGradingNotConfiguredReturnsBadRequest() {
        ResultRepository resultRepository = mock(ResultRepository.class);
        SituationalAnswer answer = new SituationalAnswer();
        answer.setId(5);
        answer.setTaskId(9);
        when(resultRepository.getSituationalAnswerById(5)).thenReturn(answer);

        var response = new GradingController(
            resultRepository,
            mock(LessonRepository.class),
            mock(NotificationService.class),
            null
        ).grade(5, Map.of("mode", "ai"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("error", "AI grading not configured");
    }

    @Test
    void aiGradingWithLessonReturnsResultWithoutPersisting() {
        ResultRepository resultRepository = mock(ResultRepository.class);
        LessonRepository lessonRepository = mock(LessonRepository.class);
        NotificationService notificationService = mock(NotificationService.class);
        GradingService gradingService = mock(GradingService.class);

        SituationalAnswer answer = new SituationalAnswer();
        answer.setId(5);
        answer.setTaskId(9);
        answer.setAnswerText("Talaba javobi");
        when(resultRepository.getSituationalAnswerById(5)).thenReturn(answer);

        SituationalTask task = new SituationalTask();
        task.setId(9);
        task.setLessonId(3);
        task.setTaskText("Vaziyatli topshiriq matni");
        when(lessonRepository.getSituationalTaskById(9)).thenReturn(task);

        Lesson lesson = new Lesson();
        lesson.setId(3);
        when(lessonRepository.getLessonById(3)).thenReturn(lesson);

        GradingService.GradingResult result = new GradingService.GradingResult(
            70, "xalqaro va mahalliy fikrlar asosida", true,
            List.of(new GradingService.Criterion("Metod", 20, "to'g'ri tanlangan")),
            List.of("fixed.txt", "FOS_1.txt"), "high", false);
        when(gradingService.gradeForLesson(3, "Vaziyatli topshiriq matni", "Talaba javobi"))
            .thenReturn(result);

        var response = new GradingController(resultRepository, lessonRepository, notificationService, gradingService)
            .grade(5, Map.of("mode", "ai"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertThat(body)
            .containsEntry("grade", 70)
            .containsEntry("passed", true)
            .containsEntry("confidence", "high")
            .containsEntry("citations", List.of("fixed.txt", "FOS_1.txt"))
            .containsEntry("sourceGap", false);

        verify(gradingService).gradeForLesson(3, "Vaziyatli topshiriq matni", "Talaba javobi");
        verify(gradingService, never()).grade(anyString(), anyString());
        verifyNoInteractions(notificationService);
        verify(resultRepository, never()).gradeSituationalAnswer(anyInt(), anyInt(), anyString(), any());
    }

    @Test
    void aiGradingWithoutLessonFallsBackToPlainGrade() {
        ResultRepository resultRepository = mock(ResultRepository.class);
        LessonRepository lessonRepository = mock(LessonRepository.class);
        GradingService gradingService = mock(GradingService.class);

        SituationalAnswer answer = new SituationalAnswer();
        answer.setId(5);
        answer.setTaskId(9);
        answer.setAnswerText("javob");
        when(resultRepository.getSituationalAnswerById(5)).thenReturn(answer);
        when(lessonRepository.getSituationalTaskById(9)).thenReturn(null);

        GradingService.GradingResult empty = new GradingService.GradingResult(
            0, "", false, List.of(), List.of(), "low", true);
        when(gradingService.grade("", "javob")).thenReturn(empty);

        var response = new GradingController(resultRepository, lessonRepository, mock(NotificationService.class), gradingService)
            .grade(5, Map.of("mode", "ai"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(gradingService).grade("", "javob");
        verify(gradingService, never()).gradeForLesson(anyInt(), anyString(), anyString());
    }

    @Test
    void aiGradingFailureReturnsInternalServerError() {
        ResultRepository resultRepository = mock(ResultRepository.class);
        LessonRepository lessonRepository = mock(LessonRepository.class);
        GradingService gradingService = mock(GradingService.class);

        SituationalAnswer answer = new SituationalAnswer();
        answer.setId(5);
        answer.setTaskId(9);
        answer.setAnswerText("javob");
        when(resultRepository.getSituationalAnswerById(5)).thenReturn(answer);
        when(lessonRepository.getSituationalTaskById(9)).thenReturn(null);
        when(gradingService.grade(anyString(), anyString()))
            .thenThrow(new RuntimeException("OpenAI timeout"));

        var response = new GradingController(resultRepository, lessonRepository, mock(NotificationService.class), gradingService)
            .grade(5, Map.of("mode", "ai"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body.get("error").toString())
            .contains("AI grading failed")
            .contains("OpenAI timeout");
    }

    @Test
    void invalidModeReturnsBadRequest() {
        ResultRepository resultRepository = mock(ResultRepository.class);
        when(resultRepository.getSituationalAnswerById(5)).thenReturn(new SituationalAnswer());

        var response = new GradingController(
            resultRepository,
            mock(LessonRepository.class),
            mock(NotificationService.class),
            null
        ).grade(5, Map.of("mode", "bogus"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

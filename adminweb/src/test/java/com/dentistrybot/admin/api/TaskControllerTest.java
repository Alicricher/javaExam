package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.SituationalTask;
import com.dentistrybot.shared.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskControllerTest {

    @Test
    void createRejectsMissingFields() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new TaskController(repo).create(Map.of("lessonId", 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createSituationalTask(any());
    }

    @Test
    void createUsesDefaultTimeLimitAndNextOrder() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getNextTaskOrderNum(1)).thenReturn(3);
        when(repo.createSituationalTask(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = new TaskController(repo).create(Map.of("lessonId", 1, "taskText", "Vaziyat"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SituationalTask created = (SituationalTask) response.getBody();
        assertThat(created.getTimeLimitMinutes()).isEqualTo(30);
        assertThat(created.getOrderNum()).isEqualTo(3);
        assertThat(created.getTaskText()).isEqualTo("Vaziyat");
    }

    @Test
    void createHonorsExplicitTimeLimit() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getNextTaskOrderNum(1)).thenReturn(1);
        when(repo.createSituationalTask(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = new TaskController(repo).create(Map.of("lessonId", 1, "taskText", "Vaziyat", "timeLimitMinutes", 45));

        SituationalTask created = (SituationalTask) response.getBody();
        assertThat(created.getTimeLimitMinutes()).isEqualTo(45);
    }

    @Test
    void updateReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new TaskController(repo).update(1, Map.of("taskText", "New"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAppliesTextAndTime() {
        LessonRepository repo = mock(LessonRepository.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        when(repo.getSituationalTaskById(1)).thenReturn(t);

        var response = new TaskController(repo).update(1, Map.of("taskText", " New text ", "timeLimitMinutes", 15));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updateSituationalTaskText(1, "New text");
        verify(repo).updateSituationalTaskTime(1, 15);
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new TaskController(repo).delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repo, never()).deleteSituationalTask(anyInt());
    }

    @Test
    void deleteRenumbersRemainingTasks() {
        LessonRepository repo = mock(LessonRepository.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        t.setLessonId(7);
        when(repo.getSituationalTaskById(1)).thenReturn(t);

        var response = new TaskController(repo).delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).deleteSituationalTask(1);
        verify(repo).renumberSituationalTasks(7);
    }
}

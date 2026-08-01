package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LessonControllerTest {

    @Test
    void getReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new LessonController(repo).get(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getReturnsLessonWhenFound() {
        LessonRepository repo = mock(LessonRepository.class);
        Lesson l = new Lesson();
        l.setId(1);
        when(repo.getLessonById(1)).thenReturn(l);

        var response = new LessonController(repo).get(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(l);
    }

    @Test
    void createRejectsMissingFields() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new LessonController(repo).create(Map.of("unitId", 1));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createLesson(any());
    }

    @Test
    void createAssignsNextLessonNumber() {
        LessonRepository repo = mock(LessonRepository.class);
        Lesson existing = new Lesson();
        existing.setLessonNumber(3);
        when(repo.getLessonsByUnitId(1)).thenReturn(List.of(existing));
        when(repo.createLesson(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = new LessonController(repo).create(Map.of("unitId", 1, "titleUz", "Dars"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Lesson created = (Lesson) response.getBody();
        assertThat(created.getLessonNumber()).isEqualTo(4);
        assertThat(created.getUnitId()).isEqualTo(1);
    }

    @Test
    void createStartsAtOneForEmptyUnit() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getLessonsByUnitId(1)).thenReturn(List.of());
        when(repo.createLesson(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = new LessonController(repo).create(Map.of("unitId", 1, "titleUz", "Dars"));

        Lesson created = (Lesson) response.getBody();
        assertThat(created.getLessonNumber()).isEqualTo(1);
    }

    @Test
    void updateReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new LessonController(repo).update(1, Map.of("titleUz", "New"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateRejectsBlankTitle() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getLessonById(1)).thenReturn(new Lesson());

        var response = new LessonController(repo).update(1, Map.of("titleUz", " "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).updateLesson(any());
    }

    @Test
    void updateSucceeds() {
        LessonRepository repo = mock(LessonRepository.class);
        Lesson l = new Lesson();
        l.setId(1);
        when(repo.getLessonById(1)).thenReturn(l);

        var response = new LessonController(repo).update(1, Map.of("titleUz", "New"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(l.getTitleUz()).isEqualTo("New");
        verify(repo).updateLesson(l);
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new LessonController(repo).delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repo, never()).deleteLesson(anyInt());
    }

    @Test
    void deleteRenumbersRemainingLessons() {
        LessonRepository repo = mock(LessonRepository.class);
        Lesson l = new Lesson();
        l.setId(1);
        l.setUnitId(5);
        when(repo.getLessonById(1)).thenReturn(l);

        var response = new LessonController(repo).delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).deleteLesson(1);
        verify(repo).renumberLessons(5);
    }

    @Test
    void theoryDelegatesToRepository() {
        LessonRepository repo = mock(LessonRepository.class);

        new LessonController(repo).theory(1);

        verify(repo).getTheoryMaterialsByLessonId(1);
    }

    @Test
    void tasksDelegatesToRepository() {
        LessonRepository repo = mock(LessonRepository.class);

        new LessonController(repo).tasks(1);

        verify(repo).getSituationalTasksByLessonId(1);
    }
}

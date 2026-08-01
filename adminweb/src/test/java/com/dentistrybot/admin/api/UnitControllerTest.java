package com.dentistrybot.admin.api;

import com.dentistrybot.shared.model.Unit;
import com.dentistrybot.shared.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UnitControllerTest {

    @Test
    void listReturnsAllUnits() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getAllUnits()).thenReturn(List.of(new Unit()));

        var response = new UnitController(repo).list();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) response.getBody()).hasSize(1);
    }

    @Test
    void getReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getUnitById(1)).thenReturn(null);

        var response = new UnitController(repo).get(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getReturnsUnitWhenFound() {
        LessonRepository repo = mock(LessonRepository.class);
        Unit u = new Unit();
        u.setId(1);
        when(repo.getUnitById(1)).thenReturn(u);

        var response = new UnitController(repo).get(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(u);
    }

    @Test
    void createRejectsBlankFields() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new UnitController(repo).create(Map.of("name", " ", "titleUz", "Foo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createUnit(any());
    }

    @Test
    void createRejectsNameTooLong() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new UnitController(repo).create(Map.of("name", "A".repeat(11), "titleUz", "Foo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createUnit(any());
    }

    @Test
    void createRejectsDuplicateName() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.checkUnitNameExists("f1", -1)).thenReturn(true);

        var response = new UnitController(repo).create(Map.of("name", "f1", "titleUz", "Foo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createUnit(any());
    }

    @Test
    void createNormalizesNameAndTitle() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.checkUnitNameExists("f1", -1)).thenReturn(false);
        when(repo.createUnit(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = new UnitController(repo).create(Map.of("name", " f1 ", "titleUz", " Foo "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Unit created = (Unit) response.getBody();
        assertThat(created.getName()).isEqualTo("F1");
        assertThat(created.getTitleUz()).isEqualTo("Foo");
    }

    @Test
    void updateReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getUnitById(1)).thenReturn(null);

        var response = new UnitController(repo).update(1, Map.of("name", "F1", "titleUz", "Foo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateRejectsDuplicateNameWhenChanged() {
        LessonRepository repo = mock(LessonRepository.class);
        Unit existing = new Unit();
        existing.setId(1);
        existing.setName("F1");
        existing.setTitleUz("Foo");
        when(repo.getUnitById(1)).thenReturn(existing);
        when(repo.checkUnitNameExists("F2", 1)).thenReturn(true);

        var response = new UnitController(repo).update(1, Map.of("name", "F2", "titleUz", "Foo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).updateUnit(any());
    }

    @Test
    void updateSucceeds() {
        LessonRepository repo = mock(LessonRepository.class);
        Unit existing = new Unit();
        existing.setId(1);
        existing.setName("F1");
        existing.setTitleUz("Foo");
        when(repo.getUnitById(1)).thenReturn(existing);

        var response = new UnitController(repo).update(1, Map.of("name", "F1", "titleUz", "Bar"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updateUnit(existing);
        assertThat(existing.getTitleUz()).isEqualTo("Bar");
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getUnitById(1)).thenReturn(null);

        var response = new UnitController(repo).delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repo, never()).deleteUnit(anyInt());
    }

    @Test
    void deleteSucceeds() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getUnitById(1)).thenReturn(new Unit());

        var response = new UnitController(repo).delete(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).deleteUnit(1);
    }

    @Test
    void lessonsDelegatesToRepository() {
        LessonRepository repo = mock(LessonRepository.class);

        new UnitController(repo).lessons(1);

        verify(repo).getLessonsByUnitId(1);
    }
}

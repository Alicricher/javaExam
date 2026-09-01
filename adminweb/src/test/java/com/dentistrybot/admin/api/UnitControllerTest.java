package com.dentistrybot.admin.api;

import com.dentistrybot.admin.security.AccessControlService;
import com.dentistrybot.shared.model.Unit;
import com.dentistrybot.shared.repository.LessonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UnitControllerTest {

    private static final Authentication AUTH = mock(Authentication.class);

    private AccessControlService permissiveAccessControl() {
        AccessControlService accessControl = mock(AccessControlService.class);
        lenient().when(accessControl.isSuperAdminOrZavKafedra(any())).thenReturn(true);
        lenient().when(accessControl.canManageUnit(any(), anyInt())).thenReturn(true);
        return accessControl;
    }

    @Test
    void listReturnsAllUnits() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getAllUnits()).thenReturn(List.of(new Unit()));

        var response = new UnitController(repo, permissiveAccessControl()).list();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) response.getBody()).hasSize(1);
    }

    @Test
    void getReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getUnitById(1)).thenReturn(null);

        var response = new UnitController(repo, permissiveAccessControl()).get(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getReturnsUnitWhenFound() {
        LessonRepository repo = mock(LessonRepository.class);
        Unit u = new Unit();
        u.setId(1);
        when(repo.getUnitById(1)).thenReturn(u);

        var response = new UnitController(repo, permissiveAccessControl()).get(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(u);
    }

    @Test
    void createRejectsBlankFields() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new UnitController(repo, permissiveAccessControl()).create(Map.of("name", " ", "titleUz", "Foo"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createUnit(any());
    }

    @Test
    void createRejectsNameTooLong() {
        LessonRepository repo = mock(LessonRepository.class);

        var response = new UnitController(repo, permissiveAccessControl()).create(Map.of("name", "A".repeat(11), "titleUz", "Foo"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createUnit(any());
    }

    @Test
    void createRejectsDuplicateName() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.checkUnitNameExists("f1", -1)).thenReturn(true);

        var response = new UnitController(repo, permissiveAccessControl()).create(Map.of("name", "f1", "titleUz", "Foo"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createUnit(any());
    }

    @Test
    void createNormalizesNameAndTitle() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.checkUnitNameExists("f1", -1)).thenReturn(false);
        when(repo.createUnit(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = new UnitController(repo, permissiveAccessControl()).create(Map.of("name", " f1 ", "titleUz", " Foo "), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Unit created = (Unit) response.getBody();
        assertThat(created.getName()).isEqualTo("F1");
        assertThat(created.getTitleUz()).isEqualTo("Foo");
    }

    @Test
    void createForbiddenForProfessor() {
        // Creating a brand-new unit is a curriculum decision, not "editing my subject" -
        // professors can never create units, even ones they'd otherwise be allowed to edit.
        LessonRepository repo = mock(LessonRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.isSuperAdminOrZavKafedra(AUTH)).thenReturn(false);

        var response = new UnitController(repo, accessControl).create(Map.of("name", "F1", "titleUz", "Foo"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).createUnit(any());
    }

    @Test
    void updateReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getUnitById(1)).thenReturn(null);

        var response = new UnitController(repo, permissiveAccessControl()).update(1, Map.of("name", "F1", "titleUz", "Foo"), AUTH);

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

        var response = new UnitController(repo, permissiveAccessControl()).update(1, Map.of("name", "F2", "titleUz", "Foo"), AUTH);

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

        var response = new UnitController(repo, permissiveAccessControl()).update(1, Map.of("name", "F1", "titleUz", "Bar"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updateUnit(existing);
        assertThat(existing.getTitleUz()).isEqualTo("Bar");
    }

    @Test
    void updateForbiddenWhenProfessorNotAssignedToUnit() {
        LessonRepository repo = mock(LessonRepository.class);
        Unit existing = new Unit();
        existing.setId(1);
        when(repo.getUnitById(1)).thenReturn(existing);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(AUTH, 1)).thenReturn(false);

        var response = new UnitController(repo, accessControl).update(1, Map.of("name", "F1", "titleUz", "Bar"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).updateUnit(any());
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getUnitById(1)).thenReturn(null);

        var response = new UnitController(repo, permissiveAccessControl()).delete(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repo, never()).deleteUnit(anyInt());
    }

    @Test
    void deleteSucceeds() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getUnitById(1)).thenReturn(new Unit());

        var response = new UnitController(repo, permissiveAccessControl()).delete(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).deleteUnit(1);
    }

    @Test
    void lessonsDelegatesToRepository() {
        LessonRepository repo = mock(LessonRepository.class);

        new UnitController(repo, permissiveAccessControl()).lessons(1);

        verify(repo).getLessonsByUnitId(1);
    }
}

package com.dentistrybot.admin.api;

import com.dentistrybot.admin.security.AccessControlService;
import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.model.TheoryMaterial;
import com.dentistrybot.shared.model.Unit;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TheoryControllerTest {

    private static final Authentication AUTH = mock(Authentication.class);

    private Lesson lesson(int id, int unitId, int number) {
        Lesson l = new Lesson();
        l.setId(id);
        l.setUnitId(unitId);
        l.setLessonNumber(number);
        return l;
    }

    /** Access-control mock that always permits - most tests aren't testing authorization. */
    private AccessControlService permissiveAccessControl() {
        AccessControlService accessControl = mock(AccessControlService.class);
        lenient().when(accessControl.canManageUnit(any(), anyInt())).thenReturn(true);
        return accessControl;
    }

    /** Stubs LessonRepository.getUnitIdForLesson so update/delete's access check resolves. */
    private void permitLesson(LessonRepository repo, int lessonId, int unitId) {
        lenient().when(repo.getUnitIdForLesson(lessonId)).thenReturn(unitId);
    }

    @Test
    void createRejectsUnknownLesson() {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);

        var response = new TheoryController(repo, fileService, permissiveAccessControl())
            .create(1, "Material", "material", null, null, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createRejectsBlankTitle() {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);
        when(repo.getLessonById(1)).thenReturn(lesson(1, 10, 1));

        var response = new TheoryController(repo, fileService, permissiveAccessControl())
            .create(1, " ", "material", null, null, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createTheoryMaterial(any());
    }

    @Test
    void createForbiddenWhenProfessorNotAssignedToUnit() {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);
        when(repo.getLessonById(1)).thenReturn(lesson(1, 10, 1));
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(AUTH, 10)).thenReturn(false);

        var response = new TheoryController(repo, fileService, accessControl)
            .create(1, "Material", "material", null, null, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).createTheoryMaterial(any());
    }

    @Test
    void createSucceedsWithoutFile() {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);
        when(repo.getLessonById(1)).thenReturn(lesson(1, 10, 1));
        when(repo.createTheoryMaterial(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = new TheoryController(repo, fileService, permissiveAccessControl())
            .create(1, " Material ", "book", " Desc ", null, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TheoryMaterial created = (TheoryMaterial) response.getBody();
        assertThat(created.getTitleUz()).isEqualTo("Material");
        assertThat(created.getMaterialType()).isEqualTo("book");
        assertThat(created.getDescription()).isEqualTo("Desc");
        assertThat(created.getFilePath()).isEqualTo("");
    }

    @Test
    void createSavesUploadedFile() throws Exception {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);
        Unit unit = new Unit();
        unit.setName("F1");
        when(repo.getLessonById(1)).thenReturn(lesson(1, 10, 2));
        when(repo.getUnitById(10)).thenReturn(unit);
        when(fileService.saveFile(eq("F1"), eq(2), eq("doc.pdf"), any())).thenReturn("F1/2/doc.pdf");
        when(repo.createTheoryMaterial(any())).thenAnswer(inv -> inv.getArgument(0));
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", "content".getBytes());

        var response = new TheoryController(repo, fileService, permissiveAccessControl())
            .create(1, "Material", "book", null, file, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        TheoryMaterial created = (TheoryMaterial) response.getBody();
        assertThat(created.getFilePath()).isEqualTo("F1/2/doc.pdf");
        verify(fileService).ensureMaterialsDir("F1", 2);
    }

    @Test
    void updateReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);

        var response = new TheoryController(repo, fileService, permissiveAccessControl())
            .update(1, "New", null, null, null, null, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateRejectsBlankTitle() {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);
        when(repo.getTheoryMaterialById(1)).thenReturn(new TheoryMaterial());
        permitLesson(repo, 0, 1);

        var response = new TheoryController(repo, fileService, permissiveAccessControl())
            .update(1, " ", null, null, null, null, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).updateTheoryMaterialTitle(anyInt(), anyString());
    }

    @Test
    void updateForbiddenWhenProfessorNotAssignedToUnit() {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);
        TheoryMaterial m = new TheoryMaterial();
        m.setId(1);
        m.setLessonId(1);
        when(repo.getTheoryMaterialById(1)).thenReturn(m);
        when(repo.getUnitIdForLesson(1)).thenReturn(10);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(AUTH, 10)).thenReturn(false);

        var response = new TheoryController(repo, fileService, accessControl)
            .update(1, "New", null, null, null, null, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).updateTheoryMaterialTitle(anyInt(), anyString());
    }

    @Test
    void updatePrefersJsonBodyOverParams() {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);
        TheoryMaterial m = new TheoryMaterial();
        m.setId(1);
        m.setLessonId(1);
        when(repo.getTheoryMaterialById(1)).thenReturn(m);
        permitLesson(repo, 1, 1);

        var response = new TheoryController(repo, fileService, permissiveAccessControl())
            .update(1, "ParamTitle", null, null, null, Map.of("titleUz", "BodyTitle"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updateTheoryMaterialTitle(1, "BodyTitle");
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);

        var response = new TheoryController(repo, fileService, permissiveAccessControl()).delete(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repo, never()).deleteTheoryMaterial(anyInt());
    }

    @Test
    void deleteRemovesFileWhenPresent() throws Exception {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);
        TheoryMaterial m = new TheoryMaterial();
        m.setId(1);
        m.setFilePath("F1/1/doc.pdf");
        when(repo.getTheoryMaterialById(1)).thenReturn(m);
        permitLesson(repo, 0, 1);

        var response = new TheoryController(repo, fileService, permissiveAccessControl()).delete(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(fileService).deleteFile("F1/1/doc.pdf");
        verify(repo).deleteTheoryMaterial(1);
    }

    @Test
    void downloadFileReturnsNotFoundWhenFilePathMissing() throws Exception {
        LessonRepository repo = mock(LessonRepository.class);
        FileService fileService = mock(FileService.class);
        TheoryMaterial m = new TheoryMaterial();
        m.setFilePath("");
        when(repo.getTheoryMaterialById(1)).thenReturn(m);
        HttpServletResponse response = mock(HttpServletResponse.class);

        new TheoryController(repo, fileService, permissiveAccessControl()).downloadFile(1, response);

        verify(response).sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}

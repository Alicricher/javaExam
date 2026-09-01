package com.dentistrybot.admin.api;

import com.dentistrybot.admin.security.AccessControlService;
import com.dentistrybot.shared.model.SituationalTask;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.service.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskControllerTest {

    private static final Authentication AUTH = mock(Authentication.class);

    /** Every lessonId resolves to unit 1 - this is a brand new repo method no test cares about
     * the specific value of, so a blanket stub can't shadow anything test-specific. */
    private LessonRepository permissiveLessonRepo() {
        LessonRepository repo = mock(LessonRepository.class);
        lenient().when(repo.getUnitIdForLesson(anyInt())).thenReturn(1);
        return repo;
    }

    private AccessControlService permissiveAccessControl() {
        AccessControlService accessControl = mock(AccessControlService.class);
        lenient().when(accessControl.canManageUnit(any(), anyInt())).thenReturn(true);
        return accessControl;
    }

    @Test
    void createRejectsMissingFields() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);

        var response = new TaskController(repo, fileService, permissiveAccessControl()).create(Map.of("lessonId", 1), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createSituationalTask(any());
    }

    @Test
    void createUsesDefaultTimeLimitAndNextOrder() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        when(repo.getNextTaskOrderNum(1)).thenReturn(3);
        when(repo.createSituationalTask(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = new TaskController(repo, fileService, permissiveAccessControl())
            .create(Map.of("lessonId", 1, "taskText", "Vaziyat"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        SituationalTask created = (SituationalTask) response.getBody();
        assertThat(created.getTimeLimitMinutes()).isEqualTo(30);
        assertThat(created.getOrderNum()).isEqualTo(3);
        assertThat(created.getTaskText()).isEqualTo("Vaziyat");
    }

    @Test
    void createHonorsExplicitTimeLimit() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        when(repo.getNextTaskOrderNum(1)).thenReturn(1);
        when(repo.createSituationalTask(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = new TaskController(repo, fileService, permissiveAccessControl())
            .create(Map.of("lessonId", 1, "taskText", "Vaziyat", "timeLimitMinutes", 45), AUTH);

        SituationalTask created = (SituationalTask) response.getBody();
        assertThat(created.getTimeLimitMinutes()).isEqualTo(45);
    }

    @Test
    void createForbiddenWhenProfessorNotAssignedToUnit() {
        LessonRepository repo = mock(LessonRepository.class);
        when(repo.getUnitIdForLesson(1)).thenReturn(10);
        FileService fileService = mock(FileService.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(AUTH, 10)).thenReturn(false);

        var response = new TaskController(repo, fileService, accessControl)
            .create(Map.of("lessonId", 1, "taskText", "Vaziyat"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).createSituationalTask(any());
    }

    @Test
    void updateReturnsNotFoundWhenMissing() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);

        var response = new TaskController(repo, fileService, permissiveAccessControl()).update(1, Map.of("taskText", "New"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateAppliesTextAndTime() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        when(repo.getSituationalTaskById(1)).thenReturn(t);

        var response = new TaskController(repo, fileService, permissiveAccessControl())
            .update(1, Map.of("taskText", " New text ", "timeLimitMinutes", 15), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updateSituationalTaskText(1, "New text");
        verify(repo).updateSituationalTaskTime(1, 15);
    }

    @Test
    void updateForbiddenWhenProfessorNotAssignedToUnit() {
        LessonRepository repo = mock(LessonRepository.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        t.setLessonId(2);
        when(repo.getSituationalTaskById(1)).thenReturn(t);
        when(repo.getUnitIdForLesson(2)).thenReturn(10);
        FileService fileService = mock(FileService.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(AUTH, 10)).thenReturn(false);

        var response = new TaskController(repo, fileService, accessControl)
            .update(1, Map.of("taskText", "New"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).updateSituationalTaskText(anyInt(), anyString());
    }

    @Test
    void deleteReturnsNotFoundWhenMissing() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);

        var response = new TaskController(repo, fileService, permissiveAccessControl()).delete(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repo, never()).deleteSituationalTask(anyInt());
    }

    @Test
    void deleteRenumbersRemainingTasks() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        t.setLessonId(7);
        when(repo.getSituationalTaskById(1)).thenReturn(t);

        var response = new TaskController(repo, fileService, permissiveAccessControl()).delete(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).deleteSituationalTask(1);
        verify(repo).renumberSituationalTasks(7);
    }

    // ---------------- uploadPhoto ----------------

    @Test
    void uploadPhotoReturnsNotFoundWhenTaskMissing() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());

        var response = new TaskController(repo, fileService, permissiveAccessControl()).uploadPhoto(1, file, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(fileService);
    }

    @Test
    void uploadPhotoRejectsEmptyFile() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        when(repo.getSituationalTaskById(1)).thenReturn(t);
        var emptyFile = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[0]);

        var response = new TaskController(repo, fileService, permissiveAccessControl()).uploadPhoto(1, emptyFile, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(fileService);
    }

    @Test
    void uploadPhotoSavesAndUpdatesPathWhenNoExistingPhoto() throws Exception {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        when(repo.getSituationalTaskById(1)).thenReturn(t);
        when(fileService.savePhoto(eq("photos/tasks"), eq("t_1_photo.jpg"), any()))
            .thenReturn("photos/tasks/t_1_photo.jpg");
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());

        var response = new TaskController(repo, fileService, permissiveAccessControl()).uploadPhoto(1, file, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("photoFilePath", "photos/tasks/t_1_photo.jpg"));
        verify(repo).updateSituationalTaskPhotoFilePath(1, "photos/tasks/t_1_photo.jpg");
        verify(fileService, never()).deleteFile(any());
    }

    @Test
    void uploadPhotoDeletesOldPhotoWhenReplacingExisting() throws Exception {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        t.setPhotoFilePath("photos/tasks/t_1_old.jpg");
        when(repo.getSituationalTaskById(1)).thenReturn(t);
        when(fileService.savePhoto(anyString(), anyString(), any()))
            .thenReturn("photos/tasks/t_1_new.jpg");
        var file = new MockMultipartFile("file", "new.jpg", "image/jpeg", "data".getBytes());

        var response = new TaskController(repo, fileService, permissiveAccessControl()).uploadPhoto(1, file, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(fileService).deleteFile("photos/tasks/t_1_old.jpg");
        verify(repo).updateSituationalTaskPhotoFilePath(1, "photos/tasks/t_1_new.jpg");
    }

    @Test
    void uploadPhotoSucceedsEvenWhenOldPhotoDeletionFails() throws Exception {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        t.setPhotoFilePath("photos/tasks/t_1_old.jpg");
        when(repo.getSituationalTaskById(1)).thenReturn(t);
        doThrow(new IOException("disk error")).when(fileService).deleteFile("photos/tasks/t_1_old.jpg");
        when(fileService.savePhoto(anyString(), anyString(), any()))
            .thenReturn("photos/tasks/t_1_new.jpg");
        var file = new MockMultipartFile("file", "new.jpg", "image/jpeg", "data".getBytes());

        var response = new TaskController(repo, fileService, permissiveAccessControl()).uploadPhoto(1, file, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updateSituationalTaskPhotoFilePath(1, "photos/tasks/t_1_new.jpg");
    }

    @Test
    void uploadPhotoReturns500WhenSaveFails() throws Exception {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        when(repo.getSituationalTaskById(1)).thenReturn(t);
        when(fileService.savePhoto(anyString(), anyString(), any()))
            .thenThrow(new IOException("disk full"));
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());

        var response = new TaskController(repo, fileService, permissiveAccessControl()).uploadPhoto(1, file, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(repo, never()).updateSituationalTaskPhotoFilePath(anyInt(), any());
    }

    // ---------------- deletePhoto ----------------

    @Test
    void deletePhotoReturnsNotFoundWhenTaskMissing() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);

        var response = new TaskController(repo, fileService, permissiveAccessControl()).deletePhoto(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verifyNoInteractions(fileService);
    }

    @Test
    void deletePhotoSkipsFileDeletionWhenTaskHasNoPhoto() throws Exception {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        when(repo.getSituationalTaskById(1)).thenReturn(t);

        var response = new TaskController(repo, fileService, permissiveAccessControl()).deletePhoto(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(fileService, never()).deleteFile(any());
        verify(repo).updateSituationalTaskPhotoFilePath(1, null);
    }

    @Test
    void deletePhotoRemovesFileAndClearsPath() throws Exception {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        t.setPhotoFilePath("photos/tasks/t_1.jpg");
        when(repo.getSituationalTaskById(1)).thenReturn(t);

        var response = new TaskController(repo, fileService, permissiveAccessControl()).deletePhoto(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(fileService).deleteFile("photos/tasks/t_1.jpg");
        verify(repo).updateSituationalTaskPhotoFilePath(1, null);
    }

    @Test
    void deletePhotoSucceedsEvenWhenFileDeletionFails() throws Exception {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        t.setPhotoFilePath("photos/tasks/t_1.jpg");
        when(repo.getSituationalTaskById(1)).thenReturn(t);
        doThrow(new IOException("disk error")).when(fileService).deleteFile("photos/tasks/t_1.jpg");

        var response = new TaskController(repo, fileService, permissiveAccessControl()).deletePhoto(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updateSituationalTaskPhotoFilePath(1, null);
    }

    @Test
    void deletePhotoReturns500WhenRepoUpdateFails() {
        LessonRepository repo = permissiveLessonRepo();
        FileService fileService = mock(FileService.class);
        SituationalTask t = new SituationalTask();
        t.setId(1);
        when(repo.getSituationalTaskById(1)).thenReturn(t);
        doThrow(new RuntimeException("db down")).when(repo).updateSituationalTaskPhotoFilePath(1, null);

        var response = new TaskController(repo, fileService, permissiveAccessControl()).deletePhoto(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

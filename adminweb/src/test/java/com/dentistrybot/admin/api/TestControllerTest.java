package com.dentistrybot.admin.api;

import com.dentistrybot.admin.security.AccessControlService;
import com.dentistrybot.shared.model.*;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.TestRepository;
import com.dentistrybot.shared.service.FileService;
import com.dentistrybot.shared.service.ImportService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TestControllerTest {

    private static final Authentication AUTH = mock(Authentication.class);

    private TestController controller(TestRepository testRepo) {
        LessonRepository lessonRepo = mock(LessonRepository.class);
        lenient().when(lessonRepo.getUnitIdForLesson(anyInt())).thenReturn(1);
        // Brand new repo methods no existing stub in this file ever touches - safe to
        // blanket-permit regardless of call order, unlike getTestById/getQuestionById
        // which many tests stub with specific ids for their own assertions.
        lenient().when(testRepo.getUnitIdForTest(anyInt())).thenReturn(1);
        lenient().when(testRepo.getUnitIdForQuestion(anyInt())).thenReturn(1);
        AccessControlService accessControl = mock(AccessControlService.class);
        lenient().when(accessControl.canManageUnit(any(), anyInt())).thenReturn(true);
        return new TestController(testRepo, lessonRepo, mock(ImportService.class), mock(FileService.class), accessControl);
    }

    private com.dentistrybot.shared.model.Test activeTest(int id) {
        com.dentistrybot.shared.model.Test t = new com.dentistrybot.shared.model.Test();
        t.setId(id);
        t.setActive(true);
        return t;
    }

    // ===== TESTS =====

    @Test
    void getTestReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).getTest(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createTestRejectsMissingLessonId() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).createTest(Map.of(), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).createTest(any());
    }

    @Test
    void createTestDefaultsTimeLimitTo30() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.createTest(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = controller(repo).createTest(Map.of("lessonId", 1), AUTH);

        com.dentistrybot.shared.model.Test created = (com.dentistrybot.shared.model.Test) response.getBody();
        assertThat(created.getTimeLimitMinutes()).isEqualTo(30);
        assertThat(created.getTotalPoints()).isEqualTo(0);
    }

    @Test
    void createTestForbiddenWhenProfessorNotAssignedToUnit() {
        TestRepository repo = mock(TestRepository.class);
        LessonRepository lessonRepo = mock(LessonRepository.class);
        when(lessonRepo.getUnitIdForLesson(1)).thenReturn(10);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(AUTH, 10)).thenReturn(false);

        var response = new TestController(repo, lessonRepo, mock(ImportService.class), mock(FileService.class), accessControl)
            .createTest(Map.of("lessonId", 1), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).createTest(any());
    }

    @Test
    void updateTestReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).updateTest(1, Map.of("titleUz", "New"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateTestAppliesProvidedFields() {
        TestRepository repo = mock(TestRepository.class);
        com.dentistrybot.shared.model.Test t = activeTest(1);
        when(repo.getTestById(1)).thenReturn(t);

        var response = controller(repo).updateTest(1, Map.of("titleUz", "New", "timeLimitMinutes", 45), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(t.getTitleUz()).isEqualTo("New");
        assertThat(t.getTimeLimitMinutes()).isEqualTo(45);
        verify(repo).updateTest(t);
    }

    @Test
    void updateTestForbiddenWhenProfessorNotAssignedToUnit() {
        TestRepository repo = mock(TestRepository.class);
        com.dentistrybot.shared.model.Test t = activeTest(1);
        when(repo.getTestById(1)).thenReturn(t);
        LessonRepository lessonRepo = mock(LessonRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(eq(AUTH), anyInt())).thenReturn(false);

        var response = new TestController(repo, lessonRepo, mock(ImportService.class), mock(FileService.class), accessControl)
            .updateTest(1, Map.of("titleUz", "New"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).updateTest(any());
    }

    @Test
    void deleteTestReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).deleteTest(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repo, never()).deleteTest(anyInt());
    }

    @Test
    void deleteTestSucceeds() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.getTestById(1)).thenReturn(activeTest(1));

        var response = controller(repo).deleteTest(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).deleteTest(1);
    }

    // ===== QUESTIONS =====

    @Test
    void getQuestionsAppliesPagination() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.getQuestionsByTestIdPaginated(1, 15, 15)).thenReturn(List.of(new Question()));
        when(repo.countQuestions(1)).thenReturn(30);

        var response = controller(repo).getQuestions(1, 2, 15);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("total", 30).containsEntry("page", 2);
    }

    @Test
    void getQuestionReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).getQuestion(1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createQuestionRejectsMissingTest() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).createQuestion(1, Map.of("questionText", "Q"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createQuestionRejectsInactiveTest() {
        TestRepository repo = mock(TestRepository.class);
        com.dentistrybot.shared.model.Test t = activeTest(1);
        t.setActive(false);
        when(repo.getTestById(1)).thenReturn(t);

        var response = controller(repo).createQuestion(1, Map.of("questionText", "Q"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createQuestionForbiddenWhenProfessorNotAssignedToUnit() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.getTestById(1)).thenReturn(activeTest(1));
        LessonRepository lessonRepo = mock(LessonRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(eq(AUTH), anyInt())).thenReturn(false);

        var response = new TestController(repo, lessonRepo, mock(ImportService.class), mock(FileService.class), accessControl)
            .createQuestion(1, Map.of("questionText", "Q"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(repo, never()).createQuestion(any());
    }

    @Test
    void createQuestionRejectsBlankText() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.getTestById(1)).thenReturn(activeTest(1));

        var response = controller(repo).createQuestion(1, Map.of("questionText", " "), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createQuestionRejectsNonPositivePoints() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.getTestById(1)).thenReturn(activeTest(1));

        var response = controller(repo).createQuestion(1, Map.of("questionText", "Q", "points", 0), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createQuestionRejectsFewerThanTwoOptions() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.getTestById(1)).thenReturn(activeTest(1));

        var response = controller(repo).createQuestion(1, Map.of(
            "questionText", "Q", "options", List.of(Map.of("optionText", "A", "isCorrect", true))), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createQuestionRejectsWhenNotExactlyOneCorrect() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.getTestById(1)).thenReturn(activeTest(1));

        var response = controller(repo).createQuestion(1, Map.of(
            "questionText", "Q",
            "options", List.of(
                Map.of("optionText", "A", "isCorrect", true),
                Map.of("optionText", "B", "isCorrect", true))), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createQuestionSucceedsAndCreatesOptions() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.getTestById(1)).thenReturn(activeTest(1));
        when(repo.getNextQuestionOrderNum(1)).thenReturn(5);
        when(repo.getQuestionWithOptions(anyInt())).thenReturn(new QuestionWithOptions(new Question(), List.of()));

        var response = controller(repo).createQuestion(1, Map.of(
            "questionText", "Q", "points", 2,
            "options", List.of(
                Map.of("optionText", "A", "isCorrect", true),
                Map.of("optionText", "B", "isCorrect", false))), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).createQuestion(argThat(q -> q.getQuestionText().equals("Q") && q.getPoints() == 2 && q.getOrderNum() == 5));
        verify(repo, times(2)).createAnswerOption(any());
    }

    @Test
    void createQuestionSkipsBlankOptionText() {
        TestRepository repo = mock(TestRepository.class);
        when(repo.getTestById(1)).thenReturn(activeTest(1));
        when(repo.getQuestionWithOptions(anyInt())).thenReturn(new QuestionWithOptions(new Question(), List.of()));

        controller(repo).createQuestion(1, Map.of(
            "questionText", "Q",
            "options", List.of(
                Map.of("optionText", "A", "isCorrect", true),
                Map.of("optionText", " ", "isCorrect", false),
                Map.of("optionText", "C", "isCorrect", false))), AUTH);

        verify(repo, times(2)).createAnswerOption(any());
    }

    @Test
    void updateQuestionReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).updateQuestion(1, Map.of("questionText", "New"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateQuestionAppliesFields() {
        TestRepository repo = mock(TestRepository.class);
        Question q = new Question();
        q.setId(1);
        when(repo.getQuestionById(1)).thenReturn(q);
        when(repo.getQuestionWithOptions(1)).thenReturn(new QuestionWithOptions(q, List.of()));

        var response = controller(repo).updateQuestion(1, Map.of("questionText", "New", "points", 3), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(q.getQuestionText()).isEqualTo("New");
        assertThat(q.getPoints()).isEqualTo(3);
        verify(repo).updateQuestion(q);
    }

    @Test
    void deleteQuestionReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).deleteQuestion(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(repo, never()).deleteQuestion(anyInt(), anyInt());
    }

    @Test
    void deleteQuestionSucceeds() {
        TestRepository repo = mock(TestRepository.class);
        Question q = new Question();
        q.setId(1);
        q.setTestId(7);
        when(repo.getQuestionById(1)).thenReturn(q);

        var response = controller(repo).deleteQuestion(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).deleteQuestion(1, 7);
    }

    @Test
    void moveQuestionReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).moveQuestion(1, Map.of("direction", "up"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void moveQuestionUpSwapsWithPrevious() {
        TestRepository repo = mock(TestRepository.class);
        Question q1 = new Question(); q1.setId(1);
        Question q2 = new Question(); q2.setId(2);
        when(repo.getQuestionById(2)).thenReturn(q2);
        when(repo.getQuestionsByTestId(0)).thenReturn(List.of(q1, q2));

        var response = controller(repo).moveQuestion(2, Map.of("direction", "up"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).swapQuestionOrders(2, 1);
    }

    @Test
    void moveQuestionUpIsNoOpAtTop() {
        TestRepository repo = mock(TestRepository.class);
        Question q1 = new Question(); q1.setId(1);
        Question q2 = new Question(); q2.setId(2);
        when(repo.getQuestionById(1)).thenReturn(q1);
        when(repo.getQuestionsByTestId(0)).thenReturn(List.of(q1, q2));

        var response = controller(repo).moveQuestion(1, Map.of("direction", "up"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo, never()).swapQuestionOrders(anyInt(), anyInt());
    }

    @Test
    void moveQuestionDownSwapsWithNext() {
        TestRepository repo = mock(TestRepository.class);
        Question q1 = new Question(); q1.setId(1);
        Question q2 = new Question(); q2.setId(2);
        when(repo.getQuestionById(1)).thenReturn(q1);
        when(repo.getQuestionsByTestId(0)).thenReturn(List.of(q1, q2));

        var response = controller(repo).moveQuestion(1, Map.of("direction", "down"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).swapQuestionOrders(1, 2);
    }

    @Test
    void moveQuestionDownIsNoOpAtBottom() {
        TestRepository repo = mock(TestRepository.class);
        Question q1 = new Question(); q1.setId(1);
        Question q2 = new Question(); q2.setId(2);
        when(repo.getQuestionById(2)).thenReturn(q2);
        when(repo.getQuestionsByTestId(0)).thenReturn(List.of(q1, q2));

        var response = controller(repo).moveQuestion(2, Map.of("direction", "down"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo, never()).swapQuestionOrders(anyInt(), anyInt());
    }

    @Test
    void clearQuestionsDelegatesToRepository() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).clearQuestions(5, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).deleteAllQuestions(5);
    }

    // ===== QUESTION PHOTOS =====

    @Test
    void uploadQuestionPhotoReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());

        var response = controller(repo).uploadQuestionPhoto(1, file, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void uploadQuestionPhotoSavesAndUpdatesPath() throws Exception {
        TestRepository repo = mock(TestRepository.class);
        Question q = new Question();
        q.setId(1);
        when(repo.getQuestionById(1)).thenReturn(q);
        FileService fileService = mock(FileService.class);
        when(fileService.savePhoto(eq("photos/questions"), eq("q_1_photo.jpg"), any()))
            .thenReturn("photos/questions/q_1_photo.jpg");
        LessonRepository lessonRepo = mock(LessonRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(any(), anyInt())).thenReturn(true);
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());

        var response = new TestController(repo, lessonRepo, mock(ImportService.class), fileService, accessControl)
            .uploadQuestionPhoto(1, file, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).updateQuestionPhotoFilePath(1, "photos/questions/q_1_photo.jpg");
    }

    @Test
    void uploadQuestionPhotoForbiddenWhenProfessorNotAssignedToUnit() {
        TestRepository repo = mock(TestRepository.class);
        Question q = new Question();
        q.setId(1);
        when(repo.getQuestionById(1)).thenReturn(q);
        FileService fileService = mock(FileService.class);
        LessonRepository lessonRepo = mock(LessonRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(eq(AUTH), anyInt())).thenReturn(false);
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());

        var response = new TestController(repo, lessonRepo, mock(ImportService.class), fileService, accessControl)
            .uploadQuestionPhoto(1, file, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verifyNoInteractions(fileService);
    }

    @Test
    void deleteQuestionPhotoReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).deleteQuestionPhoto(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteQuestionPhotoRemovesFileAndClearsPath() throws Exception {
        TestRepository repo = mock(TestRepository.class);
        Question q = new Question();
        q.setId(1);
        q.setPhotoFilePath("photos/questions/q_1.jpg");
        when(repo.getQuestionById(1)).thenReturn(q);
        FileService fileService = mock(FileService.class);
        LessonRepository lessonRepo = mock(LessonRepository.class);
        AccessControlService accessControl = mock(AccessControlService.class);
        when(accessControl.canManageUnit(any(), anyInt())).thenReturn(true);

        var response = new TestController(repo, lessonRepo, mock(ImportService.class), fileService, accessControl)
            .deleteQuestionPhoto(1, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(fileService).deleteFile("photos/questions/q_1.jpg");
        verify(repo).updateQuestionPhotoFilePath(1, null);
    }

    // ===== ANSWER OPTIONS =====

    @Test
    void updateOptionReturnsNotFoundWhenMissing() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).updateOption(1, 2, Map.of("optionText", "New"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateOptionAppliesFields() {
        TestRepository repo = mock(TestRepository.class);
        AnswerOption opt = new AnswerOption();
        opt.setId(2);
        when(repo.getAnswerOptionById(2)).thenReturn(opt);

        var response = controller(repo).updateOption(1, 2, Map.of("optionText", "New", "isCorrect", "true"), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(opt.getOptionText()).isEqualTo("New");
        assertThat(opt.isCorrect()).isTrue();
        verify(repo).updateAnswerOption(opt);
    }

    @Test
    void setCorrectRejectsMissingOptionId() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).setCorrect(1, Map.of(), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(repo, never()).setCorrectAnswer(anyInt(), anyInt());
    }

    @Test
    void setCorrectDelegatesToRepository() {
        TestRepository repo = mock(TestRepository.class);

        var response = controller(repo).setCorrect(1, Map.of("optionId", 9), AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(repo).setCorrectAnswer(1, 9);
    }

    // ===== IMPORT =====

    @Test
    void importQuestionsRejectsEmptyFile() {
        TestRepository repo = mock(TestRepository.class);
        MockMultipartFile empty = new MockMultipartFile("file", "q.xlsx", "application/octet-stream", new byte[0]);

        var response = controller(repo).importQuestions(1, empty, AUTH);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

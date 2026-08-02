package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.SituationalAnswer;
import com.dentistrybot.shared.model.SituationalTask;
import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.service.TestService;
import com.dentistrybot.shared.state.SituationalStateData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SituationalHandlerTest extends HandlerTestSupport {

    private TelegramClient bot;
    private StateManager stateManager;
    private TestService testService;
    private LessonRepository lessonRepository;
    private ResultRepository resultRepository;
    private StudentRepository studentRepository;
    private SituationalHandler handler;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramClient.class);
        stateManager = mock(StateManager.class);
        testService = mock(TestService.class);
        lessonRepository = mock(LessonRepository.class);
        resultRepository = mock(ResultRepository.class);
        studentRepository = mock(StudentRepository.class);
        handler = new SituationalHandler(bot, stateManager, testService, lessonRepository, resultRepository, studentRepository);
    }

    private Student student() {
        Student s = new Student();
        s.setId(42);
        s.setTelegramId(TELEGRAM_ID);
        return s;
    }

    private SituationalTask task(int id, int lessonId, int orderNum, String text, int timeLimit) {
        SituationalTask t = new SituationalTask();
        t.setId(id);
        t.setLessonId(lessonId);
        t.setOrderNum(orderNum);
        t.setTaskText(text);
        t.setTimeLimitMinutes(timeLimit);
        return t;
    }

    private SituationalStateData stateData() {
        SituationalStateData sd = new SituationalStateData();
        sd.setTaskId(1);
        sd.setLessonId(3);
        sd.setTaskNumber(1);
        sd.setTotalTasks(2);
        sd.setStartedAt(Instant.now().minusSeconds(30));
        sd.setTimeLimitMinutes(15);
        return sd;
    }

    // ---------------- handleSituationalCallback ----------------

    @org.junit.jupiter.api.Test
    void situationalCallback_noTasks_showsNoTaskMessage() throws Exception {
        when(lessonRepository.getSituationalTasksByLessonId(5)).thenReturn(List.of());

        handler.handleSituationalCallback(callbackWithData(StudentKeyboards.CB_SIT + "5"));

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).isEqualTo(UzMessages.MSG_NO_TASK_AVAILABLE);
    }

    @org.junit.jupiter.api.Test
    void situationalCallback_studentNotFound_stopsAfterTaskLookup() throws Exception {
        when(lessonRepository.getSituationalTasksByLessonId(5)).thenReturn(List.of(task(1, 5, 1, "matn", 30)));
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(null);

        handler.handleSituationalCallback(callbackWithData(StudentKeyboards.CB_SIT + "5"));

        verify(bot, never()).execute(any(EditMessageText.class));
    }

    @org.junit.jupiter.api.Test
    void situationalCallback_showsProgressCount() throws Exception {
        SituationalTask t1 = task(1, 5, 1, "matn1", 30);
        SituationalTask t2 = task(2, 5, 2, "matn2", 30);
        when(lessonRepository.getSituationalTasksByLessonId(5)).thenReturn(List.of(t1, t2));
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        when(resultRepository.getSubmittedTaskIds(42, List.of(1, 2))).thenReturn(Map.of(1, true));

        handler.handleSituationalCallback(callbackWithData(StudentKeyboards.CB_SIT + "5"));

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains("2 ta").contains("Bajarilgan: 1/2");
    }

    // ---------------- handleTaskSelectCallback ----------------

    @org.junit.jupiter.api.Test
    void taskSelectCallback_alreadySubmittedNoRetake_showsBlockedMessage() throws Exception {
        SituationalTask t = task(1, 5, 1, "matn", 30);
        when(lessonRepository.getSituationalTaskById(1)).thenReturn(t);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        when(resultRepository.hasSubmittedSituationalTask(42, 1)).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("no retake"))
            .when(resultRepository).useSituationalRetakeAtomically(42, 1);

        handler.handleTaskSelectCallback(callbackWithData(StudentKeyboards.CB_SIT_TASK + "1"));

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains("allaqachon javob bergansiz");
    }

    @org.junit.jupiter.api.Test
    void taskSelectCallback_notSubmittedYet_showsConfirmPrompt() throws Exception {
        SituationalTask t = task(1, 5, 1, "matn", 30);
        when(lessonRepository.getSituationalTaskById(1)).thenReturn(t);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        when(resultRepository.hasSubmittedSituationalTask(42, 1)).thenReturn(false);

        handler.handleTaskSelectCallback(callbackWithData(StudentKeyboards.CB_SIT_TASK + "1"));

        verify(resultRepository, never()).useSituationalRetakeAtomically(anyInt(), anyInt());
        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains("1-masala");
    }

    @org.junit.jupiter.api.Test
    void taskSelectCallback_submittedWithRetakeGranted_showsConfirmPrompt() throws Exception {
        SituationalTask t = task(1, 5, 1, "matn", 30);
        when(lessonRepository.getSituationalTaskById(1)).thenReturn(t);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        when(resultRepository.hasSubmittedSituationalTask(42, 1)).thenReturn(true);
        // useSituationalRetakeAtomically succeeds silently (no throw) -> retake consumed

        handler.handleTaskSelectCallback(callbackWithData(StudentKeyboards.CB_SIT_TASK + "1"));

        verify(resultRepository).useSituationalRetakeAtomically(42, 1);
        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains("1-masala").doesNotContain("allaqachon javob bergansiz");
    }

    // ---------------- handleSituationalConfirm ----------------

    @org.junit.jupiter.api.Test
    void situationalConfirm_alreadyInSituational_onlyAnswersCallback() throws Exception {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.IN_SITUATIONAL);

        handler.handleSituationalConfirm(callbackWithData(StudentKeyboards.CB_CONFIRM + "sit:1"));

        verify(bot, never()).execute(any(SendMessage.class));
        verify(stateManager, never()).setStateWithData(eq(TELEGRAM_ID), org.mockito.ArgumentMatchers.anyString(), any());
    }

    @org.junit.jupiter.api.Test
    void situationalConfirm_happyPath_startsAndShowsTaskText() throws Exception {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.IDLE);
        SituationalTask t = task(1, 3, 1, "Bemor shikoyat qilmoqda...", 15);
        when(lessonRepository.getSituationalTaskById(1)).thenReturn(t);
        when(lessonRepository.getSituationalTasksByLessonId(3)).thenReturn(List.of(t, task(2, 3, 2, "matn2", 15)));
        when(testService.getRemainingTime(any(Instant.class), anyInt())).thenReturn(Duration.ofMinutes(15));

        handler.handleSituationalConfirm(callbackWithData(StudentKeyboards.CB_CONFIRM + "sit:1"));

        ArgumentCaptor<SituationalStateData> captor = ArgumentCaptor.forClass(SituationalStateData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.IN_SITUATIONAL), captor.capture());
        assertThat(captor.getValue().getTaskId()).isEqualTo(1);
        assertThat(captor.getValue().getLessonId()).isEqualTo(3);
        assertThat(captor.getValue().getTotalTasks()).isEqualTo(2);

        List<DeleteMessage> deletes = executedOf(bot, DeleteMessage.class);
        assertThat(deletes).hasSize(1);

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent.get(0).getText()).contains("Bemor shikoyat qilmoqda...");
    }

    // ---------------- handleSituationalAnswer ----------------

    @org.junit.jupiter.api.Test
    void situationalAnswer_noActiveState_doesNothing() throws Exception {
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(null);

        handler.handleSituationalAnswer(textMessage("javob matni"));

        verify(bot, never()).execute(any(org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod.class));
    }

    @org.junit.jupiter.api.Test
    void situationalAnswer_timeUp_savesAnswerAndClearsState() throws Exception {
        UserState us = new UserState();
        SituationalStateData sd = stateData();
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, SituationalStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(true);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());

        handler.handleSituationalAnswer(textMessage("vaqtida yubormagan javob"));

        ArgumentCaptor<SituationalAnswer> captor = ArgumentCaptor.forClass(SituationalAnswer.class);
        verify(resultRepository).createSituationalAnswer(captor.capture());
        assertThat(captor.getValue().getAnswerText()).isEqualTo("vaqtida yubormagan javob");
        verify(stateManager).clearState(TELEGRAM_ID);
        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().contains(UzMessages.MSG_SITUATIONAL_TIME_UP));
    }

    @org.junit.jupiter.api.Test
    void situationalAnswer_photoRejected_asksForTextOnly() throws Exception {
        UserState us = new UserState();
        SituationalStateData sd = stateData();
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, SituationalStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(false);

        handler.handleSituationalAnswer(photoMessage(List.of(new PhotoSize())));

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().contains("Rasm qabul qilinmaydi"));
        verify(stateManager, never()).setStateWithData(eq(TELEGRAM_ID), org.mockito.ArgumentMatchers.anyString(), any());
    }

    @org.junit.jupiter.api.Test
    void situationalAnswer_blankText_asksToResend() throws Exception {
        UserState us = new UserState();
        SituationalStateData sd = stateData();
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, SituationalStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(false);

        handler.handleSituationalAnswer(textMessage("   "));

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().contains("javobingizni matn"));
    }

    @org.junit.jupiter.api.Test
    void situationalAnswer_validText_movesToConfirmState() throws Exception {
        UserState us = new UserState();
        SituationalStateData sd = stateData();
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, SituationalStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(false);

        handler.handleSituationalAnswer(textMessage("Mening javobim shu."));

        ArgumentCaptor<SituationalStateData> captor = ArgumentCaptor.forClass(SituationalStateData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.SITUATIONAL_CONFIRM_ANSWER), captor.capture());
        assertThat(captor.getValue().getAnswerText()).isEqualTo("Mening javobim shu.");

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().contains("tasdiqlaysizmi"));
    }

    // ---------------- handleConfirmAnswer ----------------

    @org.junit.jupiter.api.Test
    void confirmAnswer_happyPath_savesAndShowsNextTask() throws Exception {
        UserState us = new UserState();
        SituationalStateData sd = stateData();
        sd.setAnswerText("Javob matni");
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, SituationalStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(false);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());

        SituationalTask next = task(2, 3, 2, "keyingi masala", 15);
        when(lessonRepository.getSituationalTasksByLessonId(3)).thenReturn(List.of(task(1, 3, 1, "matn", 15), next));
        when(resultRepository.hasSubmittedSituationalTask(42, 1)).thenReturn(true);
        when(resultRepository.hasSubmittedSituationalTask(42, 2)).thenReturn(false);

        handler.handleConfirmAnswer(callbackWithData(StudentKeyboards.CB_CONFIRM + "sit_answer"));

        ArgumentCaptor<SituationalAnswer> answerCaptor = ArgumentCaptor.forClass(SituationalAnswer.class);
        verify(resultRepository).createSituationalAnswer(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getStudentId()).isEqualTo(42);
        assertThat(answerCaptor.getValue().getTaskId()).isEqualTo(1);

        verify(stateManager).clearState(TELEGRAM_ID);
        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().contains("javob qabul qilindi"));
    }

    @org.junit.jupiter.api.Test
    void confirmAnswer_allTasksDone_showsCelebrationMessage() throws Exception {
        UserState us = new UserState();
        SituationalStateData sd = stateData();
        sd.setAnswerText("Javob matni");
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, SituationalStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(false);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());

        when(lessonRepository.getSituationalTasksByLessonId(3)).thenReturn(List.of(task(1, 3, 1, "matn", 15)));
        when(resultRepository.hasSubmittedSituationalTask(42, 1)).thenReturn(true);

        handler.handleConfirmAnswer(callbackWithData(StudentKeyboards.CB_CONFIRM + "sit_answer"));

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().contains("Barcha vaziyatli masalalar bajarildi"));
    }

    @org.junit.jupiter.api.Test
    void confirmAnswer_duplicateSubmission_showsErrorAndClearsState() throws Exception {
        UserState us = new UserState();
        SituationalStateData sd = stateData();
        sd.setAnswerText("Javob matni");
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, SituationalStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(false);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        org.mockito.Mockito.doThrow(new RuntimeException("duplicate key violates unique constraint"))
            .when(resultRepository).createSituationalAnswer(any());

        handler.handleConfirmAnswer(callbackWithData(StudentKeyboards.CB_CONFIRM + "sit_answer"));

        verify(stateManager).clearState(TELEGRAM_ID);
        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().contains("allaqachon javob bergansiz"));
    }

    // ---------------- isInSituational ----------------

    @org.junit.jupiter.api.Test
    void isInSituational_trueForBothActiveStates() {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.IN_SITUATIONAL);
        assertThat(handler.isInSituational(TELEGRAM_ID)).isTrue();

        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.SITUATIONAL_CONFIRM_ANSWER);
        assertThat(handler.isInSituational(TELEGRAM_ID)).isTrue();

        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.IDLE);
        assertThat(handler.isInSituational(TELEGRAM_ID)).isFalse();
    }
}

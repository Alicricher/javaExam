package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.Test;
import com.dentistrybot.shared.model.TestAnswer;
import com.dentistrybot.shared.model.TestResult;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.repository.TestRepository;
import com.dentistrybot.shared.service.FileService;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.service.TestService;
import com.dentistrybot.shared.state.CachedOptionData;
import com.dentistrybot.shared.state.CachedQuestionData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.TestStateData;
import com.dentistrybot.shared.state.UserState;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
import org.junit.jupiter.api.BeforeEach;

import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Первый набор тестов для studentbot-обработчиков — задаёт паттерн для остальных
 * (RegistrationHandler, LessonHandler, TheoryHandler, SituationalHandler, ProfileHandler):
 * мокаем TelegramClient + репозитории/сервисы, кормим обработчику реальный CallbackQuery,
 * проверяем какие BotApiMethod реально ушли в bot.execute(...) и как изменилось состояние.
 */
class TestHandlerTest {

    private static final long TELEGRAM_ID = 111222333L;
    private static final long CHAT_ID = 111222333L;
    private static final int MESSAGE_ID = 555;

    private TelegramClient bot;
    private StateManager stateManager;
    private TestService testService;
    private TestRepository testRepository;
    private ResultRepository resultRepository;
    private StudentRepository studentRepository;
    private FileService fileService;
    private TestHandler handler;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramClient.class);
        stateManager = mock(StateManager.class);
        testService = mock(TestService.class);
        testRepository = mock(TestRepository.class);
        resultRepository = mock(ResultRepository.class);
        studentRepository = mock(StudentRepository.class);
        fileService = mock(FileService.class);
        handler = new TestHandler(bot, stateManager, testService, testRepository, resultRepository, studentRepository, fileService);
    }

    private CallbackQuery callbackWithData(String data) {
        Chat chat = Chat.builder().id(CHAT_ID).type("private").build();
        Message message = Message.builder().messageId(MESSAGE_ID).chat(chat).build();
        User from = User.builder().id(TELEGRAM_ID).firstName("QA").isBot(false).build();
        CallbackQuery cq = new CallbackQuery();
        cq.setId("cbq-1");
        cq.setFrom(from);
        cq.setMessage(message);
        cq.setData(data);
        return cq;
    }

    /** Достаёт все вызовы bot.execute(...) данного типа — execute() один и тот же
     * generic-метод для всех BotApiMethod, поэтому captor должен собрать всё подряд,
     * а фильтровать по типу приходится вручную. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<T> executedOf(Class<T> type) throws Exception {
        ArgumentCaptor<BotApiMethod> captor = ArgumentCaptor.forClass(BotApiMethod.class);
        verify(bot, atLeastOnce()).execute(captor.capture());
        return captor.getAllValues().stream().filter(type::isInstance).map(type::cast).toList();
    }

    private Student student() {
        Student s = new Student();
        s.setId(42);
        s.setTelegramId(TELEGRAM_ID);
        return s;
    }

    // ---------------- handleTestCallback ----------------

    @org.junit.jupiter.api.Test
    void testCallback_noTestForLesson_showsNoTestMessage() throws Exception {
        when(testRepository.getTestByLessonId(7)).thenReturn(null);

        handler.handleTestCallback(callbackWithData(StudentKeyboards.CB_TEST + "7"));

        verify(bot).execute(any(AnswerCallbackQuery.class));
        List<EditMessageText> edits = executedOf(EditMessageText.class);
        assertThat(edits).hasSize(1);
        assertThat(edits.get(0).getText()).isEqualTo(UzMessages.MSG_NO_TEST_AVAILABLE);
        assertThat(edits.get(0).getChatId()).isEqualTo(String.valueOf(CHAT_ID));
    }

    @org.junit.jupiter.api.Test
    void testCallback_studentNotFound_stopsAfterAnsweringCallback() throws Exception {
        Test test = new Test();
        test.setId(1);
        when(testRepository.getTestByLessonId(7)).thenReturn(test);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(null);

        handler.handleTestCallback(callbackWithData(StudentKeyboards.CB_TEST + "7"));

        verify(bot).execute(any(AnswerCallbackQuery.class));
        verify(bot, never()).execute(any(EditMessageText.class));
    }

    @org.junit.jupiter.api.Test
    void testCallback_alreadyTookTestNoRetake_showsBlockedMessage() throws Exception {
        Test test = new Test();
        test.setId(1);
        when(testRepository.getTestByLessonId(7)).thenReturn(test);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        when(testService.canTakeTest(42, 1)).thenReturn(new String[]{"false", "no_retake"});

        handler.handleTestCallback(callbackWithData(StudentKeyboards.CB_TEST + "7"));

        List<EditMessageText> edits = executedOf(EditMessageText.class);
        assertThat(edits).hasSize(1);
        assertThat(edits.get(0).getText())
            .isEqualTo(UzMessages.MSG_ALREADY_TOOK_TEST + "\n\n" + UzMessages.MSG_NO_RETAKE_AVAILABLE);
    }

    @org.junit.jupiter.api.Test
    void testCallback_firstAttempt_showsStartMessageWithoutRetakePrefix() throws Exception {
        Test test = new Test();
        test.setId(1);
        test.setTitleUz("Tish anatomiyasi");
        test.setTimeLimitMinutes(15);
        test.setTotalPoints(10);
        when(testRepository.getTestByLessonId(7)).thenReturn(test);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        when(testService.canTakeTest(42, 1)).thenReturn(new String[]{"true", "first_attempt"});
        when(testService.getTotalQuestionsCount(1)).thenReturn(10);

        handler.handleTestCallback(callbackWithData(StudentKeyboards.CB_TEST + "7"));

        List<EditMessageText> edits = executedOf(EditMessageText.class);
        String expected = String.format(UzMessages.MSG_TEST_START, "Tish anatomiyasi", 10, 15, 10);
        assertThat(edits.get(0).getText()).isEqualTo(expected);
        assertThat(edits.get(0).getText()).doesNotContain(UzMessages.MSG_RETAKE_AVAILABLE);
    }

    @org.junit.jupiter.api.Test
    void testCallback_russianStudent_showsMessageInRussian() throws Exception {
        Test test = new Test();
        test.setId(1);
        test.setTitleUz("Tish anatomiyasi");
        test.setTimeLimitMinutes(15);
        test.setTotalPoints(10);
        Student ruStudent = student();
        ruStudent.setLanguage("ru");
        when(testRepository.getTestByLessonId(7)).thenReturn(test);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(ruStudent);
        when(testService.canTakeTest(42, 1)).thenReturn(new String[]{"true", "first_attempt"});
        when(testService.getTotalQuestionsCount(1)).thenReturn(10);

        handler.handleTestCallback(callbackWithData(StudentKeyboards.CB_TEST + "7"));

        List<EditMessageText> edits = executedOf(EditMessageText.class);
        assertThat(edits.get(0).getText())
            .startsWith("Тест: Tish anatomiyasi")
            .contains("Количество вопросов: 10", "Время: 15 мин", "Всего баллов: 10", "Начать тест?")
            .doesNotContain("Savollar soni", "Testni boshlashni tasdiqlaysizmi");
    }

    @org.junit.jupiter.api.Test
    void testCallback_retakeAvailable_prependsRetakeNotice() throws Exception {
        Test test = new Test();
        test.setId(1);
        test.setTitleUz("Tish anatomiyasi");
        test.setTimeLimitMinutes(15);
        test.setTotalPoints(10);
        when(testRepository.getTestByLessonId(7)).thenReturn(test);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        when(testService.canTakeTest(42, 1)).thenReturn(new String[]{"true", "retake"});
        when(testService.getTotalQuestionsCount(1)).thenReturn(10);

        handler.handleTestCallback(callbackWithData(StudentKeyboards.CB_TEST + "7"));

        List<EditMessageText> edits = executedOf(EditMessageText.class);
        assertThat(edits.get(0).getText()).startsWith(UzMessages.MSG_RETAKE_AVAILABLE);
    }

    // ---------------- handleTestConfirm ----------------

    @org.junit.jupiter.api.Test
    void testConfirm_alreadyInTest_onlyAnswersCallbackAndStopsThere() throws Exception {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.IN_TEST);

        handler.handleTestConfirm(callbackWithData(StudentKeyboards.CB_CONFIRM + "test:1"));

        verify(bot).execute(any(AnswerCallbackQuery.class));
        verify(bot, never()).execute(any(SendMessage.class));
        verify(stateManager, never()).setStateWithData(eq(TELEGRAM_ID), org.mockito.ArgumentMatchers.anyString(), any());
    }

    @org.junit.jupiter.api.Test
    void testConfirm_cannotTake_onlyAnswersCallback() throws Exception {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.IDLE);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        when(testService.canTakeTest(42, 1)).thenReturn(new String[]{"false", "no_retake"});

        handler.handleTestConfirm(callbackWithData(StudentKeyboards.CB_CONFIRM + "test:1"));

        verify(bot).execute(any(AnswerCallbackQuery.class));
        verify(bot, never()).execute(any(SendMessage.class));
        verify(bot, never()).execute(any(DeleteMessage.class));
    }

    @org.junit.jupiter.api.Test
    void testConfirm_happyPath_startsTestAndShowsFirstQuestion() throws Exception {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.IDLE);
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());
        when(testService.canTakeTest(42, 1)).thenReturn(new String[]{"true", "first_attempt"});

        Test test = new Test();
        test.setId(1);
        test.setLessonId(3);
        test.setTimeLimitMinutes(15);
        when(testRepository.getTestById(1)).thenReturn(test);

        TestResult result = new TestResult();
        result.setId(999);
        result.setStartedAt(LocalDateTime.now());
        when(testService.startTest(42, 1, false)).thenReturn(result);

        CachedOptionData opt = new CachedOptionData();
        opt.setOptionId(11);
        opt.setOptionText("28 ta");
        opt.setCorrect(true);
        CachedQuestionData q1 = new CachedQuestionData();
        q1.setQuestionId(100);
        q1.setQuestionText("Odamda nechta doimiy tish bo'ladi?");
        q1.setOptions(List.of(opt));
        when(testService.loadQuestionsForCaching(eq(1), any())).thenReturn(List.of(q1));
        when(testService.isTimeUp(any(Instant.class), org.mockito.ArgumentMatchers.anyInt())).thenReturn(false);
        when(testService.getRemainingTime(any(Instant.class), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(java.time.Duration.ofMinutes(15));

        handler.handleTestConfirm(callbackWithData(StudentKeyboards.CB_CONFIRM + "test:1"));

        ArgumentCaptor<TestStateData> sdCaptor = ArgumentCaptor.forClass(TestStateData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.IN_TEST), sdCaptor.capture());
        TestStateData sd = sdCaptor.getValue();
        assertThat(sd.getTestId()).isEqualTo(1);
        assertThat(sd.getLessonId()).isEqualTo(3);
        assertThat(sd.getResultId()).isEqualTo(999);
        assertThat(sd.getCurrentQuestion()).isZero();
        assertThat(sd.getTotalQuestions()).isEqualTo(1);

        List<DeleteMessage> deletes = executedOf(DeleteMessage.class);
        assertThat(deletes).hasSize(1);
        assertThat(deletes.get(0).getMessageId()).isEqualTo(MESSAGE_ID);

        List<SendMessage> sent = executedOf(SendMessage.class);
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getText()).contains("Odamda nechta doimiy tish bo'ladi?");
    }

    // ---------------- handleAnswerCallback ----------------

    @org.junit.jupiter.api.Test
    void answerCallback_noActiveState_doesNothing() {
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(null);

        handler.handleAnswerCallback(callbackWithData(StudentKeyboards.CB_ANSWER + "11"));

        verifyNoInteractions(bot);
    }

    @org.junit.jupiter.api.Test
    void answerCallback_timeUp_completesTestAsTimeout() throws Exception {
        UserState us = new UserState();
        TestStateData sd = baseStateData();
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, TestStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(true);

        TestResult completed = new TestResult();
        completed.setId(999);
        completed.setScore(3);
        completed.setMaxScore(10);
        when(testService.completeTest(999, "timeout")).thenReturn(completed);
        when(resultRepository.getTestAnswersByResultId(999)).thenReturn(List.of());

        handler.handleAnswerCallback(callbackWithData(StudentKeyboards.CB_ANSWER + "11"));

        // clearState вызывается дважды на этом пути (в handleAnswerCallback и внутри
        // completeTest) — избыточно, но идемпотентно, поэтому проверяем "хотя бы раз",
        // а не точное число вызовов.
        verify(stateManager, org.mockito.Mockito.atLeastOnce()).clearState(TELEGRAM_ID);
        List<SendMessage> sent = executedOf(SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().contains(UzMessages.MSG_TEST_TIME_UP));
        assertThat(sent).anyMatch(m -> m.getText().contains("3") && m.getText().contains("10"));
    }

    @org.junit.jupiter.api.Test
    void answerCallback_correctAnswer_advancesToNextQuestion() throws Exception {
        UserState us = new UserState();
        TestStateData sd = baseStateData();
        sd.setQuestions(List.of(questionWithOption(11, true), questionWithOption(22, false)));
        sd.setTotalQuestions(2);
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, TestStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(false);
        when(testService.getRemainingTime(any(Instant.class), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(java.time.Duration.ofMinutes(10));

        handler.handleAnswerCallback(callbackWithData(StudentKeyboards.CB_ANSWER + "11"));

        verify(testService).submitAnswerWithCachedCorrectness(999, 100, 11, true);
        ArgumentCaptor<TestStateData> sdCaptor = ArgumentCaptor.forClass(TestStateData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.IN_TEST), sdCaptor.capture());
        assertThat(sdCaptor.getValue().getCurrentQuestion()).isEqualTo(1);

        List<SendMessage> sent = executedOf(SendMessage.class);
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getText()).contains("Savol 2/2");
    }

    @org.junit.jupiter.api.Test
    void answerCallback_lastQuestion_completesTest() throws Exception {
        UserState us = new UserState();
        TestStateData sd = baseStateData();
        sd.setQuestions(List.of(questionWithOption(11, true)));
        sd.setTotalQuestions(1);
        sd.setCurrentQuestion(0);
        sd.setLessonId(3);
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, TestStateData.class)).thenReturn(sd);
        when(testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())).thenReturn(false);

        TestResult completed = new TestResult();
        completed.setId(999);
        completed.setScore(10);
        completed.setMaxScore(10);
        when(testService.completeTest(999, "completed")).thenReturn(completed);
        TestAnswer correctAnswer = new TestAnswer();
        correctAnswer.setCorrect(true);
        when(resultRepository.getTestAnswersByResultId(999)).thenReturn(List.of(correctAnswer));

        handler.handleAnswerCallback(callbackWithData(StudentKeyboards.CB_ANSWER + "11"));

        verify(testService).submitAnswerWithCachedCorrectness(999, 100, 11, true);
        verify(stateManager).clearState(TELEGRAM_ID);
        List<SendMessage> sent = executedOf(SendMessage.class);
        String expected = String.format(UzMessages.MSG_TEST_COMPLETED, 10, 10, 1, 1);
        assertThat(sent).anyMatch(m -> m.getText().equals(expected));
    }

    private TestStateData baseStateData() {
        TestStateData sd = new TestStateData();
        sd.setTestId(1);
        sd.setLessonId(3);
        sd.setResultId(999);
        sd.setCurrentQuestion(0);
        sd.setTotalQuestions(1);
        sd.setStartedAt(Instant.now().minusSeconds(60));
        sd.setTimeLimitMinutes(15);
        sd.setQuestions(List.of(questionWithOption(11, true)));
        return sd;
    }

    private CachedQuestionData questionWithOption(int optionId, boolean correct) {
        CachedOptionData opt = new CachedOptionData();
        opt.setOptionId(optionId);
        opt.setOptionText("variant");
        opt.setCorrect(correct);
        CachedQuestionData q = new CachedQuestionData();
        q.setQuestionId(100);
        q.setQuestionText("savol matni");
        q.setOptions(List.of(opt));
        return q;
    }
}

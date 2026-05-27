package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.AnswerOption;
import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.Test;
import com.dentistrybot.shared.model.TestResult;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.repository.TestRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.service.TestService;
import com.dentistrybot.shared.state.CachedOptionData;
import com.dentistrybot.shared.state.CachedQuestionData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.TestStateData;
import com.dentistrybot.shared.state.UserState;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TestHandler {

    private static final Logger log = LoggerFactory.getLogger(TestHandler.class);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final TestService testService;
    private final TestRepository testRepository;
    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;

    public TestHandler(TelegramClient bot, StateManager stateManager, TestService testService,
                       TestRepository testRepository, ResultRepository resultRepository,
                       StudentRepository studentRepository) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.testService = testService;
        this.testRepository = testRepository;
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
    }

    public void handleTestCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring(StudentKeyboards.CB_TEST.length()));
            Test test = testRepository.getTestByLessonId(lessonId);
            answerCallback(callback.getId());

            if (test == null) {
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(UzMessages.MSG_NO_TEST_AVAILABLE)
                    .replyMarkup(backToLesson(lessonId)).build());
                return;
            }

            Student student = studentRepository.getStudentByTelegramId(telegramId);
            if (student == null) return;

            String[] canTake = testService.canTakeTest(student.getId(), test.getId());
            boolean ok = "true".equals(canTake[0]);
            String reason = canTake[1];

            if (!ok) {
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId)
                    .text(UzMessages.MSG_ALREADY_TOOK_TEST + "\n\n" + UzMessages.MSG_NO_RETAKE_AVAILABLE)
                    .replyMarkup(backToLesson(lessonId)).build());
                return;
            }

            int qCount = testService.getTotalQuestionsCount(test.getId());
            String prefix = "retake".equals(reason) ? UzMessages.MSG_RETAKE_AVAILABLE + "\n\n" : "";
            String text = prefix + String.format(UzMessages.MSG_TEST_START,
                test.getTitleUz(), qCount, test.getTimeLimitMinutes(), test.getTotalPoints());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(StudentKeyboards.testConfirm(test.getId())).build());
        } catch (Exception e) {
            log.error("handleTestCallback error: {}", e.getMessage());
        }
    }

    public void handleTestConfirm(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            if (isInTest(telegramId)) { answerCallback(callback.getId()); return; }

            String suffix = callback.getData().substring((StudentKeyboards.CB_CONFIRM + "test:").length());
            int testId = Integer.parseInt(suffix);
            Student student = studentRepository.getStudentByTelegramId(telegramId);
            if (student == null) return;

            String[] canTake = testService.canTakeTest(student.getId(), testId);
            if (!"true".equals(canTake[0])) {
                answerCallback(callback.getId());
                return;
            }

            boolean isRetake = "retake".equals(canTake[1]);
            TestResult result = testService.startTest(student.getId(), testId, isRetake);
            Test test = testRepository.getTestById(testId);
            List<CachedQuestionData> questions = testService.loadQuestionsForCaching(testId);

            TestStateData sd = new TestStateData();
            sd.setTestId(testId);
            sd.setLessonId(test.getLessonId());
            sd.setResultId(result.getId());
            sd.setCurrentQuestion(0);
            sd.setTotalQuestions(questions.size());
            sd.setStartedAt(result.getStartedAt().toInstant(java.time.ZoneOffset.UTC));
            sd.setTimeLimitMinutes(test.getTimeLimitMinutes());
            sd.setQuestions(questions);

            stateManager.setStateWithData(telegramId, StateConstants.IN_TEST, sd);
            answerCallback(callback.getId());
            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
            showQuestion(chatId, telegramId, sd);
        } catch (Exception e) {
            log.error("handleTestConfirm error: {}", e.getMessage());
        }
    }

    public void handleAnswerCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            TestStateData sd = stateManager.getStateData(us, TestStateData.class);
            if (sd == null) return;

            if (testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())) {
                stateManager.clearState(telegramId);
                bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).text("⏰ Vaqt tugadi!").build());
                bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                sendText(chatId, "⏰ Vaqt tugadi! Test avtomatik yakunlandi.");
                completeTest(chatId, telegramId, sd, "timeout");
                return;
            }

            // parse "ans:optionId"
            String[] parts = callback.getData().substring(StudentKeyboards.CB_ANSWER.length()).split(":");
            int optionId = Integer.parseInt(parts[parts.length == 2 ? 1 : 0]);

            if (sd.getCurrentQuestion() >= sd.getQuestions().size()) {
                completeTest(chatId, telegramId, sd, "completed");
                return;
            }

            CachedQuestionData cq = sd.getQuestions().get(sd.getCurrentQuestion());
            boolean isCorrect = cq.getOptions().stream()
                .filter(o -> o.getOptionId() == optionId)
                .map(CachedOptionData::isCorrect).findFirst().orElse(false);

            testService.submitAnswerWithCachedCorrectness(sd.getResultId(), cq.getQuestionId(), optionId, isCorrect);
            answerCallback(callback.getId());
            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

            sd.setCurrentQuestion(sd.getCurrentQuestion() + 1);
            stateManager.setStateWithData(telegramId, StateConstants.IN_TEST, sd);
            showQuestion(chatId, telegramId, sd);
        } catch (Exception e) {
            log.error("handleAnswerCallback error: {}", e.getMessage());
        }
    }

    public boolean isInTest(long telegramId) {
        return StateConstants.IN_TEST.equals(stateManager.getState(telegramId));
    }

    public boolean checkTestTimeout(long telegramId) {
        UserState us = stateManager.getStateWithData(telegramId);
        if (us == null || !StateConstants.IN_TEST.equals(us.getState())) return false;
        TestStateData sd = stateManager.getStateData(us, TestStateData.class);
        return sd != null && testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes());
    }

    public void forceCompleteTest(long chatId, long telegramId) {
        UserState us = stateManager.getStateWithData(telegramId);
        if (us == null) return;
        TestStateData sd = stateManager.getStateData(us, TestStateData.class);
        if (sd == null) return;
        completeTest(chatId, telegramId, sd, "timeout");
    }

    private void showQuestion(long chatId, long telegramId, TestStateData sd) {
        try {
            if (testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())) {
                stateManager.clearState(telegramId);
                sendText(chatId, "⏰ Vaqt tugadi! Test avtomatik yakunlandi.");
                completeTest(chatId, telegramId, sd, "timeout");
                return;
            }

            if (sd.getCurrentQuestion() >= sd.getQuestions().size()) {
                completeTest(chatId, telegramId, sd, "completed");
                return;
            }

            if (testService.shouldShowWarning(sd.getStartedAt(), sd.getTimeLimitMinutes())) {
                sendText(chatId, UzMessages.MSG_TEST_TIME_WARNING);
            }

            CachedQuestionData cq = sd.getQuestions().get(sd.getCurrentQuestion());
            long sec = testService.getRemainingTime(sd.getStartedAt(), sd.getTimeLimitMinutes()).getSeconds();
            String timeStr = String.format("%d:%02d", sec / 60, sec % 60);

            String text = String.format(UzMessages.MSG_TEST_QUESTION,
                sd.getCurrentQuestion() + 1, sd.getTotalQuestions(), cq.getQuestionText())
                + "\n\nQolgan vaqt: " + timeStr;

            List<AnswerOption> options = new ArrayList<>();
            for (CachedOptionData o : cq.getOptions()) {
                AnswerOption ao = new AnswerOption();
                ao.setId(o.getOptionId());
                ao.setOptionText(o.getOptionText());
                options.add(ao);
            }

            bot.execute(SendMessage.builder()
                .chatId(chatId).text(text)
                .replyMarkup(StudentKeyboards.answerOptions(options))
                .build());
        } catch (Exception e) {
            log.error("showQuestion error: {}", e.getMessage());
        }
    }

    private void completeTest(long chatId, long telegramId, TestStateData sd, String status) {
        try {
            TestResult result = testService.completeTest(sd.getResultId(), status);
            int correctCount = resultRepository.getTestAnswersByResultId(sd.getResultId()).stream()
                .mapToInt(a -> a.isCorrect() ? 1 : 0).sum();
            stateManager.clearState(telegramId);

            String prefix = "timeout".equals(status) ? UzMessages.MSG_TEST_TIME_UP + "\n\n" : "";
            String text = prefix + String.format(UzMessages.MSG_TEST_COMPLETED,
                result.getScore(), result.getMaxScore(), correctCount, sd.getTotalQuestions());

            InlineKeyboardMarkup kb = sd.getLessonId() != 0
                ? backToLesson(sd.getLessonId()) : null;

            SendMessage msg = SendMessage.builder().chatId(chatId).text(text).build();
            if (kb != null) msg.setReplyMarkup(kb);
            else msg.setReplyMarkup(StudentKeyboards.mainMenu());
            bot.execute(msg);
        } catch (Exception e) {
            log.error("completeTest error: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup backToLesson(int lessonId) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text(UzMessages.BTN_BACK)
                .callbackData(StudentKeyboards.CB_BACK + "lesson:" + lessonId)
                .build()))
            .build();
    }

    private void sendText(long chatId, String text) {
        try { bot.execute(SendMessage.builder().chatId(chatId).text(text).build()); }
        catch (Exception e) { log.error("sendText: {}", e.getMessage()); }
    }

    private void answerCallback(String id) {
        try { bot.execute(AnswerCallbackQuery.builder().callbackQueryId(id).build()); }
        catch (Exception e) { log.error("answerCallback: {}", e.getMessage()); }
    }
}

package com.dentistrybot.student.bot;

import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.SituationalStateData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import com.dentistrybot.student.handler.*;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class StudentUpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(StudentUpdateDispatcher.class);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final StudentRepository studentRepository;
    private final RegistrationHandler registrationHandler;
    private final ProfileHandler profileHandler;
    private final LessonHandler lessonHandler;
    private final TestHandler testHandler;
    private final TheoryHandler theoryHandler;
    private final SituationalHandler situationalHandler;

    private final ThreadPoolExecutor executor;
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> lockLastUsed = new ConcurrentHashMap<>();

    public StudentUpdateDispatcher(TelegramClient bot, StateManager stateManager,
                                   StudentRepository studentRepository,
                                   RegistrationHandler registrationHandler,
                                   ProfileHandler profileHandler,
                                   LessonHandler lessonHandler,
                                   TestHandler testHandler,
                                   TheoryHandler theoryHandler,
                                   SituationalHandler situationalHandler,
                                   int workers, int queueSize) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.studentRepository = studentRepository;
        this.registrationHandler = registrationHandler;
        this.profileHandler = profileHandler;
        this.lessonHandler = lessonHandler;
        this.testHandler = testHandler;
        this.theoryHandler = theoryHandler;
        this.situationalHandler = situationalHandler;
        this.executor = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(queueSize), new ThreadPoolExecutor.DiscardPolicy());
    }

    public void dispatch(Update update) {
        long userId = getUserId(update);
        if (userId == 0) return;
        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock(true));
        executor.submit(() -> {
            lock.lock();
            try {
                lockLastUsed.put(userId, System.currentTimeMillis());
                if (update.hasCallbackQuery()) handleCallback(update.getCallbackQuery());
                else if (update.hasMessage()) handleMessage(update.getMessage());
            } finally {
                lock.unlock();
            }
        });
    }

    // Periodically called by @Scheduled in the application to clean up idle locks
    public void cleanupLocks(long ttlMs) {
        long cutoff = System.currentTimeMillis() - ttlMs;
        userLocks.entrySet().removeIf(entry -> {
            Long lastUsed = lockLastUsed.get(entry.getKey());
            if (lastUsed != null && lastUsed < cutoff) {
                ReentrantLock lock = entry.getValue();
                if (lock.tryLock()) {
                    try {
                        lockLastUsed.remove(entry.getKey());
                        return true;
                    } finally {
                        lock.unlock();
                    }
                }
            }
            return false;
        });
    }

    private void handleMessage(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();

        boolean exists = studentRepository.studentExists(telegramId);

        if (message.getText() != null && message.getText().startsWith("/start")) {
            if (!exists) registrationHandler.startRegistration(chatId, telegramId);
            else {
                var student = studentRepository.getStudentByTelegramId(telegramId);
                String text = student != null
                    ? String.format(UzMessages.MSG_WELCOME_BACK, student.getFullName())
                    : UzMessages.MSG_WELCOME;
                try {
                    bot.execute(SendMessage.builder().chatId(chatId).text(text)
                        .replyMarkup(StudentKeyboards.mainMenu()).build());
                } catch (Exception e) { log.error("start message error: {}", e.getMessage()); }
            }
            return;
        }

        if (!exists) {
            if (registrationHandler.isInRegistration(telegramId)) registrationHandler.handleRegistrationStep(message);
            else registrationHandler.startRegistration(chatId, telegramId);
            return;
        }

        UserState userState = stateManager.getStateWithData(telegramId);
        String state = userState != null ? userState.getState() : StateConstants.IDLE;

        if (StateConstants.IN_TEST.equals(state)) {
            if (testHandler.checkTestTimeout(telegramId)) testHandler.forceCompleteTest(chatId, telegramId);
            else sendText(chatId, "Test jarayonida. Iltimos, testni yakunlang.");
            return;
        }

        if (StateConstants.IN_SITUATIONAL.equals(state) || StateConstants.SITUATIONAL_CONFIRM_ANSWER.equals(state)) {
            boolean timedOut = false;
            if (userState != null) {
                SituationalStateData sd = stateManager.getStateData(userState, SituationalStateData.class);
                if (sd != null) {
                    timedOut = Instant.now().isAfter(sd.getStartedAt().plusSeconds((long) sd.getTimeLimitMinutes() * 60));
                }
            }
            if (timedOut) situationalHandler.forceCompleteSituational(chatId, telegramId);
            else if (message.getText() != null) situationalHandler.handleSituationalAnswer(message);
            else if (message.getPhoto() != null) sendText(chatId, "Faqat matn javobini yuboring. Rasm qabul qilinmaydi.");
            return;
        }

        switch (state) {
            case StateConstants.EDIT_COURSE, StateConstants.EDIT_GROUP,
                 StateConstants.EDIT_SUBGROUP, StateConstants.EDIT_FACULTY ->
                profileHandler.handleProfileEditText(message);
            case StateConstants.REGISTER_FULL_NAME, StateConstants.REGISTER_COURSE,
                 StateConstants.REGISTER_GROUP, StateConstants.REGISTER_SUBGROUP,
                 StateConstants.REGISTER_FACULTY ->
                registrationHandler.handleRegistrationStep(message);
            default -> {
                if (UzMessages.BTN_START_LEARNING.equals(message.getText())) lessonHandler.showUnits(chatId);
                else if (UzMessages.BTN_PROFILE.equals(message.getText())) profileHandler.showProfile(chatId, telegramId);
                else if (StateConstants.IDLE.equals(state) || state.isEmpty()) {
                    try {
                        bot.execute(SendMessage.builder().chatId(chatId).text(UzMessages.MSG_MAIN_MENU)
                            .replyMarkup(StudentKeyboards.mainMenu()).build());
                    } catch (Exception e) { log.error("default menu error: {}", e.getMessage()); }
                }
            }
        }
    }

    private void handleCallback(CallbackQuery callback) {
        String data = callback.getData();
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();

        boolean exists = studentRepository.studentExists(telegramId);
        boolean isRegCallback = registrationHandler.isInRegistration(telegramId) &&
            (data.startsWith(StudentKeyboards.CB_COURSE) || data.startsWith(StudentKeyboards.CB_GROUP) ||
             data.startsWith(StudentKeyboards.CB_SUBGROUP) || data.startsWith(StudentKeyboards.CB_FACULTY));

        if (!exists && !isRegCallback) {
            try { bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId())
                .text("Iltimos, avval ro'yxatdan o'ting.").build()); }
            catch (Exception e) { log.error("answerCallback error: {}", e.getMessage()); }
            return;
        }

        if (StudentKeyboards.CB_CANCEL.equals(data)) {
            stateManager.clearState(telegramId);
            try {
                bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).build());
                bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                bot.execute(SendMessage.builder().chatId(chatId).text(UzMessages.MSG_MAIN_MENU)
                    .replyMarkup(StudentKeyboards.mainMenu()).build());
            } catch (Exception e) { log.error("cancel callback error: {}", e.getMessage()); }
            return;
        }

        if (data.startsWith(StudentKeyboards.CB_UNIT)) lessonHandler.handleUnitCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_LESSON)) lessonHandler.handleLessonCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_BACK)) lessonHandler.handleBackCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_TEST)) testHandler.handleTestCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_CONFIRM + "test:")) testHandler.handleTestConfirm(callback);
        else if (data.startsWith(StudentKeyboards.CB_ANSWER)) testHandler.handleAnswerCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_THEORY)) theoryHandler.handleTheoryCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_MAT_TYPE)) theoryHandler.handleMaterialTypeCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_MATERIAL)) theoryHandler.handleMaterialCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_SIT_TASK)) situationalHandler.handleTaskSelectCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_SIT)) situationalHandler.handleSituationalCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_CONFIRM + "sit:")) situationalHandler.handleSituationalConfirm(callback);
        else if ("confirm_situational_answer".equals(data)) situationalHandler.handleConfirmAnswer(callback);
        else if ("edit_situational_answer".equals(data)) situationalHandler.handleEditAnswer(callback);
        else if (data.startsWith(StudentKeyboards.CB_EDIT_PROFILE)) profileHandler.handleEditProfileCallback(callback);
        else if (data.startsWith(StudentKeyboards.CB_COURSE)) {
            int course = Integer.parseInt(data.substring(StudentKeyboards.CB_COURSE.length()));
            String state = stateManager.getState(telegramId);
            if (StateConstants.REGISTER_COURSE.equals(state)) registrationHandler.handleCourseCallback(callback, course);
            else if (StateConstants.EDIT_COURSE.equals(state)) profileHandler.handleEditCourseCallback(callback, course);
        }
        else if (data.startsWith(StudentKeyboards.CB_GROUP)) {
            String group = data.substring(StudentKeyboards.CB_GROUP.length());
            String state = stateManager.getState(telegramId);
            if (StateConstants.REGISTER_GROUP.equals(state)) registrationHandler.handleGroupCallback(callback, group);
            else if (StateConstants.EDIT_GROUP.equals(state)) profileHandler.handleEditGroupCallback(callback, group);
        }
        else if (data.startsWith(StudentKeyboards.CB_SUBGROUP)) {
            String sub = data.substring(StudentKeyboards.CB_SUBGROUP.length());
            String state = stateManager.getState(telegramId);
            if (StateConstants.REGISTER_SUBGROUP.equals(state)) registrationHandler.handleSubgroupCallback(callback, sub);
            else if (StateConstants.EDIT_SUBGROUP.equals(state)) profileHandler.handleEditSubgroupCallback(callback, sub);
        }
        else if (data.startsWith(StudentKeyboards.CB_FACULTY)) {
            String fac = data.substring(StudentKeyboards.CB_FACULTY.length());
            String state = stateManager.getState(telegramId);
            if (StateConstants.REGISTER_FACULTY.equals(state)) registrationHandler.handleFacultyCallback(callback, fac);
            else if (StateConstants.EDIT_FACULTY.equals(state)) profileHandler.handleEditFacultyCallback(callback, fac);
        }
        else {
            try { bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).build()); }
            catch (Exception e) { log.error("default callback answer error: {}", e.getMessage()); }
        }
    }

    private long getUserId(Update update) {
        if (update.hasCallbackQuery()) return update.getCallbackQuery().getFrom().getId();
        if (update.hasMessage() && update.getMessage().getFrom() != null) return update.getMessage().getFrom().getId();
        return 0;
    }

    private void sendText(long chatId, String text) {
        try { bot.execute(SendMessage.builder().chatId(chatId).text(text).build()); }
        catch (Exception e) { log.error("sendText: {}", e.getMessage()); }
    }
}

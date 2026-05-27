package com.dentistrybot.admin.bot;

import com.dentistrybot.admin.handler.AuthHandler;
import com.dentistrybot.admin.handler.ManagementHandler;
import com.dentistrybot.admin.handler.ResultsHandler;
import com.dentistrybot.admin.handler.StudentsHandler;
import com.dentistrybot.admin.keyboard.AdminKeyboards;
import com.dentistrybot.admin.localization.AdminMessages;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.StateConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class AdminUpdateDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AdminUpdateDispatcher.class);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final AuthHandler authHandler;
    private final StudentsHandler studentsHandler;
    private final ResultsHandler resultsHandler;
    private final ManagementHandler managementHandler;

    private final ExecutorService pool;
    private final ConcurrentHashMap<Long, ReentrantLock> userLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> lockLastUsed = new ConcurrentHashMap<>();

    public AdminUpdateDispatcher(TelegramClient bot, StateManager stateManager,
                                  AuthHandler authHandler, StudentsHandler studentsHandler,
                                  ResultsHandler resultsHandler, ManagementHandler managementHandler,
                                  int workers, int queueSize) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.authHandler = authHandler;
        this.studentsHandler = studentsHandler;
        this.resultsHandler = resultsHandler;
        this.managementHandler = managementHandler;
        this.pool = new ThreadPoolExecutor(workers, workers, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadPoolExecutor.DiscardPolicy());
    }

    public void dispatch(Update update) {
        long userId;
        if (update.hasCallbackQuery()) {
            userId = update.getCallbackQuery().getFrom().getId();
        } else if (update.hasMessage()) {
            User from = update.getMessage().getFrom();
            if (from == null) return;
            userId = from.getId();
        } else {
            return;
        }

        ReentrantLock lock = userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
        lockLastUsed.put(userId, System.currentTimeMillis());

        pool.submit(() -> {
            lock.lock();
            try {
                if (update.hasCallbackQuery()) {
                    handleCallback(update.getCallbackQuery());
                } else if (update.hasMessage()) {
                    handleMessage(update.getMessage());
                }
            } finally {
                lock.unlock();
            }
        });
    }

    public void cleanupLocks(long ttlMs) {
        long now = System.currentTimeMillis();
        userLocks.forEach((userId, lock) -> {
            Long lastUsed = lockLastUsed.get(userId);
            if (lastUsed != null && now - lastUsed > ttlMs) {
                if (lock.tryLock()) {
                    try {
                        userLocks.remove(userId);
                        lockLastUsed.remove(userId);
                    } finally {
                        lock.unlock();
                    }
                }
            }
        });
    }

    private void handleMessage(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();

        if (message.getText() != null && message.getText().startsWith("/start")) {
            authHandler.handleStart(chatId, telegramId);
            return;
        }

        if (!authHandler.isAuthenticated(telegramId)) {
            if (authHandler.isInLoginState(telegramId)) {
                authHandler.handlePasswordInput(message);
            } else {
                authHandler.handleStart(chatId, telegramId);
            }
            return;
        }

        if (authHandler.isInLoginState(telegramId)) {
            authHandler.handlePasswordInput(message);
            return;
        }

        if (studentsHandler.isEditingStudent(telegramId)) {
            String state = stateManager.getState(telegramId);
            if (StateConstants.EDIT_STUDENT_NAME.equals(state)) {
                studentsHandler.handleNameEditInput(message);
            } else {
                studentsHandler.handleFilterInput(message);
            }
            return;
        }

        boolean[] gradingState = resultsHandler.isManualGrading(telegramId);
        if (gradingState[0]) {
            if (gradingState[1]) {
                resultsHandler.handleManualGradeInput(message);
            } else {
                resultsHandler.handleManualGradeFeedbackInput(message);
            }
            return;
        }

        if (resultsHandler.isFilteringResults(telegramId)) {
            String state = stateManager.getState(telegramId);
            if (state != null && state.startsWith("filter_situational")) {
                resultsHandler.handleFilterSituationalInput(message);
            } else {
                resultsHandler.handleFilterResultsInput(message);
            }
            return;
        }

        if (managementHandler.isManaging(telegramId)) {
            String state = stateManager.getState(telegramId);
            handleManagementInput(message, state);
            return;
        }

        authHandler.showMainMenu(chatId);
    }

    private void handleManagementInput(Message message, String state) {
        if (state == null) return;
        long chatId = message.getChatId();

        if (message.getDocument() != null) {
            if (state.endsWith("_import")) {
                managementHandler.handleQuestionFileUpload(message);
            } else if (state.endsWith("_upload")) {
                managementHandler.handleTheoryFileUpload(message);
            } else {
                sendText(chatId, "Hozir fayl qabul qilinmaydi.");
            }
            return;
        }

        if (state.endsWith("_import")) {
            sendText(chatId, "Iltimos, Excel (.xlsx) yoki CSV fayl yuboring.");
            return;
        }
        if (state.endsWith("_upload")) {
            sendText(chatId, "Iltimos, fayl yuboring.");
            return;
        }

        if (state.endsWith("_edit_title") && state.startsWith("manage_lessons")) {
            managementHandler.handleEditLessonTitleInput(message);
        } else if (state.endsWith("_title") && state.startsWith("manage_lessons")) {
            managementHandler.handleLessonTitleInput(message);
        } else if (state.endsWith("_time") && state.startsWith("manage_tests")) {
            managementHandler.handleTestTimeInput(message);
        } else if (state.endsWith("_edit_time") && state.startsWith("manage_tests")) {
            managementHandler.handleTestTimeInput(message);
        } else if (state.endsWith("_edit_time") && state.startsWith("manage_tasks")) {
            managementHandler.handleEditTaskTimeInput(message);
        } else if (state.endsWith("_edit_q_text")) {
            managementHandler.handleQuestionTextInput(message);
        } else if (state.endsWith("_edit_q_points")) {
            managementHandler.handleQuestionPointsInput(message);
        } else if (state.endsWith("_edit_option")) {
            managementHandler.handleOptionTextInput(message);
        } else if (state.endsWith("_title") && state.startsWith("manage_theory")) {
            managementHandler.handleTheoryTitleInput(message);
        } else if (state.endsWith("_desc") && state.startsWith("manage_theory")) {
            managementHandler.handleTheoryTitleInput(message);
        } else if (state.endsWith("_edit_title") && state.startsWith("manage_theory")) {
            managementHandler.handleTheoryEditInput(message);
        } else if (state.endsWith("_edit_text") && state.startsWith("manage_theory")) {
            managementHandler.handleTheoryEditInput(message);
        } else if (state.endsWith("_time") && state.startsWith("manage_tasks")) {
            managementHandler.handleTaskTimeInput(message);
        } else if (state.endsWith("_edit_text") && state.startsWith("manage_tasks")) {
            managementHandler.handleEditTaskTextInput(message);
        } else if (state.endsWith("_text") && state.startsWith("manage_tasks")) {
            managementHandler.handleTaskTextInput(message);
        } else if ((StateConstants.MANAGE_UNITS + "_add_name").equals(state)) {
            managementHandler.handleAddUnitNameInput(message);
        } else if ((StateConstants.MANAGE_UNITS + "_add_title").equals(state)) {
            managementHandler.handleAddUnitTitleInput(message);
        } else if ((StateConstants.MANAGE_UNITS + "_edit_name").equals(state)) {
            managementHandler.handleEditUnitNameInput(message);
        } else if ((StateConstants.MANAGE_UNITS + "_edit_title").equals(state)) {
            managementHandler.handleEditUnitTitleInput(message);
        }
    }

    private void handleCallback(CallbackQuery callback) {
        String data = callback.getData();
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();

        if (!authHandler.isAuthenticated(telegramId)) {
            try { bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId())
                    .text("Iltimos, avval tizimga kiring.").build()); } catch (Exception ignored) {}
            return;
        }

        if (AdminKeyboards.CB_CANCEL.equals(data)) {
            handleCancel(callback, telegramId, chatId, messageId);
            return;
        }

        if (data == null) return;

        if (data.startsWith(AdminKeyboards.CB_ADMIN_MENU)) {
            String action = data.substring(AdminKeyboards.CB_ADMIN_MENU.length());
            switch (action) {
                case "students" -> studentsHandler.showStudentList(callback);
                case "test_results" -> resultsHandler.showTestResults(callback, 1);
                case "situational" -> resultsHandler.showSituationalAnswers(callback, 1);
                case "content" -> managementHandler.showContentManagement(callback);
                case "logout" -> authHandler.handleLogout(callback);
            }
        } else if (data.startsWith(AdminKeyboards.CB_BACK)) {
            String target = data.substring(AdminKeyboards.CB_BACK.length());
            switch (target) {
                case "admin_menu" -> managementHandler.handleBackToAdminMenu(callback);
                case "students" -> studentsHandler.showStudentList(callback);
            }
        } else if (data.startsWith(AdminKeyboards.CB_FILTER)) {
            studentsHandler.handleFilterCallback(callback);
        } else if (data.startsWith("student:view:")) {
            studentsHandler.handleStudentViewCallback(callback);
        } else if (data.startsWith("student:edit_name:")) {
            studentsHandler.handleEditNameCallback(callback);
        } else if (data.startsWith("student:sit_retake:")) {
            studentsHandler.handleSitRetakeCallback(callback);
        } else if (data.startsWith("student:retake:")) {
            studentsHandler.handleRetakeCallback(callback);
        } else if (data.startsWith("retake:sit:")) {
            studentsHandler.handleSitRetakeGrantCallback(callback);
        } else if (data.startsWith(AdminKeyboards.CB_RETAKE)) {
            studentsHandler.handleRetakeGrantCallback(callback);
        } else if (data.startsWith("filter_results:")) {
            resultsHandler.handleFilterResultsCallback(callback);
        } else if (data.startsWith("filter_situational:")) {
            resultsHandler.handleFilterSituationalCallback(callback);
        } else if (data.startsWith("student_results:")) {
            resultsHandler.showStudentResults(callback);
        } else if (data.startsWith("test_detail:")) {
            resultsHandler.showStudentTestDetail(callback);
        } else if (data.startsWith("grant_retake:")) {
            resultsHandler.handleGrantRetake(callback);
        } else if (data.startsWith(AdminKeyboards.CB_MANAGE)) {
            managementHandler.handleManageCallback(callback);
        } else if ("add_lesson".equals(data)) {
            managementHandler.handleAddLessonCallback(callback);
        } else if (data.startsWith("edit_unit:")) {
            managementHandler.handleEditUnitCallback(callback);
        } else if ("add_unit".equals(data)) {
            managementHandler.handleAddUnitCallback(callback);
        } else if (data.startsWith("edit_unit_name:")) {
            managementHandler.handleEditUnitNameCallback(callback);
        } else if (data.startsWith("edit_unit_title:")) {
            managementHandler.handleEditUnitTitleCallback(callback);
        } else if (data.startsWith("delete_unit_confirm:")) {
            managementHandler.handleDeleteUnitConfirmCallback(callback);
        } else if (data.startsWith("delete_unit_yes:")) {
            managementHandler.handleDeleteUnitYesCallback(callback);
        } else if (data.startsWith("add_lesson_unit:")) {
            managementHandler.handleAddLessonUnitCallback(callback);
        } else if (data.startsWith("view_unit_lessons:")) {
            managementHandler.handleViewUnitLessonsCallback(callback);
        } else if (data.startsWith("view_unit_tests:")) {
            managementHandler.handleViewUnitTestsCallback(callback);
        } else if (data.startsWith("group_unit:")) {
            resultsHandler.showGroupSubgroupLessons(callback);
        } else if (data.startsWith("group_lesson:")) {
            resultsHandler.showGroupSubgroupTestResults(callback);
        } else if (data.startsWith("student_test_history:")) {
            resultsHandler.showStudentTestHistory(callback);
        } else if (data.startsWith("view_test_attempts:")) {
            resultsHandler.showTestAttempts(callback);
        } else if (data.startsWith("filter_name_page:")) {
            resultsHandler.showFilteredByNameResults(callback);
        } else if (data.startsWith("view_situational:")) {
            resultsHandler.viewSituationalAnswer(callback);
        } else if (data.startsWith("grade_situational:")) {
            resultsHandler.handleGradeSituationalCallback(callback);
        } else if (data.startsWith("confirm_grade:")) {
            resultsHandler.handleConfirmGradeCallback(callback);
        } else if ("cancel_grade".equals(data)) {
            resultsHandler.handleCancelGradeCallback(callback);
        } else if (data.startsWith("manual_grade:")) {
            resultsHandler.handleManualGradeCallback(callback);
        } else if (data.startsWith("view_unit_theory:")) {
            managementHandler.handleViewUnitTheoryCallback(callback);
        } else if (data.startsWith("view_unit_tasks:")) {
            managementHandler.handleViewUnitTasksCallback(callback);
        } else if (data.startsWith("edit_lesson:")) {
            managementHandler.handleEditLessonCallback(callback);
        } else if (data.startsWith("edit_lesson_title:")) {
            managementHandler.handleEditLessonTitleCallback(callback);
        } else if (data.startsWith("delete_lesson:")) {
            managementHandler.handleDeleteLessonCallback(callback);
        } else if (data.startsWith("confirm_delete_lesson:")) {
            managementHandler.handleConfirmDeleteLessonCallback(callback);
        } else if (data.startsWith("manage_test:")) {
            managementHandler.handleManageTestCallback(callback);
        } else if (data.startsWith("create_test:")) {
            managementHandler.handleCreateTestCallback(callback);
        } else if (data.startsWith("import_new_test:")) {
            managementHandler.handleImportNewTestCallback(callback);
        } else if (data.startsWith("import_questions:")) {
            managementHandler.handleImportQuestionsCallback(callback);
        } else if (data.startsWith("download_template:")) {
            managementHandler.handleDownloadTemplateCallback(callback);
        } else if (data.startsWith("edit_test_time:")) {
            managementHandler.handleEditTestTimeCallback(callback);
        } else if (data.startsWith("clear_questions:")) {
            managementHandler.handleClearQuestionsCallback(callback);
        } else if (data.startsWith("confirm_clear:")) {
            managementHandler.handleConfirmClearQuestions(callback);
        } else if (data.startsWith("manage_theory:")) {
            managementHandler.handleManageTheoryCallback(callback);
        } else if (data.startsWith("upload_theory:")) {
            managementHandler.handleUploadTheoryCallback(callback);
        } else if (data.startsWith("add_theory_text:")) {
            managementHandler.handleAddTheoryTextCallback(callback);
        } else if (data.startsWith("view_theory_material:")) {
            managementHandler.handleViewTheoryMaterialCallback(callback);
        } else if (data.startsWith("edit_theory_title:")) {
            managementHandler.handleEditTheoryTitleCallback(callback);
        } else if (data.startsWith("edit_theory_text:")) {
            managementHandler.handleEditTheoryTextCallback(callback);
        } else if (data.startsWith("delete_theory_material:")) {
            managementHandler.handleDeleteTheoryMaterialCallback(callback);
        } else if (data.startsWith("manage_task:")) {
            managementHandler.handleManageTaskCallback(callback);
        } else if (data.startsWith("create_task:")) {
            managementHandler.handleCreateTaskCallback(callback);
        } else if (data.startsWith("edit_task:")) {
            managementHandler.handleEditTaskCallback(callback);
        } else if (data.startsWith("edit_task_time:")) {
            managementHandler.handleEditTaskTimeCallback(callback);
        } else if (data.startsWith("edit_task_text:")) {
            managementHandler.handleEditTaskTextCallback(callback);
        } else if (data.startsWith("delete_task:")) {
            managementHandler.handleDeleteTaskCallback(callback);
        } else if (data.startsWith("delete_test:")) {
            managementHandler.handleDeleteTestCallback(callback);
        } else if (data.startsWith("confirm_delete_test:")) {
            managementHandler.handleConfirmDeleteTestCallback(callback);
        } else if (data.startsWith("list_questions:")) {
            managementHandler.handleListQuestionsCallback(callback, 1);
        } else if (data.startsWith("questions_page:")) {
            int page = parseIntSuffix(data, "questions_page:");
            managementHandler.handleListQuestionsCallback(callback, Math.max(1, page));
        } else if (data.startsWith("view_question:")) {
            managementHandler.handleViewQuestionCallback(callback);
        } else if (data.startsWith("edit_q_text:")) {
            managementHandler.handleEditQuestionTextCallback(callback);
        } else if (data.startsWith("edit_q_points:")) {
            managementHandler.handleEditQuestionPointsCallback(callback);
        } else if (data.startsWith("edit_answers:")) {
            managementHandler.handleEditAnswersCallback(callback);
        } else if (data.startsWith("edit_option:")) {
            managementHandler.handleEditOptionCallback(callback);
        } else if (data.startsWith("change_correct:")) {
            managementHandler.handleChangeCorrectCallback(callback);
        } else if (data.startsWith("set_correct:")) {
            managementHandler.handleSetCorrectCallback(callback);
        } else if (data.startsWith("move_q_up:")) {
            managementHandler.handleMoveQuestionUpCallback(callback);
        } else if (data.startsWith("move_q_down:")) {
            managementHandler.handleMoveQuestionDownCallback(callback);
        } else if (data.startsWith("delete_question:")) {
            managementHandler.handleDeleteQuestionCallback(callback);
        } else if (data.startsWith("confirm_delete_q:")) {
            managementHandler.handleConfirmDeleteQuestionCallback(callback);
        } else if (data.startsWith(AdminKeyboards.CB_PAGE)) {
            String pageData = data.substring(AdminKeyboards.CB_PAGE.length());
            String[] parts = pageData.split(":");
            if (parts.length == 2) {
                int page = parseIntStr(parts[1]);
                switch (parts[0]) {
                    case "test_results" -> resultsHandler.showTestResults(callback, page);
                    case "situational" -> resultsHandler.showSituationalAnswers(callback, page);
                }
            }
        } else {
            try { bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).build()); }
            catch (Exception ignored) {}
        }
    }

    private void handleCancel(CallbackQuery callback, long telegramId, long chatId, int messageId) {
        try { bot.execute(AnswerCallbackQuery.builder().callbackQueryId(callback.getId()).build()); }
        catch (Exception ignored) {}

        stateManager.clearState(telegramId);
        deleteMessage(chatId, messageId);
        sendMenu(chatId);
    }

    private void deleteMessage(long chatId, int messageId) {
        try { bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build()); }
        catch (Exception ignored) {}
    }

    private void sendMenu(long chatId) {
        try {
            bot.execute(SendMessage.builder().chatId(chatId).text(AdminMessages.MSG_ADMIN_MENU)
                    .replyMarkup(AdminKeyboards.adminMenu()).build());
        } catch (Exception e) { log.error("sendMenu: {}", e.getMessage()); }
    }

    private void sendText(long chatId, String text) {
        try { bot.execute(SendMessage.builder().chatId(chatId).text(text).build()); }
        catch (Exception e) { log.error("sendText: {}", e.getMessage()); }
    }

    private int parseIntSuffix(String data, String prefix) {
        try { return Integer.parseInt(data.substring(prefix.length())); }
        catch (Exception e) { return 1; }
    }

    private int parseIntStr(String s) {
        try { return Integer.parseInt(s); }
        catch (Exception e) { return 1; }
    }
}

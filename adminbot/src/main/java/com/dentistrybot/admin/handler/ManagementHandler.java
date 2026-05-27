package com.dentistrybot.admin.handler;

import com.dentistrybot.admin.keyboard.AdminKeyboards;
import com.dentistrybot.admin.localization.AdminMessages;
import com.dentistrybot.shared.model.*;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.TestRepository;
import com.dentistrybot.shared.service.FileService;
import com.dentistrybot.shared.service.ImportService;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.EditingStateData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ManagementHandler {

    private static final Logger log = LoggerFactory.getLogger(ManagementHandler.class);
    private static final int QUESTIONS_PER_PAGE = 10;

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final LessonRepository lessonRepository;
    private final TestRepository testRepository;
    private final FileService fileService;
    private final ImportService importService;
    private final String botToken;

    public ManagementHandler(TelegramClient bot, StateManager stateManager,
                              LessonRepository lessonRepository, TestRepository testRepository,
                              FileService fileService, ImportService importService, String botToken) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.lessonRepository = lessonRepository;
        this.testRepository = testRepository;
        this.fileService = fileService;
        this.importService = importService;
        this.botToken = botToken;
    }

    public void showContentManagement(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(AdminMessages.MSG_CONTENT_MANAGEMENT)
                .replyMarkup(AdminKeyboards.contentManagement()).build());
        } catch (Exception e) { log.error("showContentManagement: {}", e.getMessage()); }
    }

    public void handleBackToAdminMenu(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(AdminMessages.MSG_ADMIN_MENU)
                .replyMarkup(AdminKeyboards.adminMenu()).build());
        } catch (Exception e) { log.error("handleBackToAdminMenu: {}", e.getMessage()); }
    }

    public void handleManageCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String action = callback.getData().substring(AdminKeyboards.CB_MANAGE.length());
        try {
            answerCallback(callback.getId());
            switch (action) {
                case "units" -> {
                    List<Unit> units = lessonRepository.getAllUnits();
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId).text("Fanlar:")
                        .replyMarkup(AdminKeyboards.unitList(units)).build());
                }
                case "lessons" -> {
                    List<Unit> units = lessonRepository.getAllUnits();
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId).text("Fanni tanlang (darslarini ko'rish uchun):")
                        .replyMarkup(buildUnitSelectionKeyboard(units, "view_unit_lessons:")).build());
                }
                case "tests" -> {
                    List<Unit> units = lessonRepository.getAllUnits();
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId).text("Fanni tanlang (testlarni ko'rish uchun):")
                        .replyMarkup(buildUnitSelectionKeyboard(units, "view_unit_tests:")).build());
                }
                case "theory" -> {
                    List<Unit> units = lessonRepository.getAllUnits();
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId).text("Fanni tanlang:")
                        .replyMarkup(buildUnitSelectionKeyboard(units, "view_unit_theory:")).build());
                }
                case "tasks" -> {
                    List<Unit> units = lessonRepository.getAllUnits();
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId).text("Fanni tanlang:")
                        .replyMarkup(buildUnitSelectionKeyboard(units, "view_unit_tasks:")).build());
                }
            }
        } catch (Exception e) { log.error("handleManageCallback: {}", e.getMessage()); }
    }

    // в”Ђв”Ђв”Ђ Units в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public void handleEditUnitCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("edit_unit:".length()));
            Unit unit = lessonRepository.getUnitById(unitId);
            if (unit == null) return;
            answerCallback(callback.getId());
            String text = "Fan: " + unit.getName() + " вЂ” " + unit.getTitleUz();
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(AdminKeyboards.unitEdit(unit)).build());
        } catch (Exception e) { log.error("handleEditUnitCallback: {}", e.getMessage()); }
    }

    public void handleAddUnitCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            answerCallback(callback.getId());
            stateManager.setState(telegramId, StateConstants.MANAGE_UNITS + "_add_name");
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Fan nomini kiriting (masalan: F1):").replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleAddUnitCallback: {}", e.getMessage()); }
    }

    public void handleAddUnitNameInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            String name = message.getText().trim();
            EditingStateData ed = new EditingStateData();
            ed.setExtraData(name);
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_UNITS + "_add_title", ed);
            sendText(chatId, "Sarlavhani kiriting (o'zbekcha):");
        } catch (Exception e) { log.error("handleAddUnitNameInput: {}", e.getMessage()); }
    }

    public void handleAddUnitTitleInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            String name = ed != null ? ed.getExtraData() : "";
            Unit unit = new Unit();
            unit.setName(name);
            unit.setTitleUz(message.getText().trim());
            lessonRepository.createUnit(unit);
            stateManager.clearState(telegramId);
            sendText(chatId, "вњ… Fan qo'shildi: " + name);
        } catch (Exception e) { log.error("handleAddUnitTitleInput: {}", e.getMessage()); }
    }

    public void handleEditUnitNameCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("edit_unit_name:".length()));
            EditingStateData ed = new EditingStateData();
            ed.setTargetId(unitId);
            ed.setTargetType("unit");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_UNITS + "_edit_name", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Yangi nomni kiriting:").replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditUnitNameCallback: {}", e.getMessage()); }
    }

    public void handleEditUnitNameInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            Unit unit = lessonRepository.getUnitById(ed.getTargetId());
            if (unit == null) return;
            unit.setName(message.getText().trim());
            lessonRepository.updateUnit(unit);
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleEditUnitNameInput: {}", e.getMessage()); }
    }

    public void handleEditUnitTitleCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("edit_unit_title:".length()));
            EditingStateData ed = new EditingStateData();
            ed.setTargetId(unitId);
            ed.setTargetType("unit");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_UNITS + "_edit_title", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Yangi sarlavhani kiriting:").replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditUnitTitleCallback: {}", e.getMessage()); }
    }

    public void handleEditUnitTitleInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            Unit unit = lessonRepository.getUnitById(ed.getTargetId());
            if (unit == null) return;
            unit.setTitleUz(message.getText().trim());
            lessonRepository.updateUnit(unit);
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleEditUnitTitleInput: {}", e.getMessage()); }
    }

    public void handleDeleteUnitConfirmCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("delete_unit_confirm:".length()));
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_DELETE_CONFIRM)
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                        btn("Ha", "delete_unit_yes:" + unitId),
                        btn("Yo'q", AdminKeyboards.CB_CANCEL))).build()).build());
        } catch (Exception e) { log.error("handleDeleteUnitConfirmCallback: {}", e.getMessage()); }
    }

    public void handleDeleteUnitYesCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("delete_unit_yes:".length()));
            lessonRepository.deleteUnit(unitId);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_DELETED).build());
        } catch (Exception e) { log.error("handleDeleteUnitYesCallback: {}", e.getMessage()); }
    }

    // в”Ђв”Ђв”Ђ Lessons в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public void handleViewUnitLessonsCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("view_unit_lessons:".length()));
            List<Lesson> lessons = lessonRepository.getLessonsByUnitId(unitId);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text("Darslar:")
                .replyMarkup(AdminKeyboards.lessonList(lessons, unitId)).build());
        } catch (Exception e) { log.error("handleViewUnitLessonsCallback: {}", e.getMessage()); }
    }

    public void handleAddLessonCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            answerCallback(callback.getId());
            List<Unit> units = lessonRepository.getAllUnits();
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text("Fanni tanlang:")
                .replyMarkup(buildUnitSelectionKeyboard(units, "add_lesson_unit:")).build());
        } catch (Exception e) { log.error("handleAddLessonCallback: {}", e.getMessage()); }
    }

    public void handleAddLessonUnitCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("add_lesson_unit:".length()));
            EditingStateData ed = new EditingStateData();
            ed.setTargetId(unitId);
            ed.setTargetType("lesson");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_LESSONS + "_title", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_TITLE).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleAddLessonUnitCallback: {}", e.getMessage()); }
    }

    public void handleLessonTitleInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            List<Lesson> existing = lessonRepository.getLessonsByUnitId(ed.getTargetId());
            Lesson lesson = new Lesson();
            lesson.setUnitId(ed.getTargetId());
            lesson.setLessonNumber(existing.size() + 1);
            lesson.setTitleUz(message.getText().trim());
            lessonRepository.createLesson(lesson);
            stateManager.clearState(telegramId);
            sendText(chatId, "вњ… Dars qo'shildi: " + lesson.getLessonNumber() + "-dars");
        } catch (Exception e) { log.error("handleLessonTitleInput: {}", e.getMessage()); }
    }

    public void handleEditLessonCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("edit_lesson:".length()));
            Lesson lesson = lessonRepository.getLessonById(lessonId);
            if (lesson == null) return;
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(lesson.getLessonNumber() + "-dars: " + lesson.getTitleUz())
                .replyMarkup(AdminKeyboards.lessonEdit(lesson)).build());
        } catch (Exception e) { log.error("handleEditLessonCallback: {}", e.getMessage()); }
    }

    public void handleEditLessonTitleCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("edit_lesson_title:".length()));
            EditingStateData ed = new EditingStateData();
            ed.setTargetId(lessonId);
            ed.setTargetType("lesson");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_LESSONS + "_edit_title", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_TITLE).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditLessonTitleCallback: {}", e.getMessage()); }
    }

    public void handleEditLessonTitleInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            Lesson lesson = lessonRepository.getLessonById(ed.getTargetId());
            if (lesson == null) return;
            lesson.setTitleUz(message.getText().trim());
            lessonRepository.updateLesson(lesson);
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleEditLessonTitleInput: {}", e.getMessage()); }
    }

    public void handleDeleteLessonCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("delete_lesson:".length()));
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_DELETE_CONFIRM)
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                        btn("Ha", "confirm_delete_lesson:" + lessonId),
                        btn("Yo'q", AdminKeyboards.CB_CANCEL))).build()).build());
        } catch (Exception e) { log.error("handleDeleteLessonCallback: {}", e.getMessage()); }
    }

    public void handleConfirmDeleteLessonCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("confirm_delete_lesson:".length()));
            Lesson lesson = lessonRepository.getLessonById(lessonId);
            if (lesson != null) {
                lessonRepository.deleteLesson(lessonId);
                lessonRepository.renumberLessons(lesson.getUnitId());
            }
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_DELETED).build());
        } catch (Exception e) { log.error("handleConfirmDeleteLessonCallback: {}", e.getMessage()); }
    }

    // в”Ђв”Ђв”Ђ Tests в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public void handleViewUnitTestsCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("view_unit_tests:".length()));
            List<Lesson> lessons = lessonRepository.getLessonsByUnitId(unitId);
            answerCallback(callback.getId());
            List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
            for (Lesson l : lessons)
                rows.add(new InlineKeyboardRow(btn(l.getLessonNumber() + "-dars: " + l.getTitleUz(), "manage_test:" + l.getId())));
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL)));
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text("Darsni tanlang:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("handleViewUnitTestsCallback: {}", e.getMessage()); }
    }

    public void handleManageTestCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("manage_test:".length()));
            Test test = testRepository.getTestByLessonId(lessonId);
            answerCallback(callback.getId());
            String text = test != null
                ? "Test: " + test.getTitleUz() + "\nVaqt: " + test.getTimeLimitMinutes() + " daqiqa\nBall: " + test.getTotalPoints()
                : "Bu dars uchun test mavjud emas.";
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(AdminKeyboards.manageTest(lessonId, test != null)).build());
        } catch (Exception e) { log.error("handleManageTestCallback: {}", e.getMessage()); }
    }

    public void handleCreateTestCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("create_test:".length()));
            EditingStateData ed = new EditingStateData();
            ed.setTargetId(lessonId);
            ed.setTargetType("test");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TESTS + "_time", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_TIME_LIMIT).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleCreateTestCallback: {}", e.getMessage()); }
    }

    public void handleTestTimeInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            int minutes = Integer.parseInt(message.getText().trim());
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            String state = us.getState();
            if (state.endsWith("_edit_time")) {
                // Edit existing test time
                Test test = testRepository.getTestById(ed.getTargetId());
                if (test != null) {
                    test.setTimeLimitMinutes(minutes);
                    testRepository.updateTest(test);
                }
            } else {
                // Create new test
                Test test = new Test();
                test.setLessonId(ed.getTargetId());
                test.setTitleUz("Test " + ed.getTargetId());
                test.setTimeLimitMinutes(minutes);
                test.setTotalPoints(0);
                testRepository.createTest(test);
            }
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (NumberFormatException ex) {
            sendText(chatId, "Son kiriting.");
        } catch (Exception e) { log.error("handleTestTimeInput: {}", e.getMessage()); }
    }

    public void handleEditTestTimeCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("edit_test_time:".length()));
            Test test = testRepository.getTestByLessonId(lessonId);
            if (test == null) return;
            EditingStateData ed = new EditingStateData();
            ed.setTargetId(test.getId());
            ed.setTargetType("test");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TESTS + "_edit_time", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_TIME_LIMIT).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditTestTimeCallback: {}", e.getMessage()); }
    }

    public void handleImportQuestionsCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("import_questions:".length()));
            Test test = testRepository.getTestByLessonId(lessonId);
            if (test == null) return;
            EditingStateData ed = new EditingStateData();
            ed.setTargetId(test.getId());
            ed.setTargetType("test");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TESTS + "_import", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Excel (.xlsx) yoki CSV fayl yuboring:").replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleImportQuestionsCallback: {}", e.getMessage()); }
    }

    public void handleImportNewTestCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("import_new_test:".length()));
            // Create test with default time, then import
            Test test = new Test();
            test.setLessonId(lessonId);
            test.setTitleUz("Test " + lessonId);
            test.setTimeLimitMinutes(60);
            test.setTotalPoints(0);
            testRepository.createTest(test);
            EditingStateData ed = new EditingStateData();
            ed.setTargetId(test.getId());
            ed.setTargetType("test");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TESTS + "_import", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Test yaratildi! Savollar faylini yuboring:").replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleImportNewTestCallback: {}", e.getMessage()); }
    }

    public void handleQuestionFileUpload(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        Document doc = message.getDocument();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;

            org.telegram.telegrambots.meta.api.objects.File tgFile = bot.execute(GetFile.builder().fileId(doc.getFileId()).build());
            String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
            String ext = doc.getFileName() != null && doc.getFileName().endsWith(".csv") ? ".csv" : ".xlsx";
            Path tmpPath = Files.createTempFile("import_", ext);
            ImportService.downloadTelegramFile(fileUrl, tmpPath.toString());

            int imported;
            if (ext.equals(".csv")) imported = importService.importQuestionsFromCsv(tmpPath.toString(), ed.getTargetId());
            else imported = importService.importQuestionsFromExcel(tmpPath.toString(), ed.getTargetId());
            Files.deleteIfExists(tmpPath);

            stateManager.clearState(telegramId);
            sendText(chatId, "вњ… " + imported + " ta savol import qilindi!");
        } catch (Exception e) {
            log.error("handleQuestionFileUpload: {}", e.getMessage());
            sendText(chatId, "Xatolik: " + e.getMessage());
        }
    }

    public void handleDownloadTemplateCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        try {
            answerCallback(callback.getId());
            Path tmp = Files.createTempFile("template_", ".xlsx");
            importService.generateQuestionTemplate(tmp.toString());
            fileService.sendDocument(bot, chatId, tmp.toString(), "Savollar shabloni");
            Files.deleteIfExists(tmp);
        } catch (Exception e) { log.error("handleDownloadTemplateCallback: {}", e.getMessage()); }
    }

    public void handleClearQuestionsCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("clear_questions:".length()));
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_DELETE_CONFIRM)
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(btn("Ha", "confirm_clear:" + lessonId), btn("Yo'q", AdminKeyboards.CB_CANCEL))).build()).build());
        } catch (Exception e) { log.error("handleClearQuestionsCallback: {}", e.getMessage()); }
    }

    public void handleConfirmClearQuestions(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("confirm_clear:".length()));
            Test test = testRepository.getTestByLessonId(lessonId);
            if (test != null) testRepository.deleteAllQuestions(test.getId());
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_DELETED).build());
        } catch (Exception e) { log.error("handleConfirmClearQuestions: {}", e.getMessage()); }
    }

    public void handleDeleteTestCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("delete_test:".length()));
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(AdminMessages.MSG_DELETE_CONFIRM)
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(btn("Ha", "confirm_delete_test:" + lessonId), btn("Yo'q", AdminKeyboards.CB_CANCEL))).build()).build());
        } catch (Exception e) { log.error("handleDeleteTestCallback: {}", e.getMessage()); }
    }

    public void handleConfirmDeleteTestCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("confirm_delete_test:".length()));
            Test test = testRepository.getTestByLessonId(lessonId);
            if (test != null) testRepository.deleteTest(test.getId());
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(AdminMessages.MSG_DELETED).build());
        } catch (Exception e) { log.error("handleConfirmDeleteTestCallback: {}", e.getMessage()); }
    }

    // в”Ђв”Ђв”Ђ Questions в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public void handleListQuestionsCallback(CallbackQuery callback, int page) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().contains(":")
                ? callback.getData().split(":")[1] : callback.getData().substring("list_questions:".length()));
            Test test = testRepository.getTestByLessonId(lessonId);
            if (test == null) return;
            int total = testRepository.countQuestions(test.getId());
            List<Question> questions = testRepository.getQuestionsByTestIdPaginated(test.getId(), QUESTIONS_PER_PAGE, (page - 1) * QUESTIONS_PER_PAGE);
            int totalPages = (total + QUESTIONS_PER_PAGE - 1) / QUESTIONS_PER_PAGE;
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Savollar (" + total + " ta, sahifa " + page + "/" + totalPages + "):")
                .replyMarkup(AdminKeyboards.questionList(questions, test.getId(), page, totalPages)).build());
        } catch (Exception e) { log.error("handleListQuestionsCallback: {}", e.getMessage()); }
    }

    public void handleViewQuestionCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int questionId = Integer.parseInt(callback.getData().substring("view_question:".length()));
            Question q = testRepository.getQuestionById(questionId);
            if (q == null) return;
            answerCallback(callback.getId());
            StringBuilder sb = new StringBuilder("Savol ").append(q.getOrderNum()).append(":\n\n").append(q.getQuestionText()).append("\n\n");
            var opts = testRepository.getAnswerOptionsByQuestionId(questionId);
            String[] letters = {"A", "B", "C", "D", "E"};
            for (int i = 0; i < opts.size(); i++) {
                var o = opts.get(i);
                sb.append(letters[i % letters.length]).append(") ").append(o.getOptionText());
                if (o.isCorrect()) sb.append(" вњ…");
                sb.append("\n");
            }
            sb.append("\nBall: ").append(q.getPoints());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(sb.toString())
                .replyMarkup(AdminKeyboards.questionView(q)).build());
        } catch (Exception e) { log.error("handleViewQuestionCallback: {}", e.getMessage()); }
    }

    public void handleEditQuestionTextCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int questionId = Integer.parseInt(callback.getData().substring("edit_q_text:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(questionId); ed.setTargetType("question");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TESTS + "_edit_q_text", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Yangi savol matnini kiriting:").replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditQuestionTextCallback: {}", e.getMessage()); }
    }

    public void handleQuestionTextInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            Question q = testRepository.getQuestionById(ed.getTargetId());
            if (q == null) return;
            q.setQuestionText(message.getText().trim());
            testRepository.updateQuestion(q);
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleQuestionTextInput: {}", e.getMessage()); }
    }

    public void handleEditQuestionPointsCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int questionId = Integer.parseInt(callback.getData().substring("edit_q_points:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(questionId); ed.setTargetType("question");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TESTS + "_edit_q_points", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_POINTS).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditQuestionPointsCallback: {}", e.getMessage()); }
    }

    public void handleQuestionPointsInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            int points = Integer.parseInt(message.getText().trim());
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            Question q = testRepository.getQuestionById(ed.getTargetId());
            if (q == null) return;
            q.setPoints(points);
            testRepository.updateQuestion(q);
            testRepository.updateTestTotalPoints(q.getTestId());
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleQuestionPointsInput: {}", e.getMessage()); }
    }

    public void handleEditAnswersCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int questionId = Integer.parseInt(callback.getData().substring("edit_answers:".length()));
            var opts = testRepository.getAnswerOptionsByQuestionId(questionId);
            answerCallback(callback.getId());
            List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
            String[] letters = {"A", "B", "C", "D", "E"};
            for (int i = 0; i < opts.size(); i++) {
                var o = opts.get(i);
                rows.add(new InlineKeyboardRow(btn(letters[i % letters.length] + ") " + o.getOptionText() + (o.isCorrect() ? " вњ…" : ""), "edit_option:" + o.getId())));
            }
            rows.add(new InlineKeyboardRow(btn("вњ… To'g'ri javobni o'zgartirish", "change_correct:" + questionId)));
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, "view_question:" + questionId)));
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text("Javob variantlari:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("handleEditAnswersCallback: {}", e.getMessage()); }
    }

    public void handleEditOptionCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int optionId = Integer.parseInt(callback.getData().substring("edit_option:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(optionId); ed.setTargetType("option");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TESTS + "_edit_option", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Yangi variant matnini kiriting:").replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditOptionCallback: {}", e.getMessage()); }
    }

    public void handleOptionTextInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            AnswerOption opt = testRepository.getAnswerOptionById(ed.getTargetId());
            if (opt == null) return;
            opt.setOptionText(message.getText().trim());
            testRepository.updateAnswerOption(opt);
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleOptionTextInput: {}", e.getMessage()); }
    }

    public void handleChangeCorrectCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int questionId = Integer.parseInt(callback.getData().substring("change_correct:".length()));
            var opts = testRepository.getAnswerOptionsByQuestionId(questionId);
            answerCallback(callback.getId());
            List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
            String[] letters = {"A", "B", "C", "D", "E"};
            for (int i = 0; i < opts.size(); i++) {
                var o = opts.get(i);
                rows.add(new InlineKeyboardRow(btn(letters[i % letters.length] + ") " + o.getOptionText(), "set_correct:" + o.getId() + ":" + questionId)));
            }
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, "edit_answers:" + questionId)));
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text("To'g'ri javobni tanlang:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("handleChangeCorrectCallback: {}", e.getMessage()); }
    }

    public void handleSetCorrectCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            String[] parts = callback.getData().substring("set_correct:".length()).split(":");
            int optionId = Integer.parseInt(parts[0]);
            int questionId = Integer.parseInt(parts[1]);
            testRepository.setCorrectAnswer(questionId, optionId);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(AdminMessages.MSG_SUCCESS)
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, "view_question:" + questionId))).build()).build());
        } catch (Exception e) { log.error("handleSetCorrectCallback: {}", e.getMessage()); }
    }

    public void handleMoveQuestionUpCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int qId = Integer.parseInt(callback.getData().substring("move_q_up:".length()));
            Question q = testRepository.getQuestionById(qId);
            if (q != null && q.getOrderNum() > 1) {
                List<Question> all = testRepository.getQuestionsByTestId(q.getTestId());
                all.stream().filter(x -> x.getOrderNum() == q.getOrderNum() - 1).findFirst()
                    .ifPresent(prev -> testRepository.swapQuestionOrders(qId, prev.getId()));
            }
            answerCallback(callback.getId());
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleMoveQuestionUpCallback: {}", e.getMessage()); }
    }

    public void handleMoveQuestionDownCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int qId = Integer.parseInt(callback.getData().substring("move_q_down:".length()));
            Question q = testRepository.getQuestionById(qId);
            if (q != null) {
                List<Question> all = testRepository.getQuestionsByTestId(q.getTestId());
                all.stream().filter(x -> x.getOrderNum() == q.getOrderNum() + 1).findFirst()
                    .ifPresent(next -> testRepository.swapQuestionOrders(qId, next.getId()));
            }
            answerCallback(callback.getId());
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleMoveQuestionDownCallback: {}", e.getMessage()); }
    }

    public void handleDeleteQuestionCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int qId = Integer.parseInt(callback.getData().substring("delete_question:".length()));
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(AdminMessages.MSG_DELETE_CONFIRM)
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(btn("Ha", "confirm_delete_q:" + qId), btn("Yo'q", "view_question:" + qId))).build()).build());
        } catch (Exception e) { log.error("handleDeleteQuestionCallback: {}", e.getMessage()); }
    }

    public void handleConfirmDeleteQuestionCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int qId = Integer.parseInt(callback.getData().substring("confirm_delete_q:".length()));
            var q = testRepository.getQuestionById(qId);
            testRepository.deleteQuestion(qId, q != null ? q.getTestId() : 0);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(AdminMessages.MSG_DELETED).build());
        } catch (Exception e) { log.error("handleConfirmDeleteQuestionCallback: {}", e.getMessage()); }
    }

    // в”Ђв”Ђв”Ђ Theory в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public void handleViewUnitTheoryCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("view_unit_theory:".length()));
            List<Lesson> lessons = lessonRepository.getLessonsByUnitId(unitId);
            answerCallback(callback.getId());
            List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
            for (Lesson l : lessons)
                rows.add(new InlineKeyboardRow(btn(l.getLessonNumber() + "-dars: " + l.getTitleUz(), "manage_theory:" + l.getId())));
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL)));
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text("Darsni tanlang:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("handleViewUnitTheoryCallback: {}", e.getMessage()); }
    }

    public void handleManageTheoryCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("manage_theory:".length()));
            List<TheoryMaterial> materials = lessonRepository.getTheoryMaterialsByLessonId(lessonId);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text("Nazariya materiallar:")
                .replyMarkup(AdminKeyboards.theoryList(materials, lessonId)).build());
        } catch (Exception e) { log.error("handleManageTheoryCallback: {}", e.getMessage()); }
    }

    public void handleUploadTheoryCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("upload_theory:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(lessonId); ed.setTargetType("material");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_THEORY + "_title", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_TITLE).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleUploadTheoryCallback: {}", e.getMessage()); }
    }

    public void handleTheoryTitleInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            ed.setExtraData(message.getText().trim());
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_THEORY + "_upload", ed);
            sendText(chatId, AdminMessages.MSG_UPLOAD_FILE);
        } catch (Exception e) { log.error("handleTheoryTitleInput: {}", e.getMessage()); }
    }

    public void handleTheoryFileUpload(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        Document doc = message.getDocument();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;

            Lesson lesson = lessonRepository.getLessonById(ed.getTargetId());
            if (lesson == null) return;

            org.telegram.telegrambots.meta.api.objects.File tgFile = bot.execute(GetFile.builder().fileId(doc.getFileId()).build());
            String fileUrl = "https://api.telegram.org/file/bot" + botToken + "/" + tgFile.getFilePath();
            Path tmp = Files.createTempFile("theory_", "_" + doc.getFileName());
            ImportService.downloadTelegramFile(fileUrl, tmp.toString());

            Unit unit = lessonRepository.getUnitById(lesson.getUnitId());
            String unitName = unit != null ? unit.getName() : "unit" + lesson.getUnitId();
            String relativePath = fileService.saveFile(unitName, lesson.getLessonNumber(), doc.getFileName(), Files.readAllBytes(tmp));
            Files.deleteIfExists(tmp);

            TheoryMaterial material = new TheoryMaterial();
            material.setLessonId(ed.getTargetId());
            material.setTitleUz(ed.getExtraData() != null ? ed.getExtraData() : doc.getFileName());
            material.setMaterialType("material");
            material.setFilePath(relativePath);
            lessonRepository.createTheoryMaterial(material);

            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_FILE_UPLOADED);
        } catch (Exception e) {
            log.error("handleTheoryFileUpload: {}", e.getMessage());
            sendText(chatId, "Xatolik: " + e.getMessage());
        }
    }

    public void handleAddTheoryTextCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("add_theory_text:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(lessonId); ed.setTargetType("material");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_THEORY + "_title", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_TITLE).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleAddTheoryTextCallback: {}", e.getMessage()); }
    }

    public void handleViewTheoryMaterialCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int materialId = Integer.parseInt(callback.getData().substring("view_theory_material:".length()));
            TheoryMaterial m = lessonRepository.getTheoryMaterialById(materialId);
            if (m == null) return;
            answerCallback(callback.getId());
            String text = "рџ“„ " + m.getTitleUz() + "\nTuri: " + m.getMaterialType()
                + (m.getDescription() != null ? "\n\n" + m.getDescription() : "");
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(AdminKeyboards.theoryMaterialView(m)).build());
        } catch (Exception e) { log.error("handleViewTheoryMaterialCallback: {}", e.getMessage()); }
    }

    public void handleEditTheoryTitleCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int materialId = Integer.parseInt(callback.getData().substring("edit_theory_title:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(materialId); ed.setTargetType("material");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_THEORY + "_edit_title", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_TITLE).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditTheoryTitleCallback: {}", e.getMessage()); }
    }

    public void handleEditTheoryTextCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int materialId = Integer.parseInt(callback.getData().substring("edit_theory_text:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(materialId); ed.setTargetType("material");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_THEORY + "_edit_text", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_DESCRIPTION).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditTheoryTextCallback: {}", e.getMessage()); }
    }

    public void handleTheoryEditInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            if (us == null) return;
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            if (us.getState().endsWith("_edit_title")) {
                lessonRepository.updateTheoryMaterialTitle(ed.getTargetId(), message.getText().trim());
            } else {
                lessonRepository.updateTheoryMaterialDescription(ed.getTargetId(), message.getText().trim());
            }
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleTheoryEditInput: {}", e.getMessage()); }
    }

    public void handleDeleteTheoryMaterialCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int materialId = Integer.parseInt(callback.getData().substring("delete_theory_material:".length()));
            TheoryMaterial m = lessonRepository.getTheoryMaterialById(materialId);
            if (m != null) {
                if (m.getFilePath() != null && !m.getFilePath().isEmpty())
                    try { fileService.deleteFile(m.getFilePath()); } catch (Exception ignored) {}
                lessonRepository.deleteTheoryMaterial(materialId);
            }
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(AdminMessages.MSG_DELETED).build());
        } catch (Exception e) { log.error("handleDeleteTheoryMaterialCallback: {}", e.getMessage()); }
    }

    // в”Ђв”Ђв”Ђ Situational Tasks в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    public void handleViewUnitTasksCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int unitId = Integer.parseInt(callback.getData().substring("view_unit_tasks:".length()));
            List<Lesson> lessons = lessonRepository.getLessonsByUnitId(unitId);
            answerCallback(callback.getId());
            List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
            for (Lesson l : lessons)
                rows.add(new InlineKeyboardRow(btn(l.getLessonNumber() + "-dars", "manage_task:" + l.getId())));
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL)));
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text("Darsni tanlang:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("handleViewUnitTasksCallback: {}", e.getMessage()); }
    }

    public void handleManageTaskCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("manage_task:".length()));
            List<SituationalTask> tasks = lessonRepository.getSituationalTasksByLessonId(lessonId);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Vaziyatli masalalar (" + tasks.size() + " ta):")
                .replyMarkup(AdminKeyboards.taskList(tasks, lessonId)).build());
        } catch (Exception e) { log.error("handleManageTaskCallback: {}", e.getMessage()); }
    }

    public void handleCreateTaskCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring("create_task:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(lessonId); ed.setTargetType("task");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TASKS + "_time", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_TIME_LIMIT).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleCreateTaskCallback: {}", e.getMessage()); }
    }

    public void handleTaskTimeInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            int minutes = Integer.parseInt(message.getText().trim());
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            ed.setExtraData(String.valueOf(minutes));
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TASKS + "_text", ed);
            sendText(chatId, "Vaziyatli masala matnini kiriting:");
        } catch (NumberFormatException ex) {
            sendText(chatId, "Son kiriting.");
        } catch (Exception e) { log.error("handleTaskTimeInput: {}", e.getMessage()); }
    }

    public void handleTaskTextInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            int minutes = ed.getExtraData() != null ? Integer.parseInt(ed.getExtraData()) : 30;
            List<SituationalTask> existing = lessonRepository.getSituationalTasksByLessonId(ed.getTargetId());
            SituationalTask task = new SituationalTask();
            task.setLessonId(ed.getTargetId());
            task.setOrderNum(existing.size() + 1);
            task.setTaskText(message.getText().trim());
            task.setTimeLimitMinutes(minutes);
            lessonRepository.createSituationalTask(task);
            stateManager.clearState(telegramId);
            sendText(chatId, "вњ… Vaziyatli masala qo'shildi!");
        } catch (Exception e) { log.error("handleTaskTextInput: {}", e.getMessage()); }
    }

    public void handleEditTaskCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int taskId = Integer.parseInt(callback.getData().substring("edit_task:".length()));
            SituationalTask task = lessonRepository.getSituationalTaskById(taskId);
            if (task == null) return;
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(task.getOrderNum() + "-masala:\n\n" + task.getTaskText() + "\n\nVaqt: " + task.getTimeLimitMinutes() + " daqiqa")
                .replyMarkup(AdminKeyboards.taskEdit(task)).build());
        } catch (Exception e) { log.error("handleEditTaskCallback: {}", e.getMessage()); }
    }

    public void handleEditTaskTimeCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int taskId = Integer.parseInt(callback.getData().substring("edit_task_time:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(taskId); ed.setTargetType("task");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TASKS + "_edit_time", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_TIME_LIMIT).replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditTaskTimeCallback: {}", e.getMessage()); }
    }

    public void handleEditTaskTimeInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            int minutes = Integer.parseInt(message.getText().trim());
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            lessonRepository.updateSituationalTaskTime(ed.getTargetId(), minutes);
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleEditTaskTimeInput: {}", e.getMessage()); }
    }

    public void handleEditTaskTextCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int taskId = Integer.parseInt(callback.getData().substring("edit_task_text:".length()));
            EditingStateData ed = new EditingStateData(); ed.setTargetId(taskId); ed.setTargetType("task");
            stateManager.setStateWithData(telegramId, StateConstants.MANAGE_TASKS + "_edit_text", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("Yangi matnni kiriting:").replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditTaskTextCallback: {}", e.getMessage()); }
    }

    public void handleEditTaskTextInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            lessonRepository.updateSituationalTaskText(ed.getTargetId(), message.getText().trim());
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_SUCCESS);
        } catch (Exception e) { log.error("handleEditTaskTextInput: {}", e.getMessage()); }
    }

    public void handleDeleteTaskCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int taskId = Integer.parseInt(callback.getData().substring("delete_task:".length()));
            SituationalTask task = lessonRepository.getSituationalTaskById(taskId);
            if (task == null) return;
            lessonRepository.deleteSituationalTask(taskId);
            lessonRepository.renumberSituationalTasks(task.getLessonId());
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(AdminMessages.MSG_DELETED).build());
        } catch (Exception e) { log.error("handleDeleteTaskCallback: {}", e.getMessage()); }
    }

    public boolean isManaging(long telegramId) {
        String state = stateManager.getState(telegramId);
        return state != null && (state.startsWith(StateConstants.MANAGE_UNITS)
            || state.startsWith(StateConstants.MANAGE_LESSONS)
            || state.startsWith(StateConstants.MANAGE_TESTS)
            || state.startsWith(StateConstants.MANAGE_THEORY)
            || state.startsWith(StateConstants.MANAGE_TASKS)
            || "edit_student_name".equals(state));
    }

    // в”Ђв”Ђв”Ђ Helpers в”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђв”Ђ

    private InlineKeyboardMarkup buildUnitSelectionKeyboard(List<Unit> units, String prefix) {
        List<InlineKeyboardRow> rows = new java.util.ArrayList<>();
        for (Unit u : units)
            rows.add(new InlineKeyboardRow(btn(u.getName() + " - " + u.getTitleUz(), prefix + u.getId())));
        rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL)));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
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


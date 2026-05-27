package com.dentistrybot.admin.handler;

import com.dentistrybot.admin.keyboard.AdminKeyboards;
import com.dentistrybot.admin.localization.AdminMessages;
import com.dentistrybot.shared.model.*;
import com.dentistrybot.shared.repository.AdminRepository;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.GradingService;
import com.dentistrybot.shared.service.NotificationService;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.AdminGradingData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

public class ResultsHandler {

    private static final Logger log = LoggerFactory.getLogger(ResultsHandler.class);
    private static final int PAGE_SIZE = 10;

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final LessonRepository lessonRepository;
    private final AdminRepository adminRepository;
    private final NotificationService notificationService;
    private final GradingService gradingService;

    public ResultsHandler(TelegramClient bot, StateManager stateManager,
                          ResultRepository resultRepository, StudentRepository studentRepository,
                          LessonRepository lessonRepository, AdminRepository adminRepository,
                          NotificationService notificationService, GradingService gradingService) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.lessonRepository = lessonRepository;
        this.adminRepository = adminRepository;
        this.notificationService = notificationService;
        this.gradingService = gradingService;
    }

    public void showTestResults(CallbackQuery callback, int page) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId)
                .text("Test natijalarini ko'rish uchun filtrdan foydalaning:")
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(
                        btn("🔍 Guruh+Kichik guruh", "filter_results:combined"),
                        btn("🔍 Ism", "filter_results:name")))
                    .keyboardRow(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_BACK + "admin_menu")))
                    .build()).build());
        } catch (Exception e) { log.error("showTestResults: {}", e.getMessage()); }
    }

    public void showSituationalAnswers(CallbackQuery callback, int page) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            answerCallback(callback.getId());
            int total = resultRepository.countSituationalAnswersWithStudents();
            List<SituationalAnswerWithStudent> answers = resultRepository.getSituationalAnswersWithStudents(PAGE_SIZE, (page - 1) * PAGE_SIZE);
            StringBuilder sb = new StringBuilder("📋 Vaziyatli masala javoblari (" + total + " ta)\n\n");
            for (SituationalAnswerWithStudent a : answers) {
                String status = a.isGraded() ? "✅" : "⏳";
                sb.append(status).append(" ").append(a.getStudentName())
                  .append(" — ").append(a.getTaskText() != null ? truncate(a.getTaskText(), 30) : "").append("\n");
            }
            List<InlineKeyboardRow> rows = new ArrayList<>();
            for (SituationalAnswerWithStudent a : answers) {
                rows.add(new InlineKeyboardRow(btn(
                    (a.isGraded() ? "✅ " : "⏳ ") + a.getStudentName(),
                    "view_situational:" + a.getId())));
            }
            int totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
            if (totalPages > 1) {
                InlineKeyboardRow pag = new InlineKeyboardRow();
                if (page > 1) pag.add(btn(AdminMessages.BTN_PREVIOUS, "page:situational:" + (page - 1)));
                pag.add(btn(String.format(AdminMessages.MSG_PAGE, page, totalPages), "noop"));
                if (page < totalPages) pag.add(btn(AdminMessages.BTN_NEXT, "page:situational:" + (page + 1)));
                rows.add(pag);
            }
            rows.add(new InlineKeyboardRow(btn("🔍 Filtrlash", "filter_situational:name")));
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_BACK + "admin_menu")));
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(sb.toString())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("showSituationalAnswers: {}", e.getMessage()); }
    }

    public void handleFilterResultsCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String type = callback.getData().substring("filter_results:".length());
        try {
            answerCallback(callback.getId());
            String state = "combined".equals(type) ? "filter_results_combined" : "filter_results_name";
            String prompt = "combined".equals(type)
                ? "Guruh va kichik guruhni kiriting (masalan: 201a, 101b, 302A):"
                : "Talaba ismini kiriting:";
            stateManager.setState(telegramId, state);
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(prompt)
                .replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleFilterResultsCallback: {}", e.getMessage()); }
    }

    public void handleFilterSituationalCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String type = callback.getData().substring("filter_situational:".length());
        try {
            answerCallback(callback.getId());
            String state = "filter_situational_" + type;
            String prompt = "group".equals(type)
                ? "Guruh+kichik guruhni kiriting:" : "Talaba ismini kiriting:";
            stateManager.setState(telegramId, state);
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(prompt)
                .replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleFilterSituationalCallback: {}", e.getMessage()); }
    }

    public void handleFilterResultsInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        String text = message.getText();
        try {
            String state = stateManager.getState(telegramId);
            stateManager.clearState(telegramId);
            String groupName = null, subgroup = null, studentName = null;
            if ("filter_results_combined".equals(state) && text != null && text.length() >= 3) {
                groupName = text.substring(0, text.length() - 1).trim();
                subgroup = text.substring(text.length() - 1).toUpperCase();
            } else if ("filter_results_name".equals(state)) {
                studentName = text;
            }
            List<TestResultWithStudent> results = resultRepository.getTestResultsFiltered(groupName, subgroup, studentName, PAGE_SIZE, 0);
            int total = resultRepository.countTestResultsFiltered(groupName, subgroup, studentName);
            sendTestResultList(chatId, results, total, 1, text);
        } catch (Exception e) { log.error("handleFilterResultsInput: {}", e.getMessage()); }
    }

    public void handleFilterSituationalInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        String text = message.getText();
        try {
            String state = stateManager.getState(telegramId);
            stateManager.clearState(telegramId);
            String groupName = null, subgroupName = null, studentName = null;
            if ("filter_situational_group".equals(state) && text != null && text.length() >= 3) {
                groupName = text.substring(0, text.length() - 1).trim();
                subgroupName = text.substring(text.length() - 1).toUpperCase();
            } else if ("filter_situational_name".equals(state)) {
                studentName = text;
            }
            List<SituationalAnswerWithStudent> answers = resultRepository.getSituationalAnswersFiltered(groupName, subgroupName, studentName, PAGE_SIZE, 0);
            int total = resultRepository.countSituationalAnswersFiltered(groupName, subgroupName, studentName);
            sendSituationalList(chatId, answers, total, 1);
        } catch (Exception e) { log.error("handleFilterSituationalInput: {}", e.getMessage()); }
    }

    public void showStudentResults(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int studentId = Integer.parseInt(callback.getData().substring("student_results:".length()));
            Student s = studentRepository.getStudentById(studentId);
            List<TestResultWithStudent> results = resultRepository.getTestResultsByStudentId(studentId, 20, 0);
            answerCallback(callback.getId());
            StringBuilder sb = new StringBuilder();
            if (s != null) sb.append("📊 ").append(s.getFullName()).append(" natijalari:\n\n");
            if (results.isEmpty()) sb.append("Natijalar topilmadi.");
            else for (TestResultWithStudent r : results)
                sb.append("• ").append(r.getTestTitle()).append(": ").append(r.getScore()).append("/").append(r.getMaxScore()).append("\n");
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(sb.toString())
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_BACK + "students"))).build())
                .build());
        } catch (Exception e) { log.error("showStudentResults: {}", e.getMessage()); }
    }

    public void showStudentTestDetail(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int resultId = Integer.parseInt(callback.getData().substring("test_detail:".length()));
            TestResult r = resultRepository.getTestResultById(resultId);
            if (r == null) return;
            answerCallback(callback.getId());
            String text = "Test natijalari:\nBall: " + r.getScore() + "/" + r.getMaxScore() + "\nStatus: " + r.getStatus();
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL))).build())
                .build());
        } catch (Exception e) { log.error("showStudentTestDetail: {}", e.getMessage()); }
    }

    public void handleGrantRetake(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            // grant_retake:studentId:testId
            String[] parts = callback.getData().substring("grant_retake:".length()).split(":");
            if (parts.length < 2) return;
            int studentId = Integer.parseInt(parts[0]);
            int testId = Integer.parseInt(parts[1]);
            TestRetake retake = new TestRetake();
            retake.setStudentId(studentId);
            retake.setTestId(testId);
            retake.setGrantedBy(callback.getFrom().getId().intValue());
            resultRepository.createTestRetake(retake);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_RETAKE_GRANTED).build());
        } catch (Exception e) { log.error("handleGrantRetake: {}", e.getMessage()); }
    }

    public void viewSituationalAnswer(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int answerId = Integer.parseInt(callback.getData().substring("view_situational:".length()));
            SituationalAnswer a = resultRepository.getSituationalAnswerById(answerId);
            if (a == null) return;
            Student s = studentRepository.getStudentById(a.getStudentId());
            answerCallback(callback.getId());
            String text = "📋 Vaziyatli masala javobini\n\n"
                + "Talaba: " + (s != null ? s.getFullName() : "N/A") + "\n"
                + "Javob:\n" + (a.getAnswerText() != null ? a.getAnswerText() : "") + "\n\n"
                + (a.isGraded() ? "✅ Baho: " + a.getGrade() + "/100\nIzoh: " + a.getFeedback() : "⏳ Baholanmagan");
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(a.isGraded() ? backOnly() : AdminKeyboards.gradingOptions(answerId)).build());
        } catch (Exception e) { log.error("viewSituationalAnswer: {}", e.getMessage()); }
    }

    public void handleGradeSituationalCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int answerId = Integer.parseInt(callback.getData().substring("grade_situational:".length()));
            SituationalAnswer a = resultRepository.getSituationalAnswerById(answerId);
            if (a == null) return;
            answerCallback(callback.getId());

            if (gradingService == null) {
                bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                    .text("AI baholash sozlanmagan. Qo'lda baholang.")
                    .replyMarkup(AdminKeyboards.gradingOptions(answerId)).build());
                return;
            }

            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("🤖 AI baholash...").build());

            SituationalTask task = lessonRepository.getSituationalTaskById(a.getTaskId());
            GradingService.GradingResult gr = gradingService.gradeForLesson(
                task != null ? task.getLessonId() : 0,
                task != null ? task.getTaskText() : "",
                a.getAnswerText());

            AdminGradingData gd = new AdminGradingData();
            gd.setAnswerId(answerId);
            gd.setGrade(gr.getGrade());
            gd.setFeedback(gr.getFeedback());
            gd.setPassed(gr.isPassed());
            var adminRow = adminRepository.getAdminByTelegramId(telegramId);
            gd.setAdminId(adminRow != null ? adminRow.getId() : 0);
            stateManager.setStateWithData(telegramId, StateConstants.ADMIN_GRADING, gd);

            String text = "🤖 AI baholash natijasi:\n\nBaho: " + gr.getGrade() + "/100\nIzoh: " + gr.getFeedback()
                + "\n\nTasdiqlaysizmi?";
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(AdminKeyboards.confirmGrade(answerId, gr.getGrade())).build());
        } catch (Exception e) { log.error("handleGradeSituationalCallback: {}", e.getMessage()); }
    }

    public void handleConfirmGradeCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int answerId = Integer.parseInt(callback.getData().substring("confirm_grade:".length()));
            UserState us = stateManager.getStateWithData(telegramId);
            AdminGradingData gd = stateManager.getStateData(us, AdminGradingData.class);
            if (gd == null) return;
            resultRepository.gradeSituationalAnswer(answerId, gd.getGrade(), gd.getFeedback(), gd.getAdminId());
            stateManager.clearState(telegramId);
            notificationService.notifySituationalGraded(answerId);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text("✅ Baholandi: " + gd.getGrade() + "/100").build());
        } catch (Exception e) { log.error("handleConfirmGradeCallback: {}", e.getMessage()); }
    }

    public void handleCancelGradeCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        stateManager.clearState(telegramId);
        try {
            answerCallback(callback.getId());
            bot.execute(DeleteMessage.builder()
                .chatId(callback.getMessage().getChatId())
                .messageId(callback.getMessage().getMessageId()).build());
        } catch (Exception e) { log.error("handleCancelGradeCallback: {}", e.getMessage()); }
    }

    public void handleManualGradeCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int answerId = Integer.parseInt(callback.getData().substring("manual_grade:".length()));
            AdminGradingData gd = new AdminGradingData();
            gd.setAnswerId(answerId);
            var adminRow = adminRepository.getAdminByTelegramId(telegramId);
            gd.setAdminId(adminRow != null ? adminRow.getId() : 0);
            stateManager.setStateWithData(telegramId, StateConstants.ADMIN_MANUAL_GRADE, gd);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_GRADE + " (0-100)")
                .replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleManualGradeCallback: {}", e.getMessage()); }
    }

    public void handleManualGradeInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            int grade = Integer.parseInt(message.getText().trim());
            if (grade < 0 || grade > 100) { sendText(chatId, "0 dan 100 gacha son kiriting."); return; }
            UserState us = stateManager.getStateWithData(telegramId);
            AdminGradingData gd = stateManager.getStateData(us, AdminGradingData.class);
            if (gd == null) return;
            gd.setGrade(grade);
            stateManager.setStateWithData(telegramId, StateConstants.ADMIN_MANUAL_GRADE_FEEDBACK, gd);
            sendText(chatId, AdminMessages.MSG_ENTER_FEEDBACK);
        } catch (NumberFormatException ex) {
            sendText(chatId, "Son kiriting (0-100).");
        } catch (Exception e) { log.error("handleManualGradeInput: {}", e.getMessage()); }
    }

    public void handleManualGradeFeedbackInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            AdminGradingData gd = stateManager.getStateData(us, AdminGradingData.class);
            if (gd == null) return;
            gd.setFeedback(message.getText().trim());
            resultRepository.gradeSituationalAnswer(gd.getAnswerId(), gd.getGrade(), gd.getFeedback(), gd.getAdminId());
            notificationService.notifySituationalGraded(gd.getAnswerId());
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_ANSWER_GRADED);
        } catch (Exception e) { log.error("handleManualGradeFeedbackInput: {}", e.getMessage()); }
    }

    public boolean[] isManualGrading(long telegramId) {
        String state = stateManager.getState(telegramId);
        boolean isGrading = StateConstants.ADMIN_MANUAL_GRADE.equals(state) || StateConstants.ADMIN_MANUAL_GRADE_FEEDBACK.equals(state);
        return new boolean[]{isGrading, StateConstants.ADMIN_MANUAL_GRADE.equals(state)};
    }

    public boolean isFilteringResults(long telegramId) {
        String state = stateManager.getState(telegramId);
        return state != null && (state.startsWith("filter_results") || state.startsWith("filter_situational"));
    }

    public void showStudentTestHistory(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int studentId = Integer.parseInt(callback.getData().substring("student_test_history:".length()));
            List<TestResultWithStudent> attempts = resultRepository.getTestResultsByStudentId(studentId, 20, 0);
            answerCallback(callback.getId());
            StringBuilder sb = new StringBuilder("📊 Test tarixi:\n\n");
            List<InlineKeyboardRow> rows = new ArrayList<>();
            for (TestResultWithStudent r : attempts) {
                sb.append("• ").append(r.getTestTitle()).append(": ").append(r.getScore()).append("/").append(r.getMaxScore()).append("\n");
                rows.add(new InlineKeyboardRow(btn(r.getTestTitle() + " " + r.getScore() + "/" + r.getMaxScore(), "view_test_attempts:" + r.getId())));
            }
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL)));
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(sb.toString())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("showStudentTestHistory: {}", e.getMessage()); }
    }

    public void showTestAttempts(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int resultId = Integer.parseInt(callback.getData().substring("view_test_attempts:".length()));
            TestResult r = resultRepository.getTestResultById(resultId);
            if (r == null) return;
            answerCallback(callback.getId());
            String text = "Test natijasi:\nBall: " + r.getScore() + "/" + r.getMaxScore() + "\nStatus: " + r.getStatus();
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(backOnly()).build());
        } catch (Exception e) { log.error("showTestAttempts: {}", e.getMessage()); }
    }

    public void showGroupSubgroupLessons(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            answerCallback(callback.getId());
            var units = lessonRepository.getAllUnits();
            List<InlineKeyboardRow> rows = new ArrayList<>();
            for (var u : units) rows.add(new InlineKeyboardRow(btn(u.getName(), "group_lesson:" + u.getId())));
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL)));
            bot.execute(EditMessageText.builder().chatId(chatId).messageId(messageId).text("Fanni tanlang:")
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("showGroupSubgroupLessons: {}", e.getMessage()); }
    }

    public void showGroupSubgroupTestResults(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            answerCallback(callback.getId());
            sendText(chatId, "Bu funksiya guruh+kichik guruh filtridan foydalaning.");
        } catch (Exception e) { log.error("showGroupSubgroupTestResults: {}", e.getMessage()); }
    }

    public void showFilteredByNameResults(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            String[] parts = callback.getData().substring("filter_name_page:".length()).split(":");
            int page = Integer.parseInt(parts[0]);
            String name = parts.length > 1 ? parts[1] : "";
            answerCallback(callback.getId());
            List<TestResultWithStudent> results = resultRepository.getTestResultsFiltered(name, null, null, PAGE_SIZE, (page - 1) * PAGE_SIZE);
            int total = resultRepository.countTestResultsFiltered(name, null, null);
            sendTestResultList(chatId, results, total, page, name);
        } catch (Exception e) { log.error("showFilteredByNameResults: {}", e.getMessage()); }
    }

    private void sendTestResultList(long chatId, List<TestResultWithStudent> results, int total, int page, String filter) {
        try {
            StringBuilder sb = new StringBuilder("📊 Natijalar (" + total + " ta)\n\n");
            List<InlineKeyboardRow> rows = new ArrayList<>();
            for (TestResultWithStudent r : results) {
                sb.append("• ").append(r.getStudentName()).append(" — ")
                  .append(r.getTestTitle()).append(": ").append(r.getScore()).append("/").append(r.getMaxScore()).append("\n");
                rows.add(new InlineKeyboardRow(btn(r.getStudentName() + " " + r.getScore() + "/" + r.getMaxScore(), "test_detail:" + r.getId())));
            }
            int totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
            if (totalPages > 1) {
                InlineKeyboardRow pag = new InlineKeyboardRow();
                if (page > 1) pag.add(btn(AdminMessages.BTN_PREVIOUS, "filter_name_page:" + (page - 1) + ":" + filter));
                pag.add(btn(String.format(AdminMessages.MSG_PAGE, page, totalPages), "noop"));
                if (page < totalPages) pag.add(btn(AdminMessages.BTN_NEXT, "filter_name_page:" + (page + 1) + ":" + filter));
                rows.add(pag);
            }
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL)));
            bot.execute(SendMessage.builder().chatId(chatId).text(sb.toString())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("sendTestResultList: {}", e.getMessage()); }
    }

    private void sendSituationalList(long chatId, List<SituationalAnswerWithStudent> answers, int total, int page) {
        try {
            StringBuilder sb = new StringBuilder("📋 Javoblar (" + total + " ta)\n\n");
            List<InlineKeyboardRow> rows = new ArrayList<>();
            for (SituationalAnswerWithStudent a : answers) {
                String status = a.isGraded() ? "✅" : "⏳";
                sb.append(status).append(" ").append(a.getStudentName()).append("\n");
                rows.add(new InlineKeyboardRow(btn(status + " " + a.getStudentName(), "view_situational:" + a.getId())));
            }
            rows.add(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL)));
            bot.execute(SendMessage.builder().chatId(chatId).text(sb.toString())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("sendSituationalList: {}", e.getMessage()); }
    }

    private InlineKeyboardMarkup backOnly() {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(btn(AdminMessages.MSG_BACK, AdminKeyboards.CB_CANCEL))).build();
    }

    private InlineKeyboardButton btn(String text, String data) {
        return InlineKeyboardButton.builder().text(text).callbackData(data).build();
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
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

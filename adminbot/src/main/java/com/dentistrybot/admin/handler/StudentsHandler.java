package com.dentistrybot.admin.handler;

import com.dentistrybot.admin.keyboard.AdminKeyboards;
import com.dentistrybot.admin.localization.AdminMessages;
import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.StudentFilter;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.AdminFilterData;
import com.dentistrybot.shared.state.EditingStateData;
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

public class StudentsHandler {

    private static final Logger log = LoggerFactory.getLogger(StudentsHandler.class);
    private static final int PAGE_SIZE = 10;

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final StudentRepository studentRepository;
    private final ResultRepository resultRepository;

    public StudentsHandler(TelegramClient bot, StateManager stateManager,
                            StudentRepository studentRepository, ResultRepository resultRepository) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.studentRepository = studentRepository;
        this.resultRepository = resultRepository;
    }

    public void showStudentList(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_FILTER_BY)
                .replyMarkup(AdminKeyboards.studentFilter()).build());
        } catch (Exception e) { log.error("showStudentList: {}", e.getMessage()); }
    }

    public void handleFilterCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String filter = callback.getData().substring(AdminKeyboards.CB_FILTER.length());
        try {
            answerCallback(callback.getId());
            switch (filter) {
                case "name" -> {
                    AdminFilterData fd = new AdminFilterData();
                    stateManager.setStateWithData(telegramId, StateConstants.ADMIN_FILTER_NAME, fd);
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId)
                        .text("Ism bo'yicha qidirish.\n\nIsmni kiriting:").build());
                }
                case "course" -> {
                    AdminFilterData fd = new AdminFilterData();
                    stateManager.setStateWithData(telegramId, StateConstants.ADMIN_FILTER_COURSE, fd);
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId)
                        .text("Kurs bo'yicha qidirish.\n\nKursni kiriting (1-6):").build());
                }
                case "combined" -> {
                    AdminFilterData fd = new AdminFilterData();
                    stateManager.setStateWithData(telegramId, StateConstants.ADMIN_FILTER_GROUP, fd);
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId)
                        .text("Guruh+Kichik guruh bo'yicha qidirish.\n\nGuruhni kiriting (masalan: 101):").build());
                }
                case "faculty" -> {
                    AdminFilterData fd = new AdminFilterData();
                    stateManager.setStateWithData(telegramId, StateConstants.ADMIN_FILTER_FACULTY, fd);
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId)
                        .text("Fakultet bo'yicha qidirish.\n\nFakultet nomini kiriting:").build());
                }
            }
        } catch (Exception e) { log.error("handleFilterCallback: {}", e.getMessage()); }
    }

    public void handleFilterInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        String text = message.getText();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            if (us == null) return;
            AdminFilterData fd = stateManager.getStateData(us, AdminFilterData.class);
            if (fd == null) fd = new AdminFilterData();
            String state = us.getState();

            StudentFilter filter = new StudentFilter();
            switch (state) {
                case StateConstants.ADMIN_FILTER_NAME -> { filter.setFullName(text); fd.setFullName(text); }
                case StateConstants.ADMIN_FILTER_COURSE -> {
                    try { int c = Integer.parseInt(text.trim()); filter.setCourse(c); fd.setCourse(c); }
                    catch (NumberFormatException ex) { sendText(chatId, "Noto'g'ri kurs."); return; }
                }
                case StateConstants.ADMIN_FILTER_GROUP -> { filter.setGroupName(text); fd.setGroupName(text); }
                case StateConstants.ADMIN_FILTER_FACULTY -> { filter.setFaculty(text); fd.setFaculty(text); }
            }

            filter.setLimit(PAGE_SIZE);
            filter.setOffset(0);
            List<Student> students = studentRepository.getStudents(filter);
            int total = studentRepository.countStudents(filter);

            stateManager.clearState(telegramId);
            sendStudentList(chatId, students, total, 1, text, state);
        } catch (Exception e) { log.error("handleFilterInput: {}", e.getMessage()); }
    }

    public void handleStudentViewCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int studentId = Integer.parseInt(callback.getData().substring("student:view:".length()));
            Student s = studentRepository.getStudentById(studentId);
            if (s == null) return;
            answerCallback(callback.getId());
            String text = String.format(AdminMessages.MSG_STUDENT_DETAILS,
                s.getFullName(), s.getCourse(), s.getGroupName(), s.getSubgroup(), s.getFaculty());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(AdminKeyboards.studentActions(s.getId())).build());
        } catch (Exception e) { log.error("handleStudentViewCallback: {}", e.getMessage()); }
    }

    public void handleEditNameCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int studentId = Integer.parseInt(callback.getData().substring("student:edit_name:".length()));
            EditingStateData ed = new EditingStateData();
            ed.setTargetId(studentId);
            ed.setTargetType("student_name");
            stateManager.setStateWithData(telegramId, "edit_student_name", ed);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_ENTER_NEW_NAME)
                .replyMarkup(AdminKeyboards.cancelOnly()).build());
        } catch (Exception e) { log.error("handleEditNameCallback: {}", e.getMessage()); }
    }

    public void handleNameEditInput(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            EditingStateData ed = stateManager.getStateData(us, EditingStateData.class);
            if (ed == null) return;
            studentRepository.updateStudentName(ed.getTargetId(), message.getText().trim());
            stateManager.clearState(telegramId);
            sendText(chatId, AdminMessages.MSG_NAME_UPDATED);
        } catch (Exception e) { log.error("handleNameEditInput: {}", e.getMessage()); }
    }

    public void handleRetakeCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int studentId = Integer.parseInt(callback.getData().substring("student:retake:".length()));
            Student s = studentRepository.getStudentById(studentId);
            if (s == null) return;
            answerCallback(callback.getId());
            // Show tests list for retake grant
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId)
                .text("Test qayta topshirishni berish uchun test ID'sini yuboring:\n\n(Talaba: " + s.getFullName() + ")")
                .build());
        } catch (Exception e) { log.error("handleRetakeCallback: {}", e.getMessage()); }
    }

    public void handleSitRetakeCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int studentId = Integer.parseInt(callback.getData().substring("student:sit_retake:".length()));
            Student s = studentRepository.getStudentById(studentId);
            if (s == null) return;
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId)
                .text("Vaziyatli masala qayta topshirishni berish:\n\nTalaba: " + s.getFullName() + "\n\nMasala ID'sini yuboring:")
                .build());
        } catch (Exception e) { log.error("handleSitRetakeCallback: {}", e.getMessage()); }
    }

    public void handleRetakeGrantCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            // retake:studentId:testId
            String[] parts = callback.getData().substring(AdminKeyboards.CB_RETAKE.length()).split(":");
            if (parts.length < 2) return;
            int studentId = Integer.parseInt(parts[0]);
            int testId = Integer.parseInt(parts[1]);
            long adminTgId = callback.getFrom().getId();

            com.dentistrybot.shared.model.TestRetake retake = new com.dentistrybot.shared.model.TestRetake();
            retake.setStudentId(studentId);
            retake.setTestId(testId);
            retake.setGrantedBy((int) adminTgId);
            resultRepository.createTestRetake(retake);

            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_RETAKE_GRANTED).build());
        } catch (Exception e) { log.error("handleRetakeGrantCallback: {}", e.getMessage()); }
    }

    public void handleSitRetakeGrantCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            // retake:sit:studentId:taskId
            String rest = callback.getData().substring("retake:sit:".length());
            String[] parts = rest.split(":");
            if (parts.length < 2) return;
            int studentId = Integer.parseInt(parts[0]);
            int taskId = Integer.parseInt(parts[1]);
            long adminTgId = callback.getFrom().getId();

            com.dentistrybot.shared.model.SituationalRetake retake = new com.dentistrybot.shared.model.SituationalRetake();
            retake.setStudentId(studentId);
            retake.setTaskId(taskId);
            retake.setGrantedBy((int) adminTgId);
            resultRepository.createSituationalRetake(retake);

            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId)
                .text(AdminMessages.MSG_RETAKE_GRANTED).build());
        } catch (Exception e) { log.error("handleSitRetakeGrantCallback: {}", e.getMessage()); }
    }

    public boolean isEditingStudent(long telegramId) {
        String state = stateManager.getState(telegramId);
        return "edit_student_name".equals(state)
            || StateConstants.ADMIN_FILTER_NAME.equals(state)
            || StateConstants.ADMIN_FILTER_COURSE.equals(state)
            || StateConstants.ADMIN_FILTER_GROUP.equals(state)
            || StateConstants.ADMIN_FILTER_FACULTY.equals(state);
    }

    private void sendStudentList(long chatId, List<Student> students, int total, int page,
                                  String filterValue, String filterType) {
        StringBuilder sb = new StringBuilder("🔍 Qidiruv natijalari: " + total + " ta talaba\n\n");
        if (students.isEmpty()) {
            sb.append(AdminMessages.MSG_NO_STUDENTS_FOUND);
        } else {
            for (int i = 0; i < students.size(); i++) {
                Student s = students.get(i);
                sb.append(i + 1 + ". ").append(s.getFullName())
                  .append(" (").append(s.getCourse()).append("-kurs, ").append(s.getGroupName()).append(")\n");
            }
        }

        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (Student s : students) {
            rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text(s.getFullName()).callbackData("student:view:" + s.getId()).build()));
        }
        int totalPages = (total + PAGE_SIZE - 1) / PAGE_SIZE;
        if (totalPages > 1) {
            InlineKeyboardRow pagRow = new InlineKeyboardRow();
            if (page > 1) pagRow.add(InlineKeyboardButton.builder()
                .text(AdminMessages.BTN_PREVIOUS).callbackData("filter_name_page:" + (page - 1)).build());
            pagRow.add(InlineKeyboardButton.builder()
                .text(String.format(AdminMessages.MSG_PAGE, page, totalPages)).callbackData("noop").build());
            if (page < totalPages) pagRow.add(InlineKeyboardButton.builder()
                .text(AdminMessages.BTN_NEXT).callbackData("filter_name_page:" + (page + 1)).build());
            rows.add(pagRow);
        }
        rows.add(new InlineKeyboardRow(InlineKeyboardButton.builder()
            .text(AdminMessages.MSG_BACK).callbackData(AdminKeyboards.CB_CANCEL).build()));

        try {
            bot.execute(SendMessage.builder().chatId(chatId).text(sb.toString())
                .replyMarkup(InlineKeyboardMarkup.builder().keyboard(rows).build()).build());
        } catch (Exception e) { log.error("sendStudentList: {}", e.getMessage()); }
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

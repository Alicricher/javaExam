package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class ProfileHandler {

    private static final Logger log = LoggerFactory.getLogger(ProfileHandler.class);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final StudentRepository studentRepository;

    public ProfileHandler(TelegramClient bot, StateManager stateManager, StudentRepository studentRepository) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.studentRepository = studentRepository;
    }

    public void showProfile(long chatId, long telegramId) {
        try {
            Student s = studentRepository.getStudentByTelegramId(telegramId);
            if (s == null) { sendText(chatId, UzMessages.MSG_NOT_FOUND); return; }
            String text = String.format(UzMessages.MSG_PROFILE,
                s.getFullName(), s.getCourse(), s.getGroupName(), s.getSubgroup(), s.getFaculty());
            bot.execute(SendMessage.builder()
                .chatId(chatId).text(text).replyMarkup(StudentKeyboards.profileEdit()).build());
        } catch (Exception e) {
            log.error("showProfile error: {}", e.getMessage());
        }
    }

    public void handleEditProfileCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String action = callback.getData().substring(StudentKeyboards.CB_EDIT_PROFILE.length());
        try {
            answerCallback(callback.getId());
            switch (action) {
                case "course" -> {
                    stateManager.setState(telegramId, StateConstants.EDIT_COURSE);
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId)
                        .text(UzMessages.MSG_ENTER_COURSE)
                        .replyMarkup(StudentKeyboards.courseSelection()).build());
                }
                case "group" -> {
                    Student s = studentRepository.getStudentByTelegramId(telegramId);
                    stateManager.setState(telegramId, StateConstants.EDIT_GROUP);
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId)
                        .text(UzMessages.MSG_ENTER_GROUP)
                        .replyMarkup(StudentKeyboards.groupSelection(s != null ? s.getCourse() : 1)).build());
                }
                case "subgroup" -> {
                    stateManager.setState(telegramId, StateConstants.EDIT_SUBGROUP);
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId)
                        .text(UzMessages.MSG_ENTER_SUBGROUP)
                        .replyMarkup(StudentKeyboards.subgroupSelection()).build());
                }
                case "faculty" -> {
                    stateManager.setState(telegramId, StateConstants.EDIT_FACULTY);
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId)
                        .text(UzMessages.MSG_ENTER_FACULTY)
                        .replyMarkup(StudentKeyboards.facultySelection()).build());
                }
            }
        } catch (Exception e) {
            log.error("handleEditProfileCallback error: {}", e.getMessage());
        }
    }

    public void handleEditCourseCallback(CallbackQuery callback, int course) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            Student s = studentRepository.getStudentByTelegramId(telegramId);
            if (s == null) return;
            s.setCourse(course);
            studentRepository.updateStudent(s);
            stateManager.setState(telegramId, StateConstants.EDIT_GROUP);
            answerCallback(callback.getId());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId)
                .text("Kurs o'zgartirildi! Endi guruhni tanlang:")
                .replyMarkup(StudentKeyboards.groupSelection(course)).build());
        } catch (Exception e) {
            log.error("handleEditCourseCallback error: {}", e.getMessage());
        }
    }

    public void handleEditGroupCallback(CallbackQuery callback, String group) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            Student s = studentRepository.getStudentByTelegramId(telegramId);
            if (s == null) return;
            s.setGroupName(group);
            studentRepository.updateStudent(s);
            stateManager.clearState(telegramId);
            answerCallback(callback.getId());
            String text = UzMessages.MSG_PROFILE_UPDATED + "\n\n" + String.format(UzMessages.MSG_PROFILE,
                s.getFullName(), s.getCourse(), s.getGroupName(), s.getSubgroup(), s.getFaculty());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(StudentKeyboards.profileEdit()).build());
        } catch (Exception e) {
            log.error("handleEditGroupCallback error: {}", e.getMessage());
        }
    }

    public void handleEditSubgroupCallback(CallbackQuery callback, String subgroup) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            Student s = studentRepository.getStudentByTelegramId(telegramId);
            if (s == null) return;
            s.setSubgroup(RegistrationHandler.normalizeCyrillicToLatin(subgroup).toUpperCase());
            studentRepository.updateStudent(s);
            stateManager.clearState(telegramId);
            answerCallback(callback.getId());
            String text = UzMessages.MSG_PROFILE_UPDATED + "\n\n" + String.format(UzMessages.MSG_PROFILE,
                s.getFullName(), s.getCourse(), s.getGroupName(), s.getSubgroup(), s.getFaculty());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(StudentKeyboards.profileEdit()).build());
        } catch (Exception e) {
            log.error("handleEditSubgroupCallback error: {}", e.getMessage());
        }
    }

    public void handleEditFacultyCallback(CallbackQuery callback, String faculty) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            Student s = studentRepository.getStudentByTelegramId(telegramId);
            if (s == null) return;
            s.setFaculty(faculty);
            studentRepository.updateStudent(s);
            stateManager.clearState(telegramId);
            answerCallback(callback.getId());
            String text = UzMessages.MSG_PROFILE_UPDATED + "\n\n" + String.format(UzMessages.MSG_PROFILE,
                s.getFullName(), s.getCourse(), s.getGroupName(), s.getSubgroup(), s.getFaculty());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(StudentKeyboards.profileEdit()).build());
        } catch (Exception e) {
            log.error("handleEditFacultyCallback error: {}", e.getMessage());
        }
    }

    public void handleProfileEditText(Message message) {
        long chatId = message.getChatId();
        sendText(chatId, "Iltimos, variantni yuqoridagi tugmalardan tanlang.");
    }

    private void sendText(long chatId, String text) {
        try { bot.execute(SendMessage.builder().chatId(chatId).text(text).build()); }
        catch (Exception e) { log.error("sendText: {}", e.getMessage()); }
    }

    private void answerCallback(String id) {
        try {
            bot.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                .callbackQueryId(id).build());
        } catch (Exception e) { log.error("answerCallback: {}", e.getMessage()); }
    }
}

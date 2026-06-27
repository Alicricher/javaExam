package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class RegistrationHandler {

    private static final Logger log = LoggerFactory.getLogger(RegistrationHandler.class);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final StudentRepository studentRepository;

    public RegistrationHandler(TelegramClient bot, StateManager stateManager, StudentRepository studentRepository) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.studentRepository = studentRepository;
    }

    public static class RegData {
        @JsonProperty("full_name") public String fullName = "";
        @JsonProperty("course") public int course;
        @JsonProperty("group") public String group = "";
        @JsonProperty("subgroup") public String subgroup = "";
    }

    public void startRegistration(long chatId, long telegramId) {
        try {
            stateManager.setState(telegramId, StateConstants.REGISTER_FULL_NAME);
            bot.execute(SendMessage.builder()
                .chatId(chatId)
                .text(UzMessages.MSG_REGISTER_START + "\n\n" + UzMessages.MSG_ENTER_FULL_NAME)
                .build());
        } catch (Exception e) {
            log.error("startRegistration error for {}: {}", telegramId, e.getMessage());
        }
    }

    public void handleRegistrationStep(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        String state = stateManager.getState(telegramId);

        try {
            switch (state) {
                case StateConstants.REGISTER_FULL_NAME -> handleFullName(chatId, telegramId, message.getText());
                case StateConstants.REGISTER_COURSE -> sendText(chatId, "Iltimos, kursni yuqoridagi tugmalardan tanlang.");
                case StateConstants.REGISTER_GROUP -> sendText(chatId, "Iltimos, guruhni yuqoridagi tugmalardan tanlang.");
                case StateConstants.REGISTER_SUBGROUP -> sendText(chatId, "Iltimos, kichik guruhni yuqoridagi tugmalardan tanlang.");
                case StateConstants.REGISTER_FACULTY -> sendText(chatId, "Iltimos, fakultetni yuqoridagi tugmalardan tanlang.");
            }
        } catch (Exception e) {
            log.error("handleRegistrationStep error: {}", e.getMessage());
        }
    }

    public void handleCourseCallback(CallbackQuery callback, int course) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            RegData data = (us != null && us.getStateData() != null)
                ? stateManager.getStateData(us, RegData.class) : new RegData();
            if (data == null) data = new RegData();
            data.course = course;
            stateManager.setStateWithData(telegramId, StateConstants.REGISTER_GROUP, data);
            answerCallback(callback.getId());
            editWithKeyboard(chatId, messageId,
                course + "-kurs tanlandi.\n\n" + UzMessages.MSG_ENTER_GROUP,
                StudentKeyboards.groupSelection(course));
        } catch (Exception e) {
            log.error("handleCourseCallback error: {}", e.getMessage());
        }
    }

    public void handleGroupCallback(CallbackQuery callback, String group) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            RegData data = (us != null && us.getStateData() != null)
                ? stateManager.getStateData(us, RegData.class) : new RegData();
            if (data == null) data = new RegData();
            data.group = group;
            stateManager.setStateWithData(telegramId, StateConstants.REGISTER_SUBGROUP, data);
            answerCallback(callback.getId());
            editWithKeyboard(chatId, messageId,
                group + " guruhi tanlandi.\n\n" + UzMessages.MSG_ENTER_SUBGROUP,
                StudentKeyboards.subgroupSelection());
        } catch (Exception e) {
            log.error("handleGroupCallback error: {}", e.getMessage());
        }
    }

    public void handleSubgroupCallback(CallbackQuery callback, String subgroup) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            RegData data = (us != null && us.getStateData() != null)
                ? stateManager.getStateData(us, RegData.class) : new RegData();
            if (data == null) data = new RegData();
            data.subgroup = normalizeCyrillicToLatin(subgroup).toUpperCase();
            stateManager.setStateWithData(telegramId, StateConstants.REGISTER_FACULTY, data);
            answerCallback(callback.getId());
            editWithKeyboard(chatId, messageId,
                subgroup + " kichik guruhi tanlandi.\n\n" + UzMessages.MSG_ENTER_FACULTY,
                StudentKeyboards.facultySelection());
        } catch (Exception e) {
            log.error("handleSubgroupCallback error: {}", e.getMessage());
        }
    }

    public void handleFacultyCallback(CallbackQuery callback, String faculty) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            RegData data = (us != null && us.getStateData() != null)
                ? stateManager.getStateData(us, RegData.class) : new RegData();
            if (data == null) data = new RegData();
            answerCallback(callback.getId());

            stateManager.clearState(telegramId);
            try { bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build()); } catch (Exception ignored) {}

            if (studentRepository.studentExists(telegramId)) {
                bot.execute(SendMessage.builder().chatId(chatId)
                    .text(UzMessages.MSG_MAIN_MENU).replyMarkup(StudentKeyboards.mainMenu()).build());
                return;
            }

            Student student = new Student();
            student.setTelegramId(telegramId);
            student.setFullName(data.fullName);
            student.setCourse(data.course);
            student.setGroupName(data.group);
            student.setSubgroup(data.subgroup);
            student.setFaculty(faculty);
            studentRepository.createStudent(student);

            String text = String.format(UzMessages.MSG_REGISTER_COMPLETE,
                student.getFullName(), student.getCourse(), student.getGroupName(),
                student.getSubgroup(), student.getFaculty());
            bot.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(StudentKeyboards.mainMenu())
                .build());
        } catch (Exception e) {
            log.error("handleFacultyCallback error: {}", e.getMessage());
        }
    }

    public boolean isInRegistration(long telegramId) {
        String state = stateManager.getState(telegramId);
        return switch (state) {
            case StateConstants.REGISTER_FULL_NAME, StateConstants.REGISTER_COURSE,
                 StateConstants.REGISTER_GROUP, StateConstants.REGISTER_SUBGROUP,
                 StateConstants.REGISTER_FACULTY -> true;
            default -> false;
        };
    }

    private void handleFullName(long chatId, long telegramId, String name) throws Exception {
        if (name == null || name.trim().length() < 3) {
            sendText(chatId, "Ism juda qisqa. Iltimos, to'liq ismingizni kiriting.");
            return;
        }
        RegData data = new RegData();
        data.fullName = name.trim();
        stateManager.setStateWithData(telegramId, StateConstants.REGISTER_COURSE, data);
        bot.execute(SendMessage.builder()
            .chatId(chatId)
            .text(UzMessages.MSG_ENTER_COURSE)
            .replyMarkup(StudentKeyboards.courseSelection())
            .build());
    }

    private void sendText(long chatId, String text) {
        try {
            bot.execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (Exception e) {
            log.error("sendText error: {}", e.getMessage());
        }
    }

    private void answerCallback(String callbackId) {
        try {
            bot.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId).build());
        } catch (Exception e) {
            log.error("answerCallback error: {}", e.getMessage());
        }
    }

    private void editWithKeyboard(long chatId, int messageId, String text, InlineKeyboardMarkup kb) {
        try {
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text).replyMarkup(kb).build());
        } catch (Exception e) {
            log.error("editWithKeyboard error: {}", e.getMessage());
        }
    }

    static String normalizeCyrillicToLatin(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(switch (c) {
                case 'А', 'а' -> 'A';
                case 'В', 'в' -> 'B';
                case 'Е', 'е' -> 'E';
                case 'К', 'к' -> 'K';
                case 'М', 'м' -> 'M';
                case 'Н', 'н' -> 'H';
                case 'О', 'о' -> 'O';
                case 'Р', 'р' -> 'P';
                case 'С', 'с' -> 'C';
                case 'Т', 'т' -> 'T';
                case 'У', 'у' -> 'Y';
                case 'Х', 'х' -> 'X';
                default -> c;
            });
        }
        return sb.toString();
    }
}

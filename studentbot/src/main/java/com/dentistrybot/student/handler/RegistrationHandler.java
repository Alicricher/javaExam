package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.Lang;
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
        @JsonProperty("language") public String language = "uz";
    }

    public void startRegistration(long chatId, long telegramId) {
        try {
            stateManager.setState(telegramId, StateConstants.REGISTER_LANGUAGE);
            bot.execute(SendMessage.builder()
                .chatId(chatId)
                .text(Lang.msgSelectLanguage())
                .replyMarkup(StudentKeyboards.languageSelection())
                .build());
        } catch (Exception e) {
            log.error("startRegistration error for {}: {}", telegramId, e.getMessage());
        }
    }

    public void handleLanguageCallback(CallbackQuery callback, String lang) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            RegData data = new RegData();
            data.language = lang;
            stateManager.setStateWithData(telegramId, StateConstants.REGISTER_FULL_NAME, data);
            answerCallback(callback.getId());
            editWithKeyboard(chatId, messageId,
                Lang.msgRegisterStart(lang) + "\n\n" + Lang.msgEnterFullName(lang),
                null);
        } catch (Exception e) {
            log.error("handleLanguageCallback error: {}", e.getMessage());
        }
    }

    public void handleRegistrationStep(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        String state = stateManager.getState(telegramId);

        try {
            UserState us = stateManager.getStateWithData(telegramId);
            RegData data = (us != null && us.getStateData() != null)
                ? stateManager.getStateData(us, RegData.class) : new RegData();
            if (data == null) data = new RegData();
            String lang = data.language != null ? data.language : "uz";
            switch (state) {
                case StateConstants.REGISTER_LANGUAGE -> sendText(chatId, Lang.msgSelectLanguage());
                case StateConstants.REGISTER_FULL_NAME -> handleFullName(chatId, telegramId, message.getText(), lang, data);
                case StateConstants.REGISTER_COURSE -> sendText(chatId, Lang.msgEnterCourse(lang));
                case StateConstants.REGISTER_GROUP -> sendText(chatId, Lang.msgEnterGroup(lang));
                case StateConstants.REGISTER_SUBGROUP -> sendText(chatId, Lang.msgEnterSubgroup(lang));
                case StateConstants.REGISTER_FACULTY -> sendText(chatId, Lang.msgEnterFaculty(lang));
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
            String lang1 = data.language != null ? data.language : "uz";
            stateManager.setStateWithData(telegramId, StateConstants.REGISTER_GROUP, data);
            answerCallback(callback.getId());
            editWithKeyboard(chatId, messageId,
                Lang.msg(lang1, course + "-kurs tanlandi.", course + "-й курс выбран.") + "\n\n" + Lang.msgEnterGroup(lang1),
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
            String lang2 = data.language != null ? data.language : "uz";
            stateManager.setStateWithData(telegramId, StateConstants.REGISTER_SUBGROUP, data);
            answerCallback(callback.getId());
            editWithKeyboard(chatId, messageId,
                Lang.msg(lang2, group + " guruhi tanlandi.", "Группа " + group + " выбрана.") + "\n\n" + Lang.msgEnterSubgroup(lang2),
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
            String lang3 = data.language != null ? data.language : "uz";
            stateManager.setStateWithData(telegramId, StateConstants.REGISTER_FACULTY, data);
            answerCallback(callback.getId());
            editWithKeyboard(chatId, messageId,
                Lang.msg(lang3, subgroup + " kichik guruhi tanlandi.", "Подгруппа " + subgroup + " выбрана.") + "\n\n" + Lang.msgEnterFaculty(lang3),
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

            String lang = data.language != null ? data.language : "uz";

            if (studentRepository.studentExists(telegramId)) {
                bot.execute(SendMessage.builder().chatId(chatId)
                    .text(Lang.msgMainMenu(lang)).replyMarkup(StudentKeyboards.mainMenu(lang)).build());
                return;
            }

            Student student = new Student();
            student.setTelegramId(telegramId);
            student.setFullName(data.fullName);
            student.setCourse(data.course);
            student.setGroupName(data.group);
            student.setSubgroup(data.subgroup);
            student.setFaculty(faculty);
            student.setLanguage(lang);
            studentRepository.createStudent(student);

            String text = Lang.msgRegisterComplete(lang,
                student.getFullName(), student.getCourse(), student.getGroupName(),
                student.getSubgroup(), student.getFaculty());
            bot.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .replyMarkup(StudentKeyboards.mainMenu(lang))
                .build());
        } catch (Exception e) {
            log.error("handleFacultyCallback error: {}", e.getMessage());
        }
    }

    public boolean isInRegistration(long telegramId) {
        String state = stateManager.getState(telegramId);
        return switch (state) {
            case StateConstants.REGISTER_LANGUAGE, StateConstants.REGISTER_FULL_NAME,
                 StateConstants.REGISTER_COURSE, StateConstants.REGISTER_GROUP,
                 StateConstants.REGISTER_SUBGROUP, StateConstants.REGISTER_FACULTY -> true;
            default -> false;
        };
    }

    private void handleFullName(long chatId, long telegramId, String name, String lang, RegData existing) throws Exception {
        if (name == null || name.trim().length() < 3) {
            sendText(chatId, Lang.msg(lang, "Ism juda qisqa. Iltimos, to'liq ismingizni kiriting.", "Имя слишком короткое. Пожалуйста, введите полное имя."));
            return;
        }
        existing.fullName = name.trim();
        stateManager.setStateWithData(telegramId, StateConstants.REGISTER_COURSE, existing);
        bot.execute(SendMessage.builder()
            .chatId(chatId)
            .text(Lang.msgEnterCourse(lang))
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
            InlineKeyboardMarkup markup = kb != null ? kb : InlineKeyboardMarkup.builder().build();
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text).replyMarkup(markup).build());
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

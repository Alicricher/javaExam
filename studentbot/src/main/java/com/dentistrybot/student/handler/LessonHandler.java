package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.Unit;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.LessonMenuStateData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

public class LessonHandler {

    private static final Logger log = LoggerFactory.getLogger(LessonHandler.class);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final LessonRepository lessonRepository;
    private final StudentRepository studentRepository;

    public LessonHandler(TelegramClient bot, StateManager stateManager, LessonRepository lessonRepository,
                          StudentRepository studentRepository) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.lessonRepository = lessonRepository;
        this.studentRepository = studentRepository;
    }

    private String langFor(long telegramId) {
        Student s = studentRepository.getStudentByTelegramId(telegramId);
        return s != null ? s.getLanguage() : "uz";
    }

    public void showUnits(long chatId) { showUnits(chatId, "uz"); }

    public void showUnits(long chatId, String lang) {
        try {
            List<Unit> units = lessonRepository.getAllUnits();
            bot.execute(SendMessage.builder()
                .chatId(chatId)
                .text(Lang.msgSelectUnit(lang))
                .replyMarkup(StudentKeyboards.units(units, lang))
                .build());
        } catch (Exception e) {
            log.error("showUnits error: {}", e.getMessage());
        }
    }

    public void handleUnitCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            int unitId = Integer.parseInt(callback.getData().substring(StudentKeyboards.CB_UNIT.length()));
            Unit unit = lessonRepository.getUnitById(unitId);
            List<Lesson> lessons = lessonRepository.getLessonsByUnitId(unitId);
            stateManager.setStateWithData(telegramId, StateConstants.SELECT_LESSON,
                new LessonMenuStateData(unitId, 0));
            answerCallback(callback.getId());
            String text = unit.getName() + " - " + unit.titleFor(lang) + "\n\n" + Lang.msgSelectLesson(lang);
            EditMessageText edit = EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(lessons.isEmpty() ? null : StudentKeyboards.lessons(lessons, unitId, lang))
                .build();
            bot.execute(edit);
        } catch (Exception e) {
            log.error("handleUnitCallback error: {}", e.getMessage());
        }
    }

    public void handleLessonCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            int lessonId = Integer.parseInt(callback.getData().substring(StudentKeyboards.CB_LESSON.length()));
            Lesson lesson = lessonRepository.getLessonById(lessonId);
            Unit unit = lessonRepository.getUnitById(lesson.getUnitId());
            stateManager.setStateWithData(telegramId, StateConstants.LESSON_MENU,
                new LessonMenuStateData(lesson.getUnitId(), lessonId));
            answerCallback(callback.getId());
            String text = Lang.msgLessonMenu(lang,
                unit.getName() + " - " + lesson.getLessonNumber() + "-dars", lesson.titleFor(lang));
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(StudentKeyboards.lessonMenu(lessonId, lesson.getUnitId(), lang)).build());
        } catch (Exception e) {
            log.error("handleLessonCallback error: {}", e.getMessage());
        }
    }

    public void handleBackCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        String backTarget = callback.getData().substring(StudentKeyboards.CB_BACK.length());
        try {
            answerCallback(callback.getId());
            if ("units".equals(backTarget)) {
                List<Unit> units = lessonRepository.getAllUnits();
                stateManager.setState(telegramId, StateConstants.SELECT_UNIT);
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(Lang.msgSelectUnit(lang))
                    .replyMarkup(StudentKeyboards.units(units, lang)).build());

            } else if (backTarget.startsWith("lessons:")) {
                int unitId = Integer.parseInt(backTarget.substring("lessons:".length()));
                Unit unit = lessonRepository.getUnitById(unitId);
                List<Lesson> lessons = lessonRepository.getLessonsByUnitId(unitId);
                stateManager.setStateWithData(telegramId, StateConstants.SELECT_LESSON,
                    new LessonMenuStateData(unitId, 0));
                String text = unit.getName() + " - " + unit.titleFor(lang) + "\n\n" + Lang.msgSelectLesson(lang);
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(text)
                    .replyMarkup(StudentKeyboards.lessons(lessons, unitId, lang)).build());

            } else if (backTarget.startsWith("lesson:")) {
                int lessonId = Integer.parseInt(backTarget.substring("lesson:".length()));
                Lesson lesson = lessonRepository.getLessonById(lessonId);
                Unit unit = lessonRepository.getUnitById(lesson.getUnitId());
                stateManager.setStateWithData(telegramId, StateConstants.LESSON_MENU,
                    new LessonMenuStateData(lesson.getUnitId(), lessonId));
                String text = Lang.msgLessonMenu(lang,
                    unit.getName() + " - " + lesson.getLessonNumber() + "-dars", lesson.titleFor(lang));
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(text)
                    .replyMarkup(StudentKeyboards.lessonMenu(lessonId, lesson.getUnitId(), lang)).build());

            } else if ("main".equals(backTarget)) {
                bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                stateManager.clearState(telegramId);
                bot.execute(SendMessage.builder()
                    .chatId(chatId).text(Lang.msgMainMenu(lang))
                    .replyMarkup(StudentKeyboards.mainMenu(lang)).build());
            }
        } catch (Exception e) {
            log.error("handleBackCallback error: {}", e.getMessage());
        }
    }

    private void answerCallback(String id) {
        try {
            bot.execute(AnswerCallbackQuery.builder().callbackQueryId(id).build());
        } catch (Exception e) { log.error("answerCallback: {}", e.getMessage()); }
    }
}

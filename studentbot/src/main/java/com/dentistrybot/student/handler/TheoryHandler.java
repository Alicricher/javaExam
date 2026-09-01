package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.model.TheoryMaterial;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.FileService;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.Lang;
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

import java.util.List;

public class TheoryHandler {

    private static final Logger log = LoggerFactory.getLogger(TheoryHandler.class);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final LessonRepository lessonRepository;
    private final FileService fileService;
    private final StudentRepository studentRepository;

    public TheoryHandler(TelegramClient bot, StateManager stateManager,
                         LessonRepository lessonRepository, FileService fileService,
                         StudentRepository studentRepository) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.lessonRepository = lessonRepository;
        this.fileService = fileService;
        this.studentRepository = studentRepository;
    }

    private String langFor(long telegramId) {
        Student s = studentRepository.getStudentByTelegramId(telegramId);
        return s != null ? s.getLanguage() : "uz";
    }

    public void handleTheoryCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            int lessonId = Integer.parseInt(callback.getData().substring(StudentKeyboards.CB_THEORY.length()));
            answerCallback(callback.getId());

            List<TheoryMaterial> materials = lessonRepository.getTheoryMaterialsByLessonId(lessonId);
            if (materials.isEmpty()) {
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(Lang.msgNoTheory(lang))
                    .replyMarkup(backToLesson(lessonId, lang)).build());
                return;
            }

            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
            sendText(chatId, Lang.msgTheoryMaterialsHeader(lang));

            for (TheoryMaterial m : materials) {
                if (m.getFilePath() != null && !m.getFilePath().isEmpty() && fileService.fileExists(m.getFilePath())) {
                    try {
                        fileService.sendDocument(bot, chatId, m.getFilePath(), m.getTitleUz());
                    } catch (Exception ex) {
                        sendText(chatId, Lang.msgFileUploadError(lang, m.getTitleUz()));
                    }
                } else if (m.getDescription() != null && !m.getDescription().isEmpty()) {
                    bot.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("*" + m.getTitleUz() + "*\n\n" + m.getDescription())
                        .parseMode("Markdown").build());
                }
            }

            bot.execute(SendMessage.builder()
                .chatId(chatId).text(Lang.msgBackToLessonHint(lang))
                .replyMarkup(backToLesson(lessonId, lang)).build());
        } catch (Exception e) {
            log.error("handleTheoryCallback error: {}", e.getMessage());
        }
    }

    public void handleMaterialTypeCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            String data = callback.getData().substring(StudentKeyboards.CB_MAT_TYPE.length());
            String[] parts = data.split(":");
            if (parts.length != 2) return;
            int lessonId = Integer.parseInt(parts[0]);
            String type = parts[1];
            answerCallback(callback.getId());

            List<TheoryMaterial> all = lessonRepository.getTheoryMaterialsByLessonId(lessonId);
            long count = all.stream().filter(m -> type.equals(m.getMaterialType())).count();
            String typeName = switch (type) {
                case "book" -> Lang.msgBooks(lang);
                case "manual" -> Lang.msgManuals(lang);
                default -> Lang.msgMaterials(lang);
            };
            if (count == 0) {
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(Lang.msgTypeNotAvailable(lang, typeName))
                    .replyMarkup(StudentKeyboards.theoryTypes(lessonId, lang)).build());
                return;
            }

            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(typeName + ":")
                .replyMarkup(StudentKeyboards.materialList(all, type, lessonId, lang)).build());
        } catch (Exception e) {
            log.error("handleMaterialTypeCallback error: {}", e.getMessage());
        }
    }

    public void handleMaterialCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            int materialId = Integer.parseInt(callback.getData().substring(StudentKeyboards.CB_MATERIAL.length()));
            answerCallback(callback.getId());

            TheoryMaterial m = lessonRepository.getTheoryMaterialById(materialId);
            if (m == null) {
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(Lang.msgMaterialNotFound(lang)).build());
                return;
            }

            if (m.getFilePath() != null && !m.getFilePath().isEmpty() && fileService.fileExists(m.getFilePath())) {
                fileService.sendDocument(bot, chatId, m.getFilePath(), m.getTitleUz());
            } else if (m.getDescription() != null && !m.getDescription().isEmpty()) {
                bot.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("*" + m.getTitleUz() + "*\n\n" + m.getDescription())
                    .parseMode("Markdown").build());
            } else {
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(Lang.msgMaterialNotAvailable(lang)).build());
            }
        } catch (Exception e) {
            log.error("handleMaterialCallback error: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup backToLesson(int lessonId, String lang) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text(Lang.btnBack(lang))
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

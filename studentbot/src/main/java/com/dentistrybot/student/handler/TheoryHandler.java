package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.TheoryMaterial;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.service.FileService;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
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

    public TheoryHandler(TelegramClient bot, StateManager stateManager,
                         LessonRepository lessonRepository, FileService fileService) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.lessonRepository = lessonRepository;
        this.fileService = fileService;
    }

    public void handleTheoryCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int lessonId = Integer.parseInt(callback.getData().substring(StudentKeyboards.CB_THEORY.length()));
            answerCallback(callback.getId());

            List<TheoryMaterial> materials = lessonRepository.getTheoryMaterialsByLessonId(lessonId);
            if (materials.isEmpty()) {
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(UzMessages.MSG_NO_THEORY)
                    .replyMarkup(backToLesson(lessonId)).build());
                return;
            }

            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
            sendText(chatId, "📚 Nazariy materiallar:");

            for (TheoryMaterial m : materials) {
                if (m.getFilePath() != null && !m.getFilePath().isEmpty() && fileService.fileExists(m.getFilePath())) {
                    try {
                        fileService.sendDocument(bot, chatId, m.getFilePath(), m.getTitleUz());
                    } catch (Exception ex) {
                        sendText(chatId, "❌ " + m.getTitleUz() + " - faylni yuklashda xatolik");
                    }
                } else if (m.getDescription() != null && !m.getDescription().isEmpty()) {
                    bot.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("*" + m.getTitleUz() + "*\n\n" + m.getDescription())
                        .parseMode("Markdown").build());
                }
            }

            bot.execute(SendMessage.builder()
                .chatId(chatId).text("⬆️ Darsga qaytish uchun tugmani bosing.")
                .replyMarkup(backToLesson(lessonId)).build());
        } catch (Exception e) {
            log.error("handleTheoryCallback error: {}", e.getMessage());
        }
    }

    public void handleMaterialTypeCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            String data = callback.getData().substring(StudentKeyboards.CB_MAT_TYPE.length());
            String[] parts = data.split(":");
            if (parts.length != 2) return;
            int lessonId = Integer.parseInt(parts[0]);
            String type = parts[1];
            answerCallback(callback.getId());

            List<TheoryMaterial> all = lessonRepository.getTheoryMaterialsByLessonId(lessonId);
            long count = all.stream().filter(m -> type.equals(m.getMaterialType())).count();
            if (count == 0) {
                String typeName = switch (type) {
                    case "book" -> UzMessages.MSG_BOOKS;
                    case "manual" -> UzMessages.MSG_MANUALS;
                    default -> UzMessages.MSG_MATERIALS;
                };
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(typeName + " mavjud emas.")
                    .replyMarkup(StudentKeyboards.theoryTypes(lessonId)).build());
                return;
            }

            String typeName = switch (type) {
                case "book" -> UzMessages.MSG_BOOKS;
                case "manual" -> UzMessages.MSG_MANUALS;
                default -> UzMessages.MSG_MATERIALS;
            };
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(typeName + ":")
                .replyMarkup(StudentKeyboards.materialList(all, type, lessonId)).build());
        } catch (Exception e) {
            log.error("handleMaterialTypeCallback error: {}", e.getMessage());
        }
    }

    public void handleMaterialCallback(CallbackQuery callback) {
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        try {
            int materialId = Integer.parseInt(callback.getData().substring(StudentKeyboards.CB_MATERIAL.length()));
            answerCallback(callback.getId());

            TheoryMaterial m = lessonRepository.getTheoryMaterialById(materialId);
            if (m == null) {
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text("Material topilmadi.").build());
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
                    .chatId(chatId).messageId(messageId).text("Material mavjud emas.").build());
            }
        } catch (Exception e) {
            log.error("handleMaterialCallback error: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup backToLesson(int lessonId) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text(UzMessages.BTN_BACK)
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

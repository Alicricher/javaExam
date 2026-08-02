package com.dentistrybot.student.handler;

import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * Общие помощники для тестов studentbot-обработчиков — вынесены из TestHandlerTest,
 * чтобы не дублировать сборку CallbackQuery/Message и capture-хелпер в каждом файле.
 */
abstract class HandlerTestSupport {

    static final long TELEGRAM_ID = 111222333L;
    static final long CHAT_ID = 111222333L;
    static final int MESSAGE_ID = 555;

    static CallbackQuery callbackWithData(String data) {
        Chat chat = Chat.builder().id(CHAT_ID).type("private").build();
        Message message = Message.builder().messageId(MESSAGE_ID).chat(chat).build();
        User from = User.builder().id(TELEGRAM_ID).firstName("QA").isBot(false).build();
        CallbackQuery cq = new CallbackQuery();
        cq.setId("cbq-1");
        cq.setFrom(from);
        cq.setMessage(message);
        cq.setData(data);
        return cq;
    }

    static Message textMessage(String text) {
        Chat chat = Chat.builder().id(CHAT_ID).type("private").build();
        User from = User.builder().id(TELEGRAM_ID).firstName("QA").isBot(false).build();
        return Message.builder().messageId(MESSAGE_ID).chat(chat).from(from).text(text).build();
    }

    static Message photoMessage(List<PhotoSize> photo) {
        Chat chat = Chat.builder().id(CHAT_ID).type("private").build();
        User from = User.builder().id(TELEGRAM_ID).firstName("QA").isBot(false).build();
        return Message.builder().messageId(MESSAGE_ID).chat(chat).from(from).photo(photo).build();
    }

    /** execute() один и тот же generic-метод для всех BotApiMethod — capture() матчит
     * любой вызов, фильтровать по конкретному типу приходится вручную. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static <T> List<T> executedOf(TelegramClient bot, Class<T> type) throws Exception {
        ArgumentCaptor<BotApiMethod> captor = ArgumentCaptor.forClass(BotApiMethod.class);
        verify(bot, atLeastOnce()).execute(captor.capture());
        return captor.getAllValues().stream().filter(type::isInstance).map(type::cast).toList();
    }
}

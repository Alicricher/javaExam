package com.dentistrybot.admin.bot;

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public class AdminBot implements LongPollingUpdateConsumer {

    private final AdminUpdateDispatcher dispatcher;

    public AdminBot(AdminUpdateDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(dispatcher::dispatch);
    }
}

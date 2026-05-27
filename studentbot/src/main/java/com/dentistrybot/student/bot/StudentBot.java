package com.dentistrybot.student.bot;

import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

public class StudentBot implements LongPollingUpdateConsumer {

    private final StudentUpdateDispatcher dispatcher;

    public StudentBot(StudentUpdateDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(dispatcher::dispatch);
    }
}

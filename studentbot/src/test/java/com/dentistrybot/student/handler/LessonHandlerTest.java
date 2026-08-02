package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.Lesson;
import com.dentistrybot.shared.model.Unit;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.LessonMenuStateData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LessonHandlerTest extends HandlerTestSupport {

    private TelegramClient bot;
    private StateManager stateManager;
    private LessonRepository lessonRepository;
    private LessonHandler handler;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramClient.class);
        stateManager = mock(StateManager.class);
        lessonRepository = mock(LessonRepository.class);
        handler = new LessonHandler(bot, stateManager, lessonRepository);
    }

    private Unit unit(int id, String name, String titleUz) {
        Unit u = new Unit();
        u.setId(id);
        u.setName(name);
        u.setTitleUz(titleUz);
        return u;
    }

    private Lesson lesson(int id, int unitId, int number, String titleUz) {
        Lesson l = new Lesson();
        l.setId(id);
        l.setUnitId(unitId);
        l.setLessonNumber(number);
        l.setTitleUz(titleUz);
        return l;
    }

    @org.junit.jupiter.api.Test
    void showUnits_sendsUnitListMessage() throws Exception {
        when(lessonRepository.getAllUnits()).thenReturn(List.of(unit(1, "F1", "Birinchi bo'lim")));

        handler.showUnits(CHAT_ID);

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getText()).isEqualTo(UzMessages.MSG_SELECT_UNIT);
    }

    @org.junit.jupiter.api.Test
    void handleUnitCallback_setsStateAndShowsLessons() throws Exception {
        when(lessonRepository.getUnitById(1)).thenReturn(unit(1, "F1", "Birinchi bo'lim"));
        when(lessonRepository.getLessonsByUnitId(1)).thenReturn(List.of(lesson(10, 1, 1, "Tish anatomiyasi")));

        handler.handleUnitCallback(callbackWithData(StudentKeyboards.CB_UNIT + "1"));

        ArgumentCaptor<LessonMenuStateData> captor = ArgumentCaptor.forClass(LessonMenuStateData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.SELECT_LESSON), captor.capture());
        assertThat(captor.getValue().getUnitId()).isEqualTo(1);
        assertThat(captor.getValue().getLessonId()).isZero();

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains("F1 - Birinchi bo'lim");
    }

    @org.junit.jupiter.api.Test
    void handleLessonCallback_setsStateAndShowsLessonMenu() throws Exception {
        when(lessonRepository.getLessonById(10)).thenReturn(lesson(10, 1, 1, "Tish anatomiyasi"));
        when(lessonRepository.getUnitById(1)).thenReturn(unit(1, "F1", "Birinchi bo'lim"));

        handler.handleLessonCallback(callbackWithData(StudentKeyboards.CB_LESSON + "10"));

        ArgumentCaptor<LessonMenuStateData> captor = ArgumentCaptor.forClass(LessonMenuStateData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.LESSON_MENU), captor.capture());
        assertThat(captor.getValue().getUnitId()).isEqualTo(1);
        assertThat(captor.getValue().getLessonId()).isEqualTo(10);

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains("Tish anatomiyasi");
    }

    @org.junit.jupiter.api.Test
    void handleBackCallback_units_showsUnitListAndSetsState() throws Exception {
        when(lessonRepository.getAllUnits()).thenReturn(List.of(unit(1, "F1", "Birinchi bo'lim")));

        handler.handleBackCallback(callbackWithData(StudentKeyboards.CB_BACK + "units"));

        verify(stateManager).setState(TELEGRAM_ID, StateConstants.SELECT_UNIT);
        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).isEqualTo(UzMessages.MSG_SELECT_UNIT);
    }

    @org.junit.jupiter.api.Test
    void handleBackCallback_lessons_showsLessonListForUnit() throws Exception {
        when(lessonRepository.getUnitById(1)).thenReturn(unit(1, "F1", "Birinchi bo'lim"));
        when(lessonRepository.getLessonsByUnitId(1)).thenReturn(List.of(lesson(10, 1, 1, "Tish anatomiyasi")));

        handler.handleBackCallback(callbackWithData(StudentKeyboards.CB_BACK + "lessons:1"));

        ArgumentCaptor<LessonMenuStateData> captor = ArgumentCaptor.forClass(LessonMenuStateData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.SELECT_LESSON), captor.capture());
        assertThat(captor.getValue().getUnitId()).isEqualTo(1);
    }

    @org.junit.jupiter.api.Test
    void handleBackCallback_lesson_showsLessonMenu() throws Exception {
        when(lessonRepository.getLessonById(10)).thenReturn(lesson(10, 1, 1, "Tish anatomiyasi"));
        when(lessonRepository.getUnitById(1)).thenReturn(unit(1, "F1", "Birinchi bo'lim"));

        handler.handleBackCallback(callbackWithData(StudentKeyboards.CB_BACK + "lesson:10"));

        ArgumentCaptor<LessonMenuStateData> captor = ArgumentCaptor.forClass(LessonMenuStateData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.LESSON_MENU), captor.capture());
        assertThat(captor.getValue().getLessonId()).isEqualTo(10);
    }

    @org.junit.jupiter.api.Test
    void handleBackCallback_main_clearsStateDeletesMessageAndShowsMainMenu() throws Exception {
        handler.handleBackCallback(callbackWithData(StudentKeyboards.CB_BACK + "main"));

        verify(stateManager).clearState(TELEGRAM_ID);
        List<DeleteMessage> deletes = executedOf(bot, DeleteMessage.class);
        assertThat(deletes).hasSize(1);
        assertThat(deletes.get(0).getMessageId()).isEqualTo(MESSAGE_ID);

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().equals(UzMessages.MSG_MAIN_MENU));
    }
}

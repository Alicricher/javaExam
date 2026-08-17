package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import com.dentistrybot.student.localization.Lang;
import com.dentistrybot.student.localization.UzMessages;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrationHandlerTest extends HandlerTestSupport {

    private TelegramClient bot;
    private StateManager stateManager;
    private StudentRepository studentRepository;
    private RegistrationHandler handler;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramClient.class);
        stateManager = mock(StateManager.class);
        studentRepository = mock(StudentRepository.class);
        handler = new RegistrationHandler(bot, stateManager, studentRepository);
    }

    @org.junit.jupiter.api.Test
    void startRegistration_setsLanguageStateAndSendsLanguageKeyboard() throws Exception {
        handler.startRegistration(CHAT_ID, TELEGRAM_ID);

        verify(stateManager).setState(TELEGRAM_ID, StateConstants.REGISTER_LANGUAGE);
        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getText()).isEqualTo(Lang.msgSelectLanguage());
    }

    @org.junit.jupiter.api.Test
    void handleLanguageCallback_uz_setsFullNameStateAndEditsMessage() throws Exception {
        handler.handleLanguageCallback(callbackWithData(""), "uz");

        ArgumentCaptor<RegistrationHandler.RegData> captor = ArgumentCaptor.forClass(RegistrationHandler.RegData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.REGISTER_FULL_NAME), captor.capture());
        assertThat(captor.getValue().language).isEqualTo("uz");

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains(Lang.msgEnterFullName("uz"));
    }

    @org.junit.jupiter.api.Test
    void handleLanguageCallback_ru_setsFullNameStateAndEditsMessage() throws Exception {
        handler.handleLanguageCallback(callbackWithData(""), "ru");

        ArgumentCaptor<RegistrationHandler.RegData> captor = ArgumentCaptor.forClass(RegistrationHandler.RegData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.REGISTER_FULL_NAME), captor.capture());
        assertThat(captor.getValue().language).isEqualTo("ru");

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains(Lang.msgEnterFullName("ru"));
    }

    @org.junit.jupiter.api.Test
    void isInRegistration_trueForLanguageState() {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.REGISTER_LANGUAGE);
        assertThat(handler.isInRegistration(TELEGRAM_ID)).isTrue();
    }

    @org.junit.jupiter.api.Test
    void handleRegistrationStep_fullNameTooShort_asksAgainWithoutStateChange() throws Exception {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.REGISTER_FULL_NAME);

        handler.handleRegistrationStep(textMessage("Al"));

        verify(stateManager, never()).setStateWithData(eq(TELEGRAM_ID), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).hasSize(1);
        assertThat(sent.get(0).getText()).contains("juda qisqa");
    }

    @org.junit.jupiter.api.Test
    void handleRegistrationStep_validFullName_advancesToCourseSelection() throws Exception {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.REGISTER_FULL_NAME);

        handler.handleRegistrationStep(textMessage("Ali Valiyev"));

        ArgumentCaptor<RegistrationHandler.RegData> captor = ArgumentCaptor.forClass(RegistrationHandler.RegData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.REGISTER_COURSE), captor.capture());
        assertThat(captor.getValue().fullName).isEqualTo("Ali Valiyev");

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent.get(0).getText()).isEqualTo(UzMessages.MSG_ENTER_COURSE);
    }

    @org.junit.jupiter.api.Test
    void handleCourseCallback_savesCourseAndAdvancesToGroup() throws Exception {
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(null);

        handler.handleCourseCallback(callbackWithData(""), 3);

        ArgumentCaptor<RegistrationHandler.RegData> captor = ArgumentCaptor.forClass(RegistrationHandler.RegData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.REGISTER_GROUP), captor.capture());
        assertThat(captor.getValue().course).isEqualTo(3);

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains("3-kurs tanlandi");
    }

    @org.junit.jupiter.api.Test
    void handleGroupCallback_savesGroupAndAdvancesToSubgroup() throws Exception {
        UserState us = new UserState();
        us.setStateData("{}");
        RegistrationHandler.RegData existing = new RegistrationHandler.RegData();
        existing.fullName = "Ali Valiyev";
        existing.course = 3;
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, RegistrationHandler.RegData.class)).thenReturn(existing);

        handler.handleGroupCallback(callbackWithData(""), "301");

        ArgumentCaptor<RegistrationHandler.RegData> captor = ArgumentCaptor.forClass(RegistrationHandler.RegData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.REGISTER_SUBGROUP), captor.capture());
        assertThat(captor.getValue().group).isEqualTo("301");
        assertThat(captor.getValue().fullName).isEqualTo("Ali Valiyev");
    }

    @org.junit.jupiter.api.Test
    void handleSubgroupCallback_normalizesCyrillicAndUppercases() throws Exception {
        UserState us = new UserState();
        us.setStateData("{}");
        RegistrationHandler.RegData existing = new RegistrationHandler.RegData();
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, RegistrationHandler.RegData.class)).thenReturn(existing);

        handler.handleSubgroupCallback(callbackWithData(""), "а"); // cyrillic 'а'

        ArgumentCaptor<RegistrationHandler.RegData> captor = ArgumentCaptor.forClass(RegistrationHandler.RegData.class);
        verify(stateManager).setStateWithData(eq(TELEGRAM_ID), eq(StateConstants.REGISTER_FACULTY), captor.capture());
        assertThat(captor.getValue().subgroup).isEqualTo("A");
    }

    @org.junit.jupiter.api.Test
    void handleFacultyCallback_newStudent_createsStudentAndShowsMainMenu() throws Exception {
        UserState us = new UserState();
        us.setStateData("{}");
        RegistrationHandler.RegData data = new RegistrationHandler.RegData();
        data.fullName = "Ali Valiyev";
        data.course = 3;
        data.group = "301";
        data.subgroup = "A";
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, RegistrationHandler.RegData.class)).thenReturn(data);
        when(studentRepository.studentExists(TELEGRAM_ID)).thenReturn(false);

        handler.handleFacultyCallback(callbackWithData(""), "Stomatologiya");

        verify(stateManager).clearState(TELEGRAM_ID);
        ArgumentCaptor<Student> studentCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).createStudent(studentCaptor.capture());
        Student created = studentCaptor.getValue();
        assertThat(created.getTelegramId()).isEqualTo(TELEGRAM_ID);
        assertThat(created.getFullName()).isEqualTo("Ali Valiyev");
        assertThat(created.getCourse()).isEqualTo(3);
        assertThat(created.getGroupName()).isEqualTo("301");
        assertThat(created.getSubgroup()).isEqualTo("A");
        assertThat(created.getFaculty()).isEqualTo("Stomatologiya");

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().contains("Ali Valiyev"));
    }

    @org.junit.jupiter.api.Test
    void handleFacultyCallback_existingStudent_skipsCreationShowsMainMenu() throws Exception {
        UserState us = new UserState();
        us.setStateData("{}");
        RegistrationHandler.RegData data = new RegistrationHandler.RegData();
        when(stateManager.getStateWithData(TELEGRAM_ID)).thenReturn(us);
        when(stateManager.getStateData(us, RegistrationHandler.RegData.class)).thenReturn(data);
        when(studentRepository.studentExists(TELEGRAM_ID)).thenReturn(true);

        handler.handleFacultyCallback(callbackWithData(""), "Stomatologiya");

        verify(studentRepository, never()).createStudent(org.mockito.ArgumentMatchers.any());
        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(m -> m.getText().equals(UzMessages.MSG_MAIN_MENU));
    }

    @org.junit.jupiter.api.Test
    void isInRegistration_trueDuringRegistrationStates() {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.REGISTER_GROUP);
        assertThat(handler.isInRegistration(TELEGRAM_ID)).isTrue();
    }

    @org.junit.jupiter.api.Test
    void isInRegistration_falseOutsideRegistration() {
        when(stateManager.getState(TELEGRAM_ID)).thenReturn(StateConstants.IDLE);
        assertThat(handler.isInRegistration(TELEGRAM_ID)).isFalse();
    }

    @org.junit.jupiter.api.Test
    void normalizeCyrillicToLatin_convertsKnownLetters() {
        // Маппинг всегда даёт ЗАГЛАВНУЮ латиницу, даже из строчной кириллицы —
        // 'case A, a -> A'. handleSubgroupCallback сам вызывает .toUpperCase() поверх,
        // так что для итогового значения регистр входа не важен.
        assertThat(RegistrationHandler.normalizeCyrillicToLatin("АВЕКМНОРСТУХ"))
            .isEqualTo("ABEKMHOPCTYX");
        assertThat(RegistrationHandler.normalizeCyrillicToLatin("авекмнорстух"))
            .isEqualTo("ABEKMHOPCTYX");
        assertThat(RegistrationHandler.normalizeCyrillicToLatin(null)).isEqualTo("");
    }
}

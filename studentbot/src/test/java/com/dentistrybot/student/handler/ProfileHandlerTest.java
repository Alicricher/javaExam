package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.Lang;
import com.dentistrybot.student.localization.UzMessages;
import org.junit.jupiter.api.BeforeEach;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProfileHandlerTest extends HandlerTestSupport {

    private TelegramClient bot;
    private StateManager stateManager;
    private StudentRepository studentRepository;
    private ProfileHandler handler;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramClient.class);
        stateManager = mock(StateManager.class);
        studentRepository = mock(StudentRepository.class);
        handler = new ProfileHandler(bot, stateManager, studentRepository);
    }

    private Student student() {
        Student s = new Student();
        s.setId(42);
        s.setTelegramId(TELEGRAM_ID);
        s.setFullName("Ali Valiyev");
        s.setCourse(3);
        s.setGroupName("301");
        s.setSubgroup("A");
        s.setFaculty("Stomatologiya");
        return s;
    }

    @org.junit.jupiter.api.Test
    void showProfile_studentFound_sendsProfileText() throws Exception {
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());

        handler.showProfile(CHAT_ID, TELEGRAM_ID);

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent.get(0).getText())
            .contains("Ali Valiyev").contains("301").contains("Stomatologiya");
    }

    @org.junit.jupiter.api.Test
    void showProfile_studentNotFound_sendsNotFoundMessage() throws Exception {
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(null);

        handler.showProfile(CHAT_ID, TELEGRAM_ID);

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent.get(0).getText()).isEqualTo(UzMessages.MSG_NOT_FOUND);
    }

    @org.junit.jupiter.api.Test
    void editProfileCallback_course_setsStateAndShowsCourseSelection() throws Exception {
        handler.handleEditProfileCallback(callbackWithData(StudentKeyboards.CB_EDIT_PROFILE + "course"));

        verify(stateManager).setState(TELEGRAM_ID, StateConstants.EDIT_COURSE);
        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).isEqualTo(UzMessages.MSG_ENTER_COURSE);
    }

    @org.junit.jupiter.api.Test
    void editProfileCallback_group_usesStudentsCurrentCourseForKeyboard() throws Exception {
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(student());

        handler.handleEditProfileCallback(callbackWithData(StudentKeyboards.CB_EDIT_PROFILE + "group"));

        verify(stateManager).setState(TELEGRAM_ID, StateConstants.EDIT_GROUP);
    }

    @org.junit.jupiter.api.Test
    void editCourseCallback_updatesStudentAndAdvancesToGroup() throws Exception {
        Student s = student();
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(s);

        handler.handleEditCourseCallback(callbackWithData(""), 4);

        verify(studentRepository).updateStudent(s);
        assertThat(s.getCourse()).isEqualTo(4);
        verify(stateManager).setState(TELEGRAM_ID, StateConstants.EDIT_GROUP);
        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains("Kurs o'zgartirildi");
    }

    @org.junit.jupiter.api.Test
    void editCourseCallback_studentNotFound_doesNothing() throws Exception {
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(null);

        handler.handleEditCourseCallback(callbackWithData(""), 4);

        verify(bot, org.mockito.Mockito.never())
            .execute(org.mockito.ArgumentMatchers.any(org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod.class));
    }

    @org.junit.jupiter.api.Test
    void editGroupCallback_updatesStudentClearsStateAndShowsProfile() throws Exception {
        Student s = student();
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(s);

        handler.handleEditGroupCallback(callbackWithData(""), "302");

        verify(studentRepository).updateStudent(s);
        assertThat(s.getGroupName()).isEqualTo("302");
        verify(stateManager).clearState(TELEGRAM_ID);
        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains(UzMessages.MSG_PROFILE_UPDATED);
    }

    @org.junit.jupiter.api.Test
    void editSubgroupCallback_normalizesCyrillicAndUpdates() throws Exception {
        Student s = student();
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(s);

        handler.handleEditSubgroupCallback(callbackWithData(""), "с"); // cyrillic 'с'

        assertThat(s.getSubgroup()).isEqualTo("C");
        verify(stateManager).clearState(TELEGRAM_ID);
    }

    @org.junit.jupiter.api.Test
    void editFacultyCallback_updatesStudentClearsStateAndShowsProfile() throws Exception {
        Student s = student();
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(s);

        handler.handleEditFacultyCallback(callbackWithData(""), "Davolash");

        assertThat(s.getFaculty()).isEqualTo("Davolash");
        verify(stateManager).clearState(TELEGRAM_ID);
        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains("Davolash");
    }

    @org.junit.jupiter.api.Test
    void handleProfileEditText_asksToUseButtons() throws Exception {
        handler.handleProfileEditText(textMessage("qo'lda yozilgan matn"));

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent.get(0).getText()).contains("tugmalardan tanlang");
    }

    @org.junit.jupiter.api.Test
    void editProfileCallback_lang_showsLanguageSelection() throws Exception {
        handler.handleEditProfileCallback(callbackWithData(StudentKeyboards.CB_EDIT_PROFILE + "lang"));

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).isEqualTo(Lang.msgSelectLanguage());
    }

    @org.junit.jupiter.api.Test
    void handleLangCallback_updatesLanguageAndShowsProfileInNewLang() throws Exception {
        Student s = student();
        s.setLanguage("uz");
        when(studentRepository.getStudentByTelegramId(TELEGRAM_ID)).thenReturn(s);

        handler.handleLangCallback(callbackWithData(""), "ru");

        verify(studentRepository).updateStudentLanguage(42, "ru");
        verify(stateManager).clearState(TELEGRAM_ID);
        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains(Lang.msgLangChanged("ru"));
    }
}

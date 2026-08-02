package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.TheoryMaterial;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.service.FileService;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
import org.junit.jupiter.api.BeforeEach;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TheoryHandlerTest extends HandlerTestSupport {

    private TelegramClient bot;
    private StateManager stateManager;
    private LessonRepository lessonRepository;
    private FileService fileService;
    private TheoryHandler handler;

    @BeforeEach
    void setUp() {
        bot = mock(TelegramClient.class);
        stateManager = mock(StateManager.class);
        lessonRepository = mock(LessonRepository.class);
        fileService = mock(FileService.class);
        handler = new TheoryHandler(bot, stateManager, lessonRepository, fileService);
    }

    private TheoryMaterial material(int id, String titleUz, String type, String filePath, String description) {
        TheoryMaterial m = new TheoryMaterial();
        m.setId(id);
        m.setTitleUz(titleUz);
        m.setMaterialType(type);
        m.setFilePath(filePath);
        m.setDescription(description);
        return m;
    }

    @org.junit.jupiter.api.Test
    void handleTheoryCallback_noMaterials_showsNoTheoryMessage() throws Exception {
        when(lessonRepository.getTheoryMaterialsByLessonId(5)).thenReturn(List.of());

        handler.handleTheoryCallback(callbackWithData(StudentKeyboards.CB_THEORY + "5"));

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits).hasSize(1);
        assertThat(edits.get(0).getText()).isEqualTo(UzMessages.MSG_NO_THEORY);
    }

    @org.junit.jupiter.api.Test
    void handleTheoryCallback_withFileMaterial_deletesOldMessageAndSendsDocument() throws Exception {
        TheoryMaterial m = material(1, "Kitob", "book", "f1/kitob.pdf", null);
        when(lessonRepository.getTheoryMaterialsByLessonId(5)).thenReturn(List.of(m));
        when(fileService.fileExists("f1/kitob.pdf")).thenReturn(true);

        handler.handleTheoryCallback(callbackWithData(StudentKeyboards.CB_THEORY + "5"));

        List<DeleteMessage> deletes = executedOf(bot, DeleteMessage.class);
        assertThat(deletes).hasSize(1);
        verify(fileService).sendDocument(bot, CHAT_ID, "f1/kitob.pdf", "Kitob");

        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(s -> s.getText().contains("Nazariy materiallar"));
        assertThat(sent).anyMatch(s -> s.getText().contains("Darsga qaytish"));
    }

    @org.junit.jupiter.api.Test
    void handleTheoryCallback_descriptionOnlyMaterial_sendsMarkdownMessage() throws Exception {
        TheoryMaterial m = material(1, "Izoh", "material", null, "Bu material haqida izoh");
        when(lessonRepository.getTheoryMaterialsByLessonId(5)).thenReturn(List.of(m));

        handler.handleTheoryCallback(callbackWithData(StudentKeyboards.CB_THEORY + "5"));

        verify(fileService, never()).sendDocument(any(), anyLong(), anyString(), anyString());
        List<SendMessage> sent = executedOf(bot, SendMessage.class);
        assertThat(sent).anyMatch(s -> s.getText().contains("Izoh") && s.getText().contains("Bu material haqida izoh"));
    }

    @org.junit.jupiter.api.Test
    void handleMaterialTypeCallback_noneOfRequestedType_showsEmptyMessage() throws Exception {
        when(lessonRepository.getTheoryMaterialsByLessonId(5))
            .thenReturn(List.of(material(1, "Kitob", "book", null, "d")));

        handler.handleMaterialTypeCallback(callbackWithData(StudentKeyboards.CB_MAT_TYPE + "5:manual"));

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).contains(UzMessages.MSG_MANUALS).contains("mavjud emas");
    }

    @org.junit.jupiter.api.Test
    void handleMaterialTypeCallback_hasMaterials_showsList() throws Exception {
        when(lessonRepository.getTheoryMaterialsByLessonId(5))
            .thenReturn(List.of(material(1, "Kitob 1", "book", null, "d")));

        handler.handleMaterialTypeCallback(callbackWithData(StudentKeyboards.CB_MAT_TYPE + "5:book"));

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).isEqualTo(UzMessages.MSG_BOOKS + ":");
    }

    @org.junit.jupiter.api.Test
    void handleMaterialCallback_notFound_showsNotFoundMessage() throws Exception {
        when(lessonRepository.getTheoryMaterialById(99)).thenReturn(null);

        handler.handleMaterialCallback(callbackWithData(StudentKeyboards.CB_MATERIAL + "99"));

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).isEqualTo("Material topilmadi.");
    }

    @org.junit.jupiter.api.Test
    void handleMaterialCallback_withFile_sendsDocument() throws Exception {
        TheoryMaterial m = material(1, "Kitob", "book", "f1/kitob.pdf", null);
        when(lessonRepository.getTheoryMaterialById(1)).thenReturn(m);
        when(fileService.fileExists("f1/kitob.pdf")).thenReturn(true);

        handler.handleMaterialCallback(callbackWithData(StudentKeyboards.CB_MATERIAL + "1"));

        verify(fileService).sendDocument(bot, CHAT_ID, "f1/kitob.pdf", "Kitob");
    }

    @org.junit.jupiter.api.Test
    void handleMaterialCallback_neitherFileNorDescription_showsUnavailableMessage() throws Exception {
        TheoryMaterial m = material(1, "Bo'sh", "material", null, null);
        when(lessonRepository.getTheoryMaterialById(1)).thenReturn(m);

        handler.handleMaterialCallback(callbackWithData(StudentKeyboards.CB_MATERIAL + "1"));

        List<EditMessageText> edits = executedOf(bot, EditMessageText.class);
        assertThat(edits.get(0).getText()).isEqualTo("Material mavjud emas.");
    }
}

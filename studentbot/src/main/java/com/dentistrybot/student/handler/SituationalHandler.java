package com.dentistrybot.student.handler;

import com.dentistrybot.shared.model.SituationalAnswer;
import com.dentistrybot.shared.model.SituationalTask;
import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.repository.LessonRepository;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.FileService;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.service.TestService;
import com.dentistrybot.shared.state.SituationalStateData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SituationalHandler {

    private static final Logger log = LoggerFactory.getLogger(SituationalHandler.class);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final TestService testService;
    private final LessonRepository lessonRepository;
    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final FileService fileService;

    public SituationalHandler(TelegramClient bot, StateManager stateManager, TestService testService,
                               LessonRepository lessonRepository, ResultRepository resultRepository,
                               StudentRepository studentRepository, FileService fileService) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.testService = testService;
        this.lessonRepository = lessonRepository;
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.fileService = fileService;
    }

    private String langFor(long telegramId) {
        Student s = studentRepository.getStudentByTelegramId(telegramId);
        return s != null ? s.getLanguage() : "uz";
    }

    public void handleSituationalCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            int lessonId = Integer.parseInt(callback.getData().substring(StudentKeyboards.CB_SIT.length()));
            List<SituationalTask> tasks = lessonRepository.getSituationalTasksByLessonId(lessonId);
            answerCallback(callback.getId());

            if (tasks.isEmpty()) {
                bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(messageId).text(Lang.msgNoTaskAvailable(lang))
                    .replyMarkup(backToLesson(lessonId, lang)).build());
                return;
            }

            Student student = studentRepository.getStudentByTelegramId(telegramId);
            if (student == null) return;

            List<Integer> taskIds = tasks.stream().map(SituationalTask::getId).toList();
            Set<Integer> answeredIds = resultRepository.getSubmittedTaskIds(student.getId(), taskIds).keySet();

            String text = Lang.msgSituationalTaskListHeader(lang, tasks.size(), answeredIds.size());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(StudentKeyboards.situationalTaskList(tasks, answeredIds, lessonId, lang)).build());
        } catch (Exception e) {
            log.error("handleSituationalCallback error: {}", e.getMessage());
        }
    }

    public void handleTaskSelectCallback(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            int taskId = Integer.parseInt(callback.getData().substring(StudentKeyboards.CB_SIT_TASK.length()));
            SituationalTask task = lessonRepository.getSituationalTaskById(taskId);
            Student student = studentRepository.getStudentByTelegramId(telegramId);
            if (task == null || student == null) return;
            answerCallback(callback.getId());

            boolean submitted = resultRepository.hasSubmittedSituationalTask(student.getId(), taskId);
            if (submitted) {
                try {
                    resultRepository.useSituationalRetakeAtomically(student.getId(), taskId);
                } catch (Exception ex) {
                    bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(messageId)
                        .text(Lang.msgAlreadySubmittedShort(lang, task.getOrderNum()))
                        .replyMarkup(backToSitList(task.getLessonId(), lang)).build());
                    return;
                }
            }

            String text = task.getOrderNum() + "-masala\n\n" +
                Lang.msgSituationalStart(lang, task.getTimeLimitMinutes());
            bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(text)
                .replyMarkup(StudentKeyboards.situationalConfirm(task.getId(), lang)).build());
        } catch (Exception e) {
            log.error("handleTaskSelectCallback error: {}", e.getMessage());
        }
    }

    public void handleSituationalConfirm(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            if (isInSituational(telegramId)) { answerCallback(callback.getId()); return; }

            int taskId = Integer.parseInt(callback.getData().substring((StudentKeyboards.CB_CONFIRM + "sit:").length()));
            SituationalTask task = lessonRepository.getSituationalTaskById(taskId);
            if (task == null) return;

            List<SituationalTask> allTasks = lessonRepository.getSituationalTasksByLessonId(task.getLessonId());

            SituationalStateData sd = new SituationalStateData();
            sd.setTaskId(taskId);
            sd.setLessonId(task.getLessonId());
            sd.setTaskNumber(task.getOrderNum());
            sd.setTotalTasks(allTasks.size());
            sd.setStartedAt(Instant.now());
            sd.setTimeLimitMinutes(task.getTimeLimitMinutes());

            stateManager.setStateWithData(telegramId, StateConstants.IN_SITUATIONAL, sd);
            answerCallback(callback.getId());
            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

            long sec = testService.getRemainingTime(sd.getStartedAt(), sd.getTimeLimitMinutes()).getSeconds();
            String timeStr = String.format("%d:%02d", sec / 60, sec % 60);
            String text = Lang.msgSituationalTask(lang, task.textFor(lang)) + "\n\n" + Lang.msgRemainingTimeLabel(lang) + timeStr;

            if (task.getPhotoFilePath() != null) {
                try {
                    java.io.File photoFile = new java.io.File(fileService.getFilePath(task.getPhotoFilePath()));
                    bot.execute(SendPhoto.builder()
                        .chatId(chatId)
                        .photo(new InputFile(photoFile))
                        .caption(text)
                        .replyMarkup(StudentKeyboards.cancelOnly(lang))
                        .build());
                } catch (Exception photoEx) {
                    log.warn("Could not send photo for task {}: {}", task.getId(), photoEx.getMessage());
                    bot.execute(SendMessage.builder().chatId(chatId).text(text).replyMarkup(StudentKeyboards.cancelOnly(lang)).build());
                }
            } else {
                bot.execute(SendMessage.builder()
                    .chatId(chatId).text(text).replyMarkup(StudentKeyboards.cancelOnly(lang)).build());
            }
        } catch (Exception e) {
            log.error("handleSituationalConfirm error: {}", e.getMessage());
        }
    }

    public void handleSituationalAnswer(Message message) {
        long telegramId = message.getFrom().getId();
        long chatId = message.getChatId();
        String lang = langFor(telegramId);
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            SituationalStateData sd = stateManager.getStateData(us, SituationalStateData.class);
            if (sd == null) return;

            if (testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())) {
                handleTimeoutWithSave(chatId, telegramId, sd,
                    message.getText() != null ? message.getText() : "");
                return;
            }

            if (testService.shouldShowWarning(sd.getStartedAt(), sd.getTimeLimitMinutes())) {
                sendText(chatId, Lang.msgSituationalTimeWarning(lang));
            }

            // Photo rejected for student bot
            if (message.getPhoto() != null && !message.getPhoto().isEmpty()) {
                sendText(chatId, Lang.msgOnlyTextAnswer(lang));
                return;
            }

            if (message.getText() == null || message.getText().isBlank()) {
                sendText(chatId, Lang.msgPleaseSendTextAnswer(lang));
                return;
            }

            sd.setAnswerText(message.getText());
            stateManager.setStateWithData(telegramId, StateConstants.SITUATIONAL_CONFIRM_ANSWER, sd);
            showAnswerConfirmation(chatId, sd, lang);
        } catch (Exception e) {
            log.error("handleSituationalAnswer error: {}", e.getMessage());
        }
    }

    public void handleConfirmAnswer(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            SituationalStateData sd = stateManager.getStateData(us, SituationalStateData.class);
            if (sd == null) return;
            answerCallback(callback.getId());

            if (testService.isTimeUp(sd.getStartedAt(), sd.getTimeLimitMinutes())) {
                handleTimeoutWithSave(chatId, telegramId, sd, "");
                return;
            }

            Student student = studentRepository.getStudentByTelegramId(telegramId);
            if (student == null) return;

            SituationalAnswer answer = new SituationalAnswer();
            answer.setStudentId(student.getId());
            answer.setTaskId(sd.getTaskId());
            answer.setAnswerText(sd.getAnswerText());
            answer.setPhotoFileId(sd.getPhotoFileId());
            answer.setSubmittedAt(LocalDateTime.now());

            try {
                resultRepository.createSituationalAnswer(answer);
            } catch (Exception ex) {
                stateManager.clearState(telegramId);
                bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                String errMsg = ex.getMessage() != null && (ex.getMessage().contains("unique") || ex.getMessage().contains("duplicate"))
                    ? Lang.msgAlreadySubmitted(lang)
                    : Lang.msgAnswerNotSavedError(lang);
                bot.execute(SendMessage.builder().chatId(chatId).text(errMsg)
                    .replyMarkup(StudentKeyboards.mainMenu(lang)).build());
                return;
            }

            stateManager.clearState(telegramId);
            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

            String successText = Lang.msgAnswerAccepted(lang, sd.getTaskNumber());
            List<SituationalTask> allTasks = lessonRepository.getSituationalTasksByLessonId(sd.getLessonId());
            SituationalTask nextTask = null;
            for (SituationalTask t : allTasks) {
                if (!resultRepository.hasSubmittedSituationalTask(student.getId(), t.getId())) {
                    nextTask = t;
                    break;
                }
            }

            InlineKeyboardMarkup kb;
            if (nextTask != null) {
                kb = InlineKeyboardMarkup.builder()
                    .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(Lang.msgGoToNextTask(lang, nextTask.getOrderNum()))
                        .callbackData(StudentKeyboards.CB_SIT_TASK + nextTask.getId()).build()))
                    .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                        .text(Lang.btnBack(lang))
                        .callbackData(StudentKeyboards.CB_BACK + "lesson:" + sd.getLessonId()).build()))
                    .build();
            } else {
                successText = Lang.msgAllTasksCompleted(lang);
                kb = backToLesson(sd.getLessonId(), lang);
            }

            bot.execute(SendMessage.builder().chatId(chatId).text(successText).replyMarkup(kb).build());
        } catch (Exception e) {
            log.error("handleConfirmAnswer error: {}", e.getMessage());
        }
    }

    public void handleEditAnswer(CallbackQuery callback) {
        long telegramId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();
        String lang = langFor(telegramId);
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            SituationalStateData sd = stateManager.getStateData(us, SituationalStateData.class);
            if (sd == null) return;
            answerCallback(callback.getId());

            sd.setAnswerText("");
            sd.setPhotoFileId("");
            stateManager.setStateWithData(telegramId, StateConstants.IN_SITUATIONAL, sd);
            bot.execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

            SituationalTask task = lessonRepository.getSituationalTaskById(sd.getTaskId());
            long sec = testService.getRemainingTime(sd.getStartedAt(), sd.getTimeLimitMinutes()).getSeconds();
            String text = Lang.msgSituationalTask(lang,
                task != null ? task.textFor(lang) : "") + "\n\n" + Lang.msgRemainingTimeLabel(lang)
                + String.format("%d:%02d", sec / 60, sec % 60);
            bot.execute(SendMessage.builder().chatId(chatId).text(text)
                .replyMarkup(StudentKeyboards.cancelOnly(lang)).build());
        } catch (Exception e) {
            log.error("handleEditAnswer error: {}", e.getMessage());
        }
    }

    public boolean isInSituational(long telegramId) {
        String state = stateManager.getState(telegramId);
        return StateConstants.IN_SITUATIONAL.equals(state) || StateConstants.SITUATIONAL_CONFIRM_ANSWER.equals(state);
    }

    public void forceCompleteSituational(long chatId, long telegramId) {
        String lang = langFor(telegramId);
        try {
            UserState us = stateManager.getStateWithData(telegramId);
            if (us != null) {
                SituationalStateData sd = stateManager.getStateData(us, SituationalStateData.class);
                if (sd != null && sd.getAnswerText() != null && !sd.getAnswerText().isEmpty()) {
                    Student student = studentRepository.getStudentByTelegramId(telegramId);
                    if (student != null) {
                        SituationalAnswer answer = new SituationalAnswer();
                        answer.setStudentId(student.getId());
                        answer.setTaskId(sd.getTaskId());
                        answer.setAnswerText(sd.getAnswerText());
                        answer.setPhotoFileId(sd.getPhotoFileId());
                        answer.setSubmittedAt(LocalDateTime.now());
                        try { resultRepository.createSituationalAnswer(answer); }
                        catch (Exception ex) { log.error("forceCompleteSituational save error: {}", ex.getMessage()); }
                    }
                }
            }
            stateManager.clearState(telegramId);
            bot.execute(SendMessage.builder().chatId(chatId)
                .text(Lang.msgSituationalTimeUp(lang))
                .replyMarkup(StudentKeyboards.mainMenu(lang)).build());
        } catch (Exception e) {
            log.error("forceCompleteSituational error: {}", e.getMessage());
        }
    }

    private void handleTimeoutWithSave(long chatId, long telegramId, SituationalStateData sd, String extraText) {
        String lang = langFor(telegramId);
        try {
            String answerText = sd.getAnswerText();
            if (!extraText.isEmpty()) answerText = extraText;
            if (answerText != null && !answerText.isEmpty()) {
                Student student = studentRepository.getStudentByTelegramId(telegramId);
                if (student != null) {
                    SituationalAnswer answer = new SituationalAnswer();
                    answer.setStudentId(student.getId());
                    answer.setTaskId(sd.getTaskId());
                    answer.setAnswerText(answerText);
                    answer.setSubmittedAt(LocalDateTime.now());
                    try { resultRepository.createSituationalAnswer(answer); }
                    catch (Exception ex) { log.error("handleTimeoutWithSave error: {}", ex.getMessage()); }
                    sendText(chatId, Lang.msgSituationalTimeUp(lang) + "\n\n" + Lang.msgAnswerSaved(lang));
                    stateManager.clearState(telegramId);
                    return;
                }
            }
            sendText(chatId, Lang.msgSituationalTimeUp(lang));
            stateManager.clearState(telegramId);
        } catch (Exception e) {
            log.error("handleTimeoutWithSave error: {}", e.getMessage());
        }
    }

    private void showAnswerConfirmation(long chatId, SituationalStateData sd, String lang) {
        try {
            StringBuilder sb = new StringBuilder(Lang.msgConfirmYourAnswer(lang));
            if (sd.getPhotoFileId() != null && !sd.getPhotoFileId().isEmpty())
                sb.append(Lang.msgPhotoUploadedLabel(lang));
            if (sd.getAnswerText() != null && !sd.getAnswerText().isEmpty()) {
                String preview = sd.getAnswerText().length() > 300
                    ? sd.getAnswerText().substring(0, 300) + "..." : sd.getAnswerText();
                sb.append(Lang.msgAnswerLabel(lang)).append(preview).append("\n");
            }
            sb.append(Lang.msgCannotEditAfterConfirm(lang));
            bot.execute(SendMessage.builder().chatId(chatId).text(sb.toString())
                .replyMarkup(StudentKeyboards.situationalAnswerConfirm(lang)).build());
        } catch (Exception e) {
            log.error("showAnswerConfirmation error: {}", e.getMessage());
        }
    }

    private InlineKeyboardMarkup backToLesson(int lessonId, String lang) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text(Lang.btnBack(lang))
                .callbackData(StudentKeyboards.CB_BACK + "lesson:" + lessonId).build()))
            .build();
    }

    private InlineKeyboardMarkup backToSitList(int lessonId, String lang) {
        return InlineKeyboardMarkup.builder()
            .keyboardRow(new InlineKeyboardRow(InlineKeyboardButton.builder()
                .text(Lang.btnBack(lang))
                .callbackData(StudentKeyboards.CB_SIT + lessonId).build()))
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

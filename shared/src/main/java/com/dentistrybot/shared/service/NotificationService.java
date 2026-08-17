package com.dentistrybot.shared.service;

import com.dentistrybot.shared.model.SituationalAnswer;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final TelegramClient studentBotClient;
    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;

    public NotificationService(TelegramClient studentBotClient,
                               ResultRepository resultRepository,
                               StudentRepository studentRepository) {
        this.studentBotClient = studentBotClient;
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
    }

    public void notifySituationalGraded(int answerId) {
        if (studentBotClient == null) {
            log.debug("Skipping situational grading notification: student bot token is not configured");
            return;
        }

        try {
            SituationalAnswer answer = resultRepository.getSituationalAnswerById(answerId);
            if (answer == null) return;

            var student = studentRepository.getStudentById(answer.getStudentId());
            if (student == null) return;

            String grade = answer.getGrade() != null ? answer.getGrade() + "/100" : "-";
            String feedback = (answer.getFeedback() != null && !answer.getFeedback().isEmpty())
                ? answer.getFeedback() : "Izoh yo'q";
            String citationsLine = (answer.getCitations() != null && !answer.getCitations().isEmpty())
                ? "\nManbalar: " + answer.getCitations() : "";

            String text = String.format(
                "Vaziyatli topshirig'ingiz baholandi!\n\nBaho: %s\nIzoh: %s%s",
                grade, feedback, citationsLine);

            studentBotClient.execute(SendMessage.builder()
                .chatId(student.getTelegramId())
                .text(text)
                .build());
        } catch (Exception e) {
            log.error("Failed to notify student about graded situational answer {}: {}", answerId, e.getMessage());
        }
    }
}

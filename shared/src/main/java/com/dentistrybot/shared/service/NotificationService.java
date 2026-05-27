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
        try {
            SituationalAnswer answer = resultRepository.getSituationalAnswerById(answerId);
            if (answer == null) return;

            var student = studentRepository.getStudentById(answer.getStudentId());
            if (student == null) return;

            String grade = answer.getGrade() != null ? answer.getGrade() + "/100" : "—";
            String feedback = (answer.getFeedback() != null && !answer.getFeedback().isEmpty())
                ? answer.getFeedback() : "Izoh yo'q";

            String text = String.format(
                "✅ Vaziyatli topshirig'ingiz baholandi!\n\nBaho: %s\nIzoh: %s",
                grade, feedback);

            SendMessage msg = SendMessage.builder()
                .chatId(student.getTelegramId())
                .text(text)
                .build();

            studentBotClient.execute(msg);
        } catch (Exception e) {
            log.error("Failed to notify student about graded situational answer {}: {}", answerId, e.getMessage());
        }
    }
}

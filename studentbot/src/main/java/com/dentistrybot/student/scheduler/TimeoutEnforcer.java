package com.dentistrybot.student.scheduler;

import com.dentistrybot.shared.model.SituationalAnswer;
import com.dentistrybot.shared.model.Student;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StateRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.service.StateManager;
import com.dentistrybot.shared.state.SituationalStateData;
import com.dentistrybot.shared.state.StateConstants;
import com.dentistrybot.shared.state.UserState;
import com.dentistrybot.student.keyboard.StudentKeyboards;
import com.dentistrybot.student.localization.UzMessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.LocalDateTime;
import java.util.List;

public class TimeoutEnforcer {

    private static final Logger log = LoggerFactory.getLogger(TimeoutEnforcer.class);

    private final TelegramClient bot;
    private final StateManager stateManager;
    private final StateRepository stateRepository;
    private final ResultRepository resultRepository;
    private final StudentRepository studentRepository;

    public TimeoutEnforcer(TelegramClient bot, StateManager stateManager,
                           StateRepository stateRepository, ResultRepository resultRepository,
                           StudentRepository studentRepository) {
        this.bot = bot;
        this.stateManager = stateManager;
        this.stateRepository = stateRepository;
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
    }

    @Scheduled(fixedDelay = 60_000)
    public void enforceTimeouts() {
        try {
            // 1. Force-complete orphaned in_progress test results in DB
            int completed = resultRepository.forceCompleteExpiredTestResults();
            if (completed > 0) log.info("Force-completed {} expired test results", completed);

            // 2. Clear states for expired tests and notify students
            List<UserState> expiredTests = stateRepository.getExpiredTestStates();
            for (UserState s : expiredTests) {
                try {
                    stateRepository.clearUserState(s.getTelegramId(), "student");
                    bot.execute(SendMessage.builder()
                        .chatId(s.getTelegramId())
                        .text("⏰ Test vaqti tugadi! Test avtomatik yakunlandi.")
                        .replyMarkup(StudentKeyboards.mainMenu())
                        .build());
                } catch (Exception e) {
                    log.error("Error notifying student {} about test timeout: {}", s.getTelegramId(), e.getMessage());
                }
            }

            // 3. Clear states for expired situational tasks and notify students
            List<UserState> expiredSit = stateRepository.getExpiredSituationalStates();
            for (UserState s : expiredSit) {
                String notifyText = "⏰ Vaziyatli masala vaqti tugadi!";
                try {
                    if (StateConstants.SITUATIONAL_CONFIRM_ANSWER.equals(s.getState())) {
                        SituationalStateData sd = stateManager.getStateData(s, SituationalStateData.class);
                        if (sd != null && sd.getAnswerText() != null && !sd.getAnswerText().isEmpty()) {
                            Student student = studentRepository.getStudentByTelegramId(s.getTelegramId());
                            if (student != null) {
                                SituationalAnswer answer = new SituationalAnswer();
                                answer.setStudentId(student.getId());
                                answer.setTaskId(sd.getTaskId());
                                answer.setAnswerText(sd.getAnswerText());
                                answer.setPhotoFileId(sd.getPhotoFileId());
                                answer.setSubmittedAt(LocalDateTime.now());
                                try {
                                    resultRepository.createSituationalAnswer(answer);
                                    notifyText = "⏰ Vaziyatli masala vaqti tugadi! Javobingiz saqlandi.";
                                } catch (Exception ex) {
                                    log.error("Failed to save situational answer for user {}: {}", s.getTelegramId(), ex.getMessage());
                                }
                            }
                        }
                    }
                    stateRepository.clearUserState(s.getTelegramId(), "student");
                    bot.execute(SendMessage.builder()
                        .chatId(s.getTelegramId())
                        .text(notifyText)
                        .replyMarkup(StudentKeyboards.mainMenu())
                        .build());
                } catch (Exception e) {
                    log.error("Error processing situational timeout for {}: {}", s.getTelegramId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("TimeoutEnforcer error: {}", e.getMessage());
        }
    }
}

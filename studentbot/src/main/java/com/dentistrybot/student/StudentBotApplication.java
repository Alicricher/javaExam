package com.dentistrybot.student;

import com.dentistrybot.shared.config.AppProperties;
import com.dentistrybot.shared.repository.*;
import com.dentistrybot.shared.service.*;
import com.dentistrybot.student.bot.StudentBot;
import com.dentistrybot.student.bot.StudentUpdateDispatcher;
import com.dentistrybot.student.handler.*;
import com.dentistrybot.student.scheduler.TimeoutEnforcer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@SpringBootApplication(scanBasePackages = {"com.dentistrybot.student", "com.dentistrybot.shared"})
@EnableScheduling
public class StudentBotApplication {

    @Autowired @org.springframework.context.annotation.Lazy private StudentUpdateDispatcher studentUpdateDispatcher;
    @Autowired private AppProperties appProperties;

    public static void main(String[] args) {
        SpringApplication.run(StudentBotApplication.class, args);
    }

    @Bean
    public TelegramClient telegramClient(AppProperties props) {
        return new OkHttpTelegramClient(props.getStudentBotToken());
    }

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(ObjectMapper objectMapper) {
        return new TelegramBotsLongPollingApplication(() -> objectMapper);
    }

    @Bean
    public ApplicationRunner botRegistration(TelegramBotsLongPollingApplication app,
                                              AppProperties props, StudentBot studentBot) {
        return args -> app.registerBot(props.getStudentBotToken(), studentBot);
    }

    @Bean
    public StateManager stateManager(StateRepository stateRepository, ObjectMapper objectMapper) {
        return new StateManager(stateRepository, objectMapper, "student");
    }

    @Bean
    public TestService testService(TestRepository testRepository, ResultRepository resultRepository) {
        return new TestService(testRepository, resultRepository);
    }

    @Bean
    public FileService fileService(AppProperties props) {
        return new FileService(props.getMaterialsPath());
    }

    @Bean
    public RegistrationHandler registrationHandler(TelegramClient bot, StateManager sm, StudentRepository sr) {
        return new RegistrationHandler(bot, sm, sr);
    }

    @Bean
    public ProfileHandler profileHandler(TelegramClient bot, StateManager sm, StudentRepository sr) {
        return new ProfileHandler(bot, sm, sr);
    }

    @Bean
    public LessonHandler lessonHandler(TelegramClient bot, StateManager sm, LessonRepository lr, StudentRepository sr) {
        return new LessonHandler(bot, sm, lr, sr);
    }

    @Bean
    public TestHandler testHandler(TelegramClient bot, StateManager sm, TestService ts,
                                   TestRepository tr, ResultRepository rr, StudentRepository sr,
                                   FileService fs) {
        return new TestHandler(bot, sm, ts, tr, rr, sr, fs);
    }

    @Bean
    public TheoryHandler theoryHandler(TelegramClient bot, StateManager sm, LessonRepository lr, FileService fs, StudentRepository sr) {
        return new TheoryHandler(bot, sm, lr, fs, sr);
    }

    @Bean
    public SituationalHandler situationalHandler(TelegramClient bot, StateManager sm, TestService ts,
                                                  LessonRepository lr, ResultRepository rr, StudentRepository sr,
                                                  FileService fs) {
        return new SituationalHandler(bot, sm, ts, lr, rr, sr, fs);
    }

    @Bean
    public StudentUpdateDispatcher studentUpdateDispatcher(TelegramClient bot, StateManager sm,
                                                            StudentRepository sr,
                                                            RegistrationHandler rh, ProfileHandler ph,
                                                            LessonHandler lh, TestHandler th,
                                                            TheoryHandler tyh, SituationalHandler sih,
                                                            AppProperties props) {
        return new StudentUpdateDispatcher(bot, sm, sr, rh, ph, lh, th, tyh, sih,
            props.getDispatcher().getWorkers(), props.getDispatcher().getQueueSize());
    }

    @Bean
    public StudentBot studentBot(StudentUpdateDispatcher dispatcher) {
        return new StudentBot(dispatcher);
    }

    @Bean
    public TimeoutEnforcer timeoutEnforcer(TelegramClient bot, StateManager sm,
                                            StateRepository sr, ResultRepository rr, StudentRepository str) {
        return new TimeoutEnforcer(bot, sm, sr, rr, str);
    }

    @Scheduled(fixedDelay = 300_000)
    public void cleanupLocks() {
        studentUpdateDispatcher.cleanupLocks(appProperties.getDispatcher().getUserTtlMs());
    }
}

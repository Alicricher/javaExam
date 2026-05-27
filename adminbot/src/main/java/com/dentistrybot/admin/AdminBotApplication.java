package com.dentistrybot.admin;

import com.dentistrybot.admin.bot.AdminBot;
import com.dentistrybot.admin.bot.AdminUpdateDispatcher;
import com.dentistrybot.admin.handler.AuthHandler;
import com.dentistrybot.admin.handler.ManagementHandler;
import com.dentistrybot.admin.handler.ResultsHandler;
import com.dentistrybot.admin.handler.StudentsHandler;
import com.dentistrybot.shared.config.AppProperties;
import com.dentistrybot.shared.repository.*;
import com.dentistrybot.shared.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication(scanBasePackages = {"com.dentistrybot.admin", "com.dentistrybot.shared"})
@EnableScheduling
@EnableConfigurationProperties(AppProperties.class)
public class AdminBotApplication {

    private static final Logger log = LoggerFactory.getLogger(AdminBotApplication.class);

    @Autowired @org.springframework.context.annotation.Lazy private AdminUpdateDispatcher adminUpdateDispatcher;
    @Autowired private AppProperties appProperties;

    public static void main(String[] args) {
        SpringApplication.run(AdminBotApplication.class, args);
    }

    @Bean(name = "adminTelegramClient")
    public TelegramClient adminTelegramClient(AppProperties props) {
        return new OkHttpTelegramClient(props.getAdminBotToken());
    }

    @Bean(name = "studentTelegramClient")
    public TelegramClient studentTelegramClient(AppProperties props) {
        return new OkHttpTelegramClient(props.getStudentBotToken());
    }

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(ObjectMapper objectMapper) {
        return new TelegramBotsLongPollingApplication(() -> objectMapper);
    }

    @Bean
    public ApplicationRunner botRegistration(TelegramBotsLongPollingApplication app,
                                              AppProperties props, AdminBot adminBot) {
        return args -> app.registerBot(props.getAdminBotToken(), adminBot);
    }

    @Bean
    public StateManager stateManager(StateRepository stateRepository, ObjectMapper objectMapper) {
        return new StateManager(stateRepository, objectMapper, "admin");
    }

    @Bean
    public FileService fileService(AppProperties props) {
        return new FileService(props.getMaterialsPath());
    }

    @Bean
    public ImportService importService(TestRepository testRepository) {
        return new ImportService(testRepository);
    }

    @Bean
    @Nullable
    public GradingService gradingService(AppProperties props, ObjectMapper objectMapper) {
        AppProperties.Grading g = props.getGrading();
        if (g.getApiKey() == null || g.getApiKey().isEmpty()) {
            log.info("GRADING_API_KEY not set — AI grading disabled");
            return null;
        }
        String systemPrompt = "";
        try {
            systemPrompt = Files.readString(Paths.get(g.getPromptFile()));
        } catch (IOException e) {
            log.warn("Could not read grading prompt file: {}", e.getMessage());
        }
        GradingService gs = new GradingService(g.getApiUrl(), g.getApiKey(), g.getModel(), systemPrompt, objectMapper);
        gs.loadLessonPrompts(g.getLessonPromptsDir());
        log.info("AI grading service initialized (model: {})", g.getModel());
        return gs;
    }

    @Bean
    public NotificationService notificationService(
            @Qualifier("studentTelegramClient") TelegramClient studentClient,
            ResultRepository resultRepository, StudentRepository studentRepository) {
        return new NotificationService(studentClient, resultRepository, studentRepository);
    }

    @Bean
    public AuthHandler authHandler(
            @Qualifier("adminTelegramClient") TelegramClient bot,
            StateManager stateManager, AdminRepository adminRepository, AppProperties props) {
        return new AuthHandler(bot, stateManager, adminRepository, props.getAdminPassword());
    }

    @Bean
    public StudentsHandler studentsHandler(
            @Qualifier("adminTelegramClient") TelegramClient bot,
            StateManager stateManager, StudentRepository studentRepository, ResultRepository resultRepository) {
        return new StudentsHandler(bot, stateManager, studentRepository, resultRepository);
    }

    @Bean
    public ResultsHandler resultsHandler(
            @Qualifier("adminTelegramClient") TelegramClient bot,
            StateManager stateManager, ResultRepository resultRepository,
            StudentRepository studentRepository, LessonRepository lessonRepository,
            AdminRepository adminRepository, NotificationService notificationService,
            ObjectProvider<GradingService> gradingServiceProvider) {
        return new ResultsHandler(bot, stateManager, resultRepository, studentRepository,
                lessonRepository, adminRepository, notificationService, gradingServiceProvider.getIfAvailable());
    }

    @Bean
    public ManagementHandler managementHandler(
            @Qualifier("adminTelegramClient") TelegramClient bot,
            StateManager stateManager, LessonRepository lessonRepository,
            TestRepository testRepository, FileService fileService,
            ImportService importService, AppProperties props) {
        return new ManagementHandler(bot, stateManager, lessonRepository, testRepository,
                fileService, importService, props.getAdminBotToken());
    }

    @Bean
    public AdminUpdateDispatcher adminUpdateDispatcher(
            @Qualifier("adminTelegramClient") TelegramClient bot,
            StateManager stateManager, AuthHandler authHandler, StudentsHandler studentsHandler,
            ResultsHandler resultsHandler, ManagementHandler managementHandler, AppProperties props) {
        return new AdminUpdateDispatcher(bot, stateManager, authHandler, studentsHandler,
                resultsHandler, managementHandler,
                props.getDispatcher().getWorkers(), props.getDispatcher().getQueueSize());
    }

    @Bean
    public AdminBot adminBot(AdminUpdateDispatcher dispatcher) {
        return new AdminBot(dispatcher);
    }

    @Scheduled(fixedDelay = 300_000)
    public void cleanupLocks() {
        adminUpdateDispatcher.cleanupLocks(appProperties.getDispatcher().getUserTtlMs());
    }
}

package com.dentistrybot.admin;

import com.dentistrybot.shared.config.AppProperties;
import com.dentistrybot.shared.repository.ResultRepository;
import com.dentistrybot.shared.repository.StudentRepository;
import com.dentistrybot.shared.repository.TestRepository;
import com.dentistrybot.shared.service.FileService;
import com.dentistrybot.shared.service.GradingService;
import com.dentistrybot.shared.service.ImportService;
import com.dentistrybot.shared.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication(scanBasePackages = {"com.dentistrybot.admin", "com.dentistrybot.shared"})
public class AdminWebApplication {

    private static final Logger log = LoggerFactory.getLogger(AdminWebApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AdminWebApplication.class, args);
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
    public NotificationService notificationService(AppProperties props,
                                                   ResultRepository resultRepository,
                                                   StudentRepository studentRepository) {
        String token = props.getStudentBotToken();
        TelegramClient client = token == null || token.isBlank() ? null : new OkHttpTelegramClient(token);
        return new NotificationService(client, resultRepository, studentRepository);
    }

    @Bean
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
        if (g.getVectorStoreForeign().isEmpty() || g.getVectorStoreLocal().isEmpty()) {
            log.warn("grading: vector store IDs not configured — AI grading disabled");
            return null;
        }
        GradingService gs = new GradingService(g.getApiUrl(), g.getApiKey(), g.getModel(), systemPrompt,
            g.getVectorStoreForeign(), g.getVectorStoreLocal(), objectMapper);
        gs.loadLessonPrompts(g.getLessonPromptsDir());
        log.info("AI grading service initialized (model: {})", g.getModel());
        return gs;
    }
}

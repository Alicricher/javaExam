package com.dentistrybot.shared.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String studentBotToken = "";
    private String adminBotToken = "";
    private String materialsPath = "./storage/materials";
    private String adminPassword = "admin123";
    private Grading grading = new Grading();
    private Dispatcher dispatcher = new Dispatcher();

    public static class Grading {
        private String apiUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private String model = "gpt-4o";
        private String promptFile = "configs/grading_prompt.txt";
        private String lessonPromptsDir = "configs/grading";

        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getPromptFile() { return promptFile; }
        public void setPromptFile(String promptFile) { this.promptFile = promptFile; }

        public String getLessonPromptsDir() { return lessonPromptsDir; }
        public void setLessonPromptsDir(String lessonPromptsDir) { this.lessonPromptsDir = lessonPromptsDir; }
    }

    public static class Dispatcher {
        private int workers = 64;
        private int queueSize = 4096;
        private long updateTimeoutMs = 60_000L;
        private long userTtlMs = 300_000L;

        public int getWorkers() { return workers; }
        public void setWorkers(int workers) { this.workers = workers; }

        public int getQueueSize() { return queueSize; }
        public void setQueueSize(int queueSize) { this.queueSize = queueSize; }

        public long getUpdateTimeoutMs() { return updateTimeoutMs; }
        public void setUpdateTimeoutMs(long updateTimeoutMs) { this.updateTimeoutMs = updateTimeoutMs; }

        public long getUserTtlMs() { return userTtlMs; }
        public void setUserTtlMs(long userTtlMs) { this.userTtlMs = userTtlMs; }
    }

    public String getStudentBotToken() { return studentBotToken; }
    public void setStudentBotToken(String t) { this.studentBotToken = t; }

    public String getAdminBotToken() { return adminBotToken; }
    public void setAdminBotToken(String t) { this.adminBotToken = t; }

    public String getMaterialsPath() { return materialsPath; }
    public void setMaterialsPath(String materialsPath) { this.materialsPath = materialsPath; }

    public String getAdminPassword() { return adminPassword; }
    public void setAdminPassword(String adminPassword) { this.adminPassword = adminPassword; }

    public Grading getGrading() { return grading; }
    public void setGrading(Grading grading) { this.grading = grading; }

    public Dispatcher getDispatcher() { return dispatcher; }
    public void setDispatcher(Dispatcher dispatcher) { this.dispatcher = dispatcher; }
}

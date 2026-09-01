package com.dentistrybot.shared.model;

import java.time.LocalDateTime;

public class SituationalTask {
    private int id;
    private int lessonId;
    private int orderNum;
    private String taskText;
    private String taskTextRu;
    private int timeLimitMinutes;
    private boolean isActive;
    private String photoFilePath;
    private LocalDateTime createdAt;

    public SituationalTask() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }

    public int getOrderNum() { return orderNum; }
    public void setOrderNum(int orderNum) { this.orderNum = orderNum; }

    public String getTaskText() { return taskText; }
    public void setTaskText(String taskText) { this.taskText = taskText; }

    public String getTaskTextRu() { return taskTextRu; }
    public void setTaskTextRu(String taskTextRu) { this.taskTextRu = taskTextRu; }

    public String textFor(String lang) {
        return "ru".equals(lang) && taskTextRu != null ? taskTextRu : taskText;
    }

    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(int timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public String getPhotoFilePath() { return photoFilePath; }
    public void setPhotoFilePath(String photoFilePath) { this.photoFilePath = photoFilePath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

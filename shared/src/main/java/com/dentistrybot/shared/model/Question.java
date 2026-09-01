package com.dentistrybot.shared.model;

import java.time.LocalDateTime;

public class Question {
    private int id;
    private int testId;
    private String questionText;
    private String questionTextRu;
    private int points;
    private int orderNum;
    private String photoFilePath;
    private LocalDateTime createdAt;

    public Question() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public String getQuestionTextRu() { return questionTextRu; }
    public void setQuestionTextRu(String questionTextRu) { this.questionTextRu = questionTextRu; }

    public String textFor(String lang) {
        return "ru".equals(lang) && questionTextRu != null ? questionTextRu : questionText;
    }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getOrderNum() { return orderNum; }
    public void setOrderNum(int orderNum) { this.orderNum = orderNum; }

    public String getPhotoFilePath() { return photoFilePath; }
    public void setPhotoFilePath(String photoFilePath) { this.photoFilePath = photoFilePath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

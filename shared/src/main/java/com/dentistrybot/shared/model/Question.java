package com.dentistrybot.shared.model;

import java.time.LocalDateTime;

public class Question {
    private int id;
    private int testId;
    private String questionText;
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

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public int getOrderNum() { return orderNum; }
    public void setOrderNum(int orderNum) { this.orderNum = orderNum; }

    public String getPhotoFilePath() { return photoFilePath; }
    public void setPhotoFilePath(String photoFilePath) { this.photoFilePath = photoFilePath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

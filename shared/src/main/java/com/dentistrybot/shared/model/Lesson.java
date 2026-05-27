package com.dentistrybot.shared.model;

import java.time.LocalDateTime;

public class Lesson {
    private int id;
    private int unitId;
    private int lessonNumber;
    private String titleUz;
    private LocalDateTime createdAt;

    public Lesson() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUnitId() { return unitId; }
    public void setUnitId(int unitId) { this.unitId = unitId; }

    public int getLessonNumber() { return lessonNumber; }
    public void setLessonNumber(int lessonNumber) { this.lessonNumber = lessonNumber; }

    public String getTitleUz() { return titleUz; }
    public void setTitleUz(String titleUz) { this.titleUz = titleUz; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

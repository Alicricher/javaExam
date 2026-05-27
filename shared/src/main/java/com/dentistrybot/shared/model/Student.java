package com.dentistrybot.shared.model;

import java.time.LocalDateTime;

public class Student {
    private int id;
    private long telegramId;
    private String fullName;
    private int course;
    private String groupName;
    private String subgroup;
    private String faculty;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Student() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getTelegramId() { return telegramId; }
    public void setTelegramId(long telegramId) { this.telegramId = telegramId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public int getCourse() { return course; }
    public void setCourse(int course) { this.course = course; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getSubgroup() { return subgroup; }
    public void setSubgroup(String subgroup) { this.subgroup = subgroup; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

package com.dentistrybot.shared.state;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class SituationalStateData {
    @JsonProperty("task_id")
    private int taskId;
    @JsonProperty("lesson_id")
    private int lessonId;
    @JsonProperty("task_number")
    private int taskNumber;
    @JsonProperty("total_tasks")
    private int totalTasks;
    @JsonProperty("started_at")
    private Instant startedAt;
    @JsonProperty("time_limit_minutes")
    private int timeLimitMinutes;
    @JsonProperty("answer_text")
    private String answerText;
    @JsonProperty("photo_file_id")
    private String photoFileId;

    public SituationalStateData() {}

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }

    public int getTaskNumber() { return taskNumber; }
    public void setTaskNumber(int taskNumber) { this.taskNumber = taskNumber; }

    public int getTotalTasks() { return totalTasks; }
    public void setTotalTasks(int totalTasks) { this.totalTasks = totalTasks; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(int timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public String getPhotoFileId() { return photoFileId; }
    public void setPhotoFileId(String photoFileId) { this.photoFileId = photoFileId; }
}

package com.dentistrybot.shared.state;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public class TestStateData {
    @JsonProperty("test_id")
    private int testId;
    @JsonProperty("lesson_id")
    private int lessonId;
    @JsonProperty("result_id")
    private int resultId;
    @JsonProperty("current_question")
    private int currentQuestion;
    @JsonProperty("total_questions")
    private int totalQuestions;
    @JsonProperty("started_at")
    private Instant startedAt;
    @JsonProperty("time_limit_minutes")
    private int timeLimitMinutes;
    @JsonProperty("questions")
    private List<CachedQuestionData> questions;
    @JsonProperty("attempt_number")
    private int attemptNumber = 1;

    public TestStateData() {}

    public int getTestId() { return testId; }
    public void setTestId(int testId) { this.testId = testId; }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }

    public int getResultId() { return resultId; }
    public void setResultId(int resultId) { this.resultId = resultId; }

    public int getCurrentQuestion() { return currentQuestion; }
    public void setCurrentQuestion(int currentQuestion) { this.currentQuestion = currentQuestion; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public int getTimeLimitMinutes() { return timeLimitMinutes; }
    public void setTimeLimitMinutes(int timeLimitMinutes) { this.timeLimitMinutes = timeLimitMinutes; }

    public List<CachedQuestionData> getQuestions() { return questions; }
    public void setQuestions(List<CachedQuestionData> questions) { this.questions = questions; }

    public int getAttemptNumber() { return attemptNumber; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
}

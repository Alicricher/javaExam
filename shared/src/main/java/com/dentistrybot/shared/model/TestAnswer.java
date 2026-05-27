package com.dentistrybot.shared.model;

import java.time.LocalDateTime;

public class TestAnswer {
    private int id;
    private int resultId;
    private int questionId;
    private Integer selectedOptionId;
    private boolean isCorrect;
    private LocalDateTime answeredAt;

    public TestAnswer() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getResultId() { return resultId; }
    public void setResultId(int resultId) { this.resultId = resultId; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public Integer getSelectedOptionId() { return selectedOptionId; }
    public void setSelectedOptionId(Integer selectedOptionId) { this.selectedOptionId = selectedOptionId; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
}

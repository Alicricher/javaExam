package com.dentistrybot.shared.state;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CachedQuestionData {
    @JsonProperty("question_id")
    private int questionId;
    @JsonProperty("question_text")
    private String questionText;
    @JsonProperty("points")
    private int points;
    @JsonProperty("options")
    private List<CachedOptionData> options;

    public CachedQuestionData() {}

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public List<CachedOptionData> getOptions() { return options; }
    public void setOptions(List<CachedOptionData> options) { this.options = options; }
}

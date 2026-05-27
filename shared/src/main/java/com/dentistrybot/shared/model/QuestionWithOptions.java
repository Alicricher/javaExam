package com.dentistrybot.shared.model;

import java.util.List;

public class QuestionWithOptions {
    private Question question;
    private List<AnswerOption> options;

    public QuestionWithOptions() {}

    public QuestionWithOptions(Question question, List<AnswerOption> options) {
        this.question = question;
        this.options = options;
    }

    public Question getQuestion() { return question; }
    public void setQuestion(Question question) { this.question = question; }

    public List<AnswerOption> getOptions() { return options; }
    public void setOptions(List<AnswerOption> options) { this.options = options; }
}

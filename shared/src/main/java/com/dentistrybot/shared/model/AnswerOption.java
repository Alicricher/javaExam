package com.dentistrybot.shared.model;

public class AnswerOption {
    private int id;
    private int questionId;
    private String optionText;
    private String optionTextRu;
    private boolean isCorrect;
    private int orderNum;

    public AnswerOption() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public String getOptionTextRu() { return optionTextRu; }
    public void setOptionTextRu(String optionTextRu) { this.optionTextRu = optionTextRu; }

    public String textFor(String lang) {
        return "ru".equals(lang) && optionTextRu != null ? optionTextRu : optionText;
    }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }

    public int getOrderNum() { return orderNum; }
    public void setOrderNum(int orderNum) { this.orderNum = orderNum; }
}

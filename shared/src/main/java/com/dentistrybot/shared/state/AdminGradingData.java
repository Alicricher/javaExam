package com.dentistrybot.shared.state;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AdminGradingData {
    @JsonProperty("answer_id")
    private int answerId;
    @JsonProperty("admin_id")
    private int adminId;
    @JsonProperty("grade")
    private int grade;
    @JsonProperty("feedback")
    private String feedback;
    @JsonProperty("passed")
    private boolean passed;

    public AdminGradingData() {}

    public int getAnswerId() { return answerId; }
    public void setAnswerId(int answerId) { this.answerId = answerId; }

    public int getAdminId() { return adminId; }
    public void setAdminId(int adminId) { this.adminId = adminId; }

    public int getGrade() { return grade; }
    public void setGrade(int grade) { this.grade = grade; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
}

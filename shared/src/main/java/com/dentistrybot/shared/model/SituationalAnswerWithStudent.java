package com.dentistrybot.shared.model;

public class SituationalAnswerWithStudent extends SituationalAnswer {
    private String studentName;
    private String taskText;
    private String lessonTitle;
    private String unitName;

    public SituationalAnswerWithStudent() {}

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getTaskText() { return taskText; }
    public void setTaskText(String taskText) { this.taskText = taskText; }

    public String getLessonTitle() { return lessonTitle; }
    public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
}

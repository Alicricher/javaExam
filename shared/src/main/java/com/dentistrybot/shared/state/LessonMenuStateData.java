package com.dentistrybot.shared.state;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LessonMenuStateData {
    @JsonProperty("unit_id")
    private int unitId;
    @JsonProperty("lesson_id")
    private int lessonId;

    public LessonMenuStateData() {}

    public LessonMenuStateData(int unitId, int lessonId) {
        this.unitId = unitId;
        this.lessonId = lessonId;
    }

    public int getUnitId() { return unitId; }
    public void setUnitId(int unitId) { this.unitId = unitId; }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }
}

package com.dentistrybot.shared.state;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EditingStateData {
    @JsonProperty("target_id")
    private int targetId;
    @JsonProperty("target_type")
    private String targetType;
    @JsonProperty("extra_data")
    private String extraData;

    public EditingStateData() {}

    public EditingStateData(int targetId, String targetType) {
        this.targetId = targetId;
        this.targetType = targetType;
    }

    public int getTargetId() { return targetId; }
    public void setTargetId(int targetId) { this.targetId = targetId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getExtraData() { return extraData; }
    public void setExtraData(String extraData) { this.extraData = extraData; }
}

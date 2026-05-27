package com.dentistrybot.shared.state;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CachedOptionData {
    @JsonProperty("option_id")
    private int optionId;
    @JsonProperty("option_text")
    private String optionText;
    @JsonProperty("is_correct")
    private boolean isCorrect;

    public CachedOptionData() {}

    public int getOptionId() { return optionId; }
    public void setOptionId(int optionId) { this.optionId = optionId; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    public boolean isCorrect() { return isCorrect; }
    public void setCorrect(boolean correct) { isCorrect = correct; }
}

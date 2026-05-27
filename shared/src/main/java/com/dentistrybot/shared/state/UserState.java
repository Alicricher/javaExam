package com.dentistrybot.shared.state;

import java.time.LocalDateTime;

public class UserState {
    private long telegramId;
    private String botType; // "student" or "admin"
    private String state;
    private String stateData; // JSON string (maps to JSONB in DB)
    private LocalDateTime updatedAt;

    public UserState() {}

    public long getTelegramId() { return telegramId; }
    public void setTelegramId(long telegramId) { this.telegramId = telegramId; }

    public String getBotType() { return botType; }
    public void setBotType(String botType) { this.botType = botType; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getStateData() { return stateData; }
    public void setStateData(String stateData) { this.stateData = stateData; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

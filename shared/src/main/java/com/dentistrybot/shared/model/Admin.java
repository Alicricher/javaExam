package com.dentistrybot.shared.model;

import java.time.LocalDateTime;

public class Admin {
    private int id;
    private long telegramId;
    private String passwordHash;
    private boolean isAuthenticated;
    private LocalDateTime sessionExpiresAt;
    private LocalDateTime createdAt;

    public Admin() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public long getTelegramId() { return telegramId; }
    public void setTelegramId(long telegramId) { this.telegramId = telegramId; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isAuthenticated() { return isAuthenticated; }
    public void setAuthenticated(boolean authenticated) { isAuthenticated = authenticated; }

    public LocalDateTime getSessionExpiresAt() { return sessionExpiresAt; }
    public void setSessionExpiresAt(LocalDateTime sessionExpiresAt) { this.sessionExpiresAt = sessionExpiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

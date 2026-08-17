package com.dentistrybot.admin.model;

import java.time.LocalDateTime;

public class AdminUser {
    private int id;
    private String username;
    private String passwordHash;
    private String role;
    private String fullName;
    private LocalDateTime createdAt;

    public static final String ROLE_SUPER_ADMIN  = "SUPER_ADMIN";
    public static final String ROLE_ZAV_KAFEDRA  = "ZAV_KAFEDRA";
    public static final String ROLE_PROFESSOR    = "PROFESSOR";

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

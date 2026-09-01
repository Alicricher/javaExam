package com.dentistrybot.shared.model;

import java.time.LocalDateTime;

public class Unit {
    private int id;
    private String name;
    private String titleUz;
    private String titleRu;
    private LocalDateTime createdAt;

    public Unit() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitleUz() { return titleUz; }
    public void setTitleUz(String titleUz) { this.titleUz = titleUz; }

    public String getTitleRu() { return titleRu; }
    public void setTitleRu(String titleRu) { this.titleRu = titleRu; }

    public String titleFor(String lang) {
        return "ru".equals(lang) && titleRu != null ? titleRu : titleUz;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

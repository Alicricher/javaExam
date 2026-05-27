package com.dentistrybot.shared.model;

import java.time.LocalDateTime;

public class SituationalRetake {
    private int id;
    private int studentId;
    private int taskId;
    private int grantedBy;
    private LocalDateTime grantedAt;
    private boolean used;
    private LocalDateTime usedAt;

    public SituationalRetake() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public int getGrantedBy() { return grantedBy; }
    public void setGrantedBy(int grantedBy) { this.grantedBy = grantedBy; }

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime usedAt) { this.usedAt = usedAt; }
}

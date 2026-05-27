package com.dentistrybot.shared.state;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AdminFilterData {
    @JsonProperty("full_name")
    private String fullName;
    @JsonProperty("course")
    private int course;
    @JsonProperty("group_name")
    private String groupName;
    @JsonProperty("faculty")
    private String faculty;
    @JsonProperty("page")
    private int page;

    public AdminFilterData() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public int getCourse() { return course; }
    public void setCourse(int course) { this.course = course; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
}

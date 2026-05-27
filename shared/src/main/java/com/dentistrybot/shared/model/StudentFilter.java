package com.dentistrybot.shared.model;

public class StudentFilter {
    private String fullName;
    private int course;
    private String groupName;
    private String subgroup;
    private String faculty;
    private int limit = 10;
    private int offset = 0;

    public StudentFilter() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public int getCourse() { return course; }
    public void setCourse(int course) { this.course = course; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getSubgroup() { return subgroup; }
    public void setSubgroup(String subgroup) { this.subgroup = subgroup; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
}

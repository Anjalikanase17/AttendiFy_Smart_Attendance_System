package com.example.attendance_Backend.dto;

public class TeacherSubjectReportDTO {
    private String subject;
    private long presentCount;
    private long totalCount;
    private double percentage;

    public TeacherSubjectReportDTO() {
    }

    public TeacherSubjectReportDTO(String subject, long presentCount, long totalCount) {
        this.subject = subject;
        this.presentCount = presentCount;
        this.totalCount = totalCount;
        this.percentage = totalCount > 0 ? (presentCount * 100.0 / totalCount) : 0;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public long getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(long presentCount) {
        this.presentCount = presentCount;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}

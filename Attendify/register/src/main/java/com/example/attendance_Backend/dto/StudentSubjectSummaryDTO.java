package com.example.attendance_Backend.dto;

public class StudentSubjectSummaryDTO {
    private String subject;
    private long presentCount;
    private long totalClasses;
    private double percentage;

    public StudentSubjectSummaryDTO() {
    }

    public StudentSubjectSummaryDTO(String subject, long presentCount, long totalClasses) {
        this.subject = subject;
        this.presentCount = presentCount;
        this.totalClasses = totalClasses;
        this.percentage = totalClasses > 0 ? (presentCount * 100.0 / totalClasses) : 0;
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

    public long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(long totalClasses) {
        this.totalClasses = totalClasses;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}

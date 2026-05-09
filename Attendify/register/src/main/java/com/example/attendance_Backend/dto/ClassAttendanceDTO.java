package com.example.attendance_Backend.dto;

public class ClassAttendanceDTO {
    private String className;
    private long presentCount;
    private long totalCount;
    private double percentage;

    public ClassAttendanceDTO() {
    }

    public ClassAttendanceDTO(String className, long presentCount, long totalCount) {
        this.className = className;
        this.presentCount = presentCount;
        this.totalCount = totalCount;
        this.percentage = totalCount > 0 ? (presentCount * 100.0 / totalCount) : 0;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
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

package com.example.attendance_Backend.dto;

public class MonthlyAttendanceDTO {
    private int month;
    private long presentDays;
    private long totalDays;

    public MonthlyAttendanceDTO() {
    }

    public MonthlyAttendanceDTO(int month, long presentDays, long totalDays) {
        this.month = month;
        this.presentDays = presentDays;
        this.totalDays = totalDays;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public long getPresentDays() {
        return presentDays;
    }

    public void setPresentDays(long presentDays) {
        this.presentDays = presentDays;
    }

    public long getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(long totalDays) {
        this.totalDays = totalDays;
    }
}

package com.example.attendance_Backend.dto;

public class DashboardDTO {

    private int totalClasses;
    private int present;
    private int absent;
    private int percentage;

    public DashboardDTO(int totalClasses, int present, int absent, int percentage) {
        this.totalClasses = totalClasses;
        this.present = present;
        this.absent = absent;
        this.percentage = percentage;
    }

    public int getTotalClasses() { return totalClasses; }
    public int getPresent() { return present; }
    public int getAbsent() { return absent; }
    public int getPercentage() { return percentage; }
}

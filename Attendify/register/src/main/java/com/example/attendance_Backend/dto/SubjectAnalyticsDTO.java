package com.example.attendance_Backend.dto;

public class SubjectAnalyticsDTO {

    private String subject;
    private long total;
    private long present;
    private long absent;

    public SubjectAnalyticsDTO(String subject, long total, long present, long absent) {
        this.subject = subject;
        this.total = total;
        this.present = present;
        this.absent = absent;
    }

    public String getSubject() { return subject; }
    public long getTotal() { return total; }
    public long getPresent() { return present; }
    public long getAbsent() { return absent; }
}

package com.example.attendance_Backend.dto;

import java.time.LocalDate;

public class DateAnalyticsDTO {

    private LocalDate date;
    private Long total;
    private Long present;
    private Long absent;

    public DateAnalyticsDTO(LocalDate date, Long total, Long present, Long absent) {
        this.date = date;
        this.total = total;
        this.present = present;
        this.absent = absent;
    }

    public LocalDate getDate() { return date; }
    public Long getTotal() { return total; }
    public Long getPresent() { return present; }
    public Long getAbsent() { return absent; }
}

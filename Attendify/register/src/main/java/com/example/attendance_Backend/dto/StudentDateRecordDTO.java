package com.example.attendance_Backend.dto;

import java.time.LocalDate;

public class StudentDateRecordDTO {
    private LocalDate date;
    private String subject;
    private String status;

    public StudentDateRecordDTO() {
    }

    public StudentDateRecordDTO(LocalDate date, String subject, String status) {
        this.date = date;
        this.subject = subject;
        this.status = status;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

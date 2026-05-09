package com.example.attendance_Backend.dto;

import java.time.LocalDate;

public class AttendanceDTO {

    private LocalDate date;
    private String subject;
    private String rollNo;
    private String name;
    private String status;

    // Constructor for TEACHER VIEW
    public AttendanceDTO(
            LocalDate date,
            String subject,
            String rollNo,
            String name,
            String status
    ) {
        this.date = date;
        this.subject = subject;
        this.rollNo = rollNo;
        this.name = name;
        this.status = status;
    }

    // Constructor for STUDENT VIEW (old usage)
    public AttendanceDTO(LocalDate date, String subject, String status) {
        this.date = date;
        this.subject = subject;
        this.status = status;
    }

    public LocalDate getDate() { return date; }
    public String getSubject() { return subject; }
    public String getRollNo() { return rollNo; }
    public String getName() { return name; }
    public String getStatus() { return status; }
}

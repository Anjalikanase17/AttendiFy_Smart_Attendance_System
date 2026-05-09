package com.example.attendance_Backend.dto;

public class LowAttendanceStudentDTO {
    private int studentId;
    private String name;
    private String rollNo;
    private String className;
    private double percentage;

    public LowAttendanceStudentDTO() {
    }

    public LowAttendanceStudentDTO(int studentId, String name, String rollNo,
            String className, long presentCount, long totalCount) {
        this.studentId = studentId;
        this.name = name;
        this.rollNo = rollNo;
        this.className = className;
        this.percentage = totalCount > 0 ? (presentCount * 100.0 / totalCount) : 0;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRollNo() {
        return rollNo;
    }

    public void setRollNo(String rollNo) {
        this.rollNo = rollNo;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public double getPercentage() {
        return percentage;
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
    }
}

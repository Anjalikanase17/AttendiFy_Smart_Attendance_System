package com.example.attendance_Backend.dto;

public class TeacherAssignmentDTO {
    private int id;
    private int teacherId;
    private String teacherName;
    private String subject;
    private String className;
    private String division;
    private String roomNumber;

    public TeacherAssignmentDTO() {
    }

    public TeacherAssignmentDTO(int id, int teacherId, String teacherName,
            String subject, String className, String division, String roomNumber) {
        this.id = id;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.subject = subject;
        this.className = className;
        this.division = division;
        this.roomNumber = roomNumber;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(int teacherId) {
        this.teacherId = teacherId;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getDivision() {
        return division;
    }

    public void setDivision(String division) {
        this.division = division;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }
}

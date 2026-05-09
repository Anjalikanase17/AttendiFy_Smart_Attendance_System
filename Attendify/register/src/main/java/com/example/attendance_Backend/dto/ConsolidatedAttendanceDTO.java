package com.example.attendance_Backend.dto;

import java.util.List;
import java.util.Map;

public class ConsolidatedAttendanceDTO {
    private List<String> subjects;
    private List<StudentConsolidatedDTO> students;

    public ConsolidatedAttendanceDTO() {
    }

    public ConsolidatedAttendanceDTO(List<String> subjects, List<StudentConsolidatedDTO> students) {
        this.subjects = subjects;
        this.students = students;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public List<StudentConsolidatedDTO> getStudents() {
        return students;
    }

    public void setStudents(List<StudentConsolidatedDTO> students) {
        this.students = students;
    }
}

package com.example.attendance_Backend.dto;

import java.util.Map;

public class StudentConsolidatedDTO {
    private String name;
    private String rollNo;
    private Map<String, Double> subjectPercentages;
    private Double overallPercentage;

    public StudentConsolidatedDTO() {
    }

    public StudentConsolidatedDTO(String name, String rollNo, Map<String, Double> subjectPercentages,
            Double overallPercentage) {
        this.name = name;
        this.rollNo = rollNo;
        this.subjectPercentages = subjectPercentages;
        this.overallPercentage = overallPercentage;
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

    public Map<String, Double> getSubjectPercentages() {
        return subjectPercentages;
    }

    public void setSubjectPercentages(Map<String, Double> subjectPercentages) {
        this.subjectPercentages = subjectPercentages;
    }

    public Double getOverallPercentage() {
        return overallPercentage;
    }

    public void setOverallPercentage(Double overallPercentage) {
        this.overallPercentage = overallPercentage;
    }
}

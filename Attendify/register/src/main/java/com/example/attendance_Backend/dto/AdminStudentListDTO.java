package com.example.attendance_Backend.dto;

public class AdminStudentListDTO {
    private final Integer id;
    private final String name;
    private final String rollNo;
    private final String email;
    private final String mobilenumber;
    private final String address;
    private final Integer classId;
    private final String className;
    private final Integer divisionId;
    private final String divisionName;

    public AdminStudentListDTO(Integer id, String name, String rollNo, String email, String mobilenumber, String address,
            Integer classId, String className, Integer divisionId, String divisionName) {
        this.id = id;
        this.name = name;
        this.rollNo = rollNo;
        this.email = email;
        this.mobilenumber = mobilenumber;
        this.address = address;
        this.classId = classId;
        this.className = className;
        this.divisionId = divisionId;
        this.divisionName = divisionName;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRollNo() {
        return rollNo;
    }

    public String getEmail() {
        return email;
    }

    public String getMobilenumber() {
        return mobilenumber;
    }

    public String getAddress() {
        return address;
    }

    public Integer getClassId() {
        return classId;
    }

    public String getClassName() {
        return className;
    }

    public Integer getDivisionId() {
        return divisionId;
    }

    public String getDivisionName() {
        return divisionName;
    }
}

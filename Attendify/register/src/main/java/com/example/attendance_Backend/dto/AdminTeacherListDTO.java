package com.example.attendance_Backend.dto;

public class AdminTeacherListDTO {
    private final Integer id;
    private final String name;
    private final String email;
    private final String mobilenumber;
    private final Integer departmentId;
    private final String departmentName;

    public AdminTeacherListDTO(Integer id, String name, String email, String mobilenumber, Integer departmentId,
            String departmentName) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.mobilenumber = mobilenumber;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMobilenumber() {
        return mobilenumber;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }
}

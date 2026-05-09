package com.example.attendance_Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "teachers", indexes = {
    @Index(name = "idx_teacher_admin_dept_name", columnList = "admin_id, department_id, name"),
    @Index(name = "idx_teacher_admin_email", columnList = "admin_id, email")
})
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @OneToMany(mappedBy = "teacher", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<TeacherAssignment> assignments;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department; 

    @Column(unique = true)
    private String email;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String mobilenumber;

    private String role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    @JsonIgnore
    private Admin admin;

    @Column(name = "device_id", length = 200)
    private String deviceId;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "permanent_address", columnDefinition = "TEXT")
    private String permanentAddress;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    private String education;

    @Column(name = "date_of_appointment")
    private LocalDate dateOfAppointment;

    @Column(name = "spouse_name")
    private String spouseName;

    @Column(name = "spouse_contact")
    private String spouseContact;

    @Transient
    private String schoolCode;

    public Teacher() {
    }

    public Teacher(String name, Department department, String email, String mobilenumber, String password) {
        this.name = name;
        this.department = department;
        this.email = email;
        this.mobilenumber = mobilenumber;
        this.password = password;
        this.role = "TEACHER";
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMobilenumber() { return mobilenumber; }
    public void setMobilenumber(String mobilenumber) { this.mobilenumber = mobilenumber; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public String getSchoolCode() { return schoolCode; }
    public void setSchoolCode(String schoolCode) { this.schoolCode = schoolCode; }

    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }

    public LocalDate getDateOfAppointment() { return dateOfAppointment; }
    public void setDateOfAppointment(LocalDate dateOfAppointment) { this.dateOfAppointment = dateOfAppointment; }

    public String getSpouseName() { return spouseName; }
    public void setSpouseName(String spouseName) { this.spouseName = spouseName; }

    public String getSpouseContact() { return spouseContact; }
    public void setSpouseContact(String spouseContact) { this.spouseContact = spouseContact; }
}

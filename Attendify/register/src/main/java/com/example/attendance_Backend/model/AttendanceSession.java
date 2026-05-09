package com.example.attendance_Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_session", indexes = {
    @Index(name = "idx_session_admin_slot_expiry", columnList = "admin_id, timetable_slot_id, expiry_time"),
    @Index(name = "idx_session_admin_teacher_expiry", columnList = "admin_id, teacher_id, expiry_time")
})
public class AttendanceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id")
    private SubjectMaster subjectMaster;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id")
    private ClassMaster classMaster;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "division_id")
    private DivisionMaster divisionMaster;

    private double teacherLat;
    private double teacherLng;
    private double radiusKm = 0.1;
    private String teacherDeviceId;

    @Column(name = "teacher_id")
    private Integer teacherId;

    private LocalDateTime expiryTime;

    /**
     * FK to teacher_timetable — set when session is created from a timetable slot
     */
    @Column(name = "timetable_slot_id")
    private Integer timetableSlotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    @JsonIgnore
    private Admin admin;

    // ===== Getters & Setters =====

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SubjectMaster getSubjectMaster() {
        return subjectMaster;
    }

    public void setSubjectMaster(SubjectMaster subjectMaster) {
        this.subjectMaster = subjectMaster;
    }

    public ClassMaster getClassMaster() {
        return classMaster;
    }

    public void setClassMaster(ClassMaster classMaster) {
        this.classMaster = classMaster;
    }

    public DivisionMaster getDivisionMaster() {
        return divisionMaster;
    }

    public void setDivisionMaster(DivisionMaster divisionMaster) {
        this.divisionMaster = divisionMaster;
    }

    public double getTeacherLat() {
        return teacherLat;
    }

    public void setTeacherLat(double teacherLat) {
        this.teacherLat = teacherLat;
    }

    public double getTeacherLng() {
        return teacherLng;
    }

    public void setTeacherLng(double teacherLng) {
        this.teacherLng = teacherLng;
    }

    public double getRadiusKm() {
        return radiusKm;
    }

    public void setRadiusKm(double radiusKm) {
        this.radiusKm = radiusKm;
    }

    public String getTeacherDeviceId() {
        return teacherDeviceId;
    }

    public void setTeacherDeviceId(String teacherDeviceId) {
        this.teacherDeviceId = teacherDeviceId;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public Integer getTimetableSlotId() {
        return timetableSlotId;
    }

    public void setTimetableSlotId(Integer timetableSlotId) {
        this.timetableSlotId = timetableSlotId;
    }

    public Integer getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Integer teacherId) {
        this.teacherId = teacherId;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }
}

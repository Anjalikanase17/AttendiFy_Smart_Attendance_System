package com.example.attendance_Backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "class_subject")
public class ClassSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassMaster classMaster;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private SubjectMaster subjectMaster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Admin admin;

    public ClassSubject() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public ClassMaster getClassMaster() {
        return classMaster;
    }

    public void setClassMaster(ClassMaster classMaster) {
        this.classMaster = classMaster;
    }

    public SubjectMaster getSubjectMaster() {
        return subjectMaster;
    }

    public void setSubjectMaster(SubjectMaster subjectMaster) {
        this.subjectMaster = subjectMaster;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }
}

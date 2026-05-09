package com.example.attendance_Backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "class_master", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "admin_id", "class_name" })
})
public class ClassMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String className;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = true)
    private Department department;

    @OneToMany(mappedBy = "classMaster", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DivisionMaster> divisions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    @JsonIgnore
    private Admin admin;

    // We don't map subjects here directly anymore, they'll go through ClassSubject
    // mapping table

    public ClassMaster() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<DivisionMaster> getDivisions() {
        return divisions;
    }

    public void setDivisions(List<DivisionMaster> divisions) {
        this.divisions = divisions;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }
}

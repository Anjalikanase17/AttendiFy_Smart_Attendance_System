package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    List<Department> findByAdminId(Long adminId);

    Optional<Department> findByDepartmentNameAndAdminId(String departmentName, Long adminId);
}

package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.ClassMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface ClassMasterRepository extends JpaRepository<ClassMaster, Integer> {
    List<ClassMaster> findByAdminId(Long adminId);

    Optional<ClassMaster> findByIdAndAdminId(Integer id, Long adminId);

    long countByAdminId(Long adminId);

    List<ClassMaster> findByDepartmentIdAndAdminId(Integer departmentId, Long adminId);

    Optional<ClassMaster> findByClassNameAndAdminId(String className, Long adminId);

    // Legacy methods during migration
    List<ClassMaster> findByDepartmentId(Integer departmentId);

    Optional<ClassMaster> findByClassName(String className);
}

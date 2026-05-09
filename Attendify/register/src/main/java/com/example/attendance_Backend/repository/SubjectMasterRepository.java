package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.SubjectMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface SubjectMasterRepository extends JpaRepository<SubjectMaster, Integer> {
    List<SubjectMaster> findByAdminId(Long adminId);

    Optional<SubjectMaster> findByIdAndAdminId(Integer id, Long adminId);

    List<SubjectMaster> findByDepartmentIdAndAdminId(Integer departmentId, Long adminId);

    Optional<SubjectMaster> findBySubjectNameAndAdminId(String subjectName, Long adminId);

    // Legacy methods during migration
    List<SubjectMaster> findByDepartmentId(Integer departmentId);

    Optional<SubjectMaster> findBySubjectName(String subjectName);
}

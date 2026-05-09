package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, Integer> {
    List<ClassSubject> findByClassMasterId(Integer classId);

    List<ClassSubject> findByClassMasterIdAndAdminId(Integer classId, Long adminId);

    List<ClassSubject> findByAdminId(Long adminId);

    List<ClassSubject> findBySubjectMasterId(Integer subjectId);

    Optional<ClassSubject> findByClassMasterIdAndSubjectMasterId(Integer classId, Integer subjectId);

    Optional<ClassSubject> findByClassMasterIdAndSubjectMasterIdAndAdminId(Integer classId, Integer subjectId,
            Long adminId);
}

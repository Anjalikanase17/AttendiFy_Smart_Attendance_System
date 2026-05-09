package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, Integer> {

    List<TeacherAssignment> findByTeacherIdAndAdminId(int teacherId, Long adminId);

    // Legacy method
    List<TeacherAssignment> findByTeacherId(int teacherId);

    boolean existsByTeacherIdAndSubjectAndClassNameAndAdminId(int teacherId, String subject, String className,
            Long adminId);

    // Legacy method
    boolean existsByTeacherIdAndSubjectAndClassName(int teacherId, String subject, String className);
}

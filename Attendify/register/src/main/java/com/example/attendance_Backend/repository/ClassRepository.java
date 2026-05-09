package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
    List<ClassEntity> findByTeacherName(String teacherName);


}

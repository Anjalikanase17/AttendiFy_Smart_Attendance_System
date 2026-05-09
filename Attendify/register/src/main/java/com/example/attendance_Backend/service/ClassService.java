package com.example.attendance_Backend.service;

import com.example.attendance_Backend.model.ClassEntity;
import com.example.attendance_Backend.repository.ClassRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClassService {

    private final ClassRepository repo;

    public ClassService(ClassRepository repo) {
        this.repo = repo;
    }

    public ClassEntity saveClass(ClassEntity cls) {
        return repo.save(cls);
    }

    public List<ClassEntity> getClassesByTeacher(String teacherName) {
        return repo.findByTeacherName(teacherName);
    }
}

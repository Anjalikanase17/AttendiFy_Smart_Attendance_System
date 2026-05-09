package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.model.ClassEntity;
import com.example.attendance_Backend.service.ClassService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/classes")
@CrossOrigin("*")
public class ClassController {

    private final ClassService service;

    public ClassController(ClassService service) {
        this.service = service;
    }

    // ADD CLASS
    @PostMapping("/add")
    public ClassEntity addClass(@RequestBody ClassEntity cls) {
        return service.saveClass(cls);
    }

    // GET CLASSES BY TEACHER
    @GetMapping("/teacher/{teacherName}")
    public List<ClassEntity> getClasses(@PathVariable String teacherName) {
        return service.getClassesByTeacher(teacherName.trim());
    }
}

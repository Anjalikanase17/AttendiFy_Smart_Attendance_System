package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.*;
import com.example.attendance_Backend.service.TeacherAssignmentService;
import com.example.attendance_Backend.service.TeacherReportService;
import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.service.TeacherService;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reports")
@CrossOrigin(origins = "*")
public class AdminReportController {

    private final TeacherReportService reportService;
    private final TeacherAssignmentService assignmentService;
    private final TeacherService teacherService;

    public AdminReportController(TeacherReportService reportService,
            TeacherAssignmentService assignmentService,
            TeacherService teacherService) {
        this.reportService = reportService;
        this.assignmentService = assignmentService;
        this.teacherService = teacherService;
    }

    /**
     * Class-wise attendance report
     * GET /api/admin/reports/class-attendance
     */
    @Cacheable(cacheNames = "report_class_attendance", key = "#principal != null ? #principal.name : 'anon'")
    @GetMapping("/class-attendance")
    public ResponseEntity<List<ClassAttendanceDTO>> getClassAttendance(Principal principal) {
        return ResponseEntity.ok(reportService.getClassAttendanceReport());
    }

    /**
     * Low attendance students
     * GET /api/admin/reports/low-attendance?threshold=75
     */
    @Cacheable(cacheNames = "report_low_attendance", key = "(#principal != null ? #principal.name : 'anon') + ':' + #threshold")
    @GetMapping("/low-attendance")
    public ResponseEntity<List<LowAttendanceStudentDTO>> getLowAttendance(
            Principal principal,
            @RequestParam(defaultValue = "75") double threshold) {
        return ResponseEntity.ok(reportService.getLowAttendanceStudents(threshold));
    }

    /**
     * One teacher's assignments + subject report
     * GET /api/admin/reports/teacher/{teacherId}
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<Map<String, Object>> getTeacherReport(@PathVariable int teacherId) {
        Map<String, Object> report = new HashMap<>();
        report.put("assignments", assignmentService.getAssignmentsForTeacher(teacherId));
        return ResponseEntity.ok(report);
    }

    /**
     * Summary of all teachers with their assignments
     * GET /api/admin/reports/all-teachers
     */
    @GetMapping("/all-teachers")
    public ResponseEntity<List<Map<String, Object>>> getAllTeachersReport(
            @RequestParam(defaultValue = "200") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        Long adminId = AdminContextHolder.getAdminId();
        PageRequest pageable = PageRequest.of(0, safeLimit, Sort.by("id").descending());
        List<Teacher> teachers;
        if (adminId != null) {
            teachers = teacherService.searchTeachersForAdmin(adminId, null, null, pageable).getContent();
        } else {
            teachers = teacherService.getAllTeachers();
        }

        List<Map<String, Object>> result = teachers.stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("email", t.getEmail());
            m.put("department", t.getDepartment());
            m.put("assignments", assignmentService.getAssignmentsForTeacher(t.getId()));
            return m;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping(value = "/all-teachers", params = { "page", "size" })
    public ResponseEntity<Map<String, Object>> getAllTeachersReportPaged(
            @RequestParam int page,
            @RequestParam int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Long adminId = AdminContextHolder.getAdminId();

        Page<Teacher> teachersPage = teacherService.searchTeachersForAdmin(
                adminId,
                null,
                null,
                PageRequest.of(safePage, safeSize, Sort.by("id").descending()));

        List<Map<String, Object>> content = teachersPage.getContent().stream().map(t -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", t.getId());
            m.put("name", t.getName());
            m.put("email", t.getEmail());
            m.put("department", t.getDepartment());
            m.put("assignments", assignmentService.getAssignmentsForTeacher(t.getId()));
            return m;
        }).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("page", teachersPage.getNumber());
        response.put("size", teachersPage.getSize());
        response.put("totalElements", teachersPage.getTotalElements());
        response.put("totalPages", teachersPage.getTotalPages());
        response.put("isFirst", teachersPage.isFirst());
        response.put("isLast", teachersPage.isLast());
        return ResponseEntity.ok(response);
    }
}

package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.AuthRequest;
import com.example.attendance_Backend.dto.AdminStudentListDTO;
import com.example.attendance_Backend.dto.AdminTeacherListDTO;
import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.TeacherRepository;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.service.AdminService;
import com.example.attendance_Backend.service.AuthService;
import com.example.attendance_Backend.service.TeacherService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final TeacherService teacherService;
    private final AdminService adminService;
    private final AuthService authService;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    public AdminController(
            TeacherService teacherService,
            AdminService adminService,
            AuthService authService,
            UserRepository userRepository,
            TeacherRepository teacherRepository) {
        this.teacherService = teacherService;
        this.adminService = adminService;
        this.authService = authService;
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginAdmin(@RequestBody AuthRequest request) {
        return authService.loginAdmin(request.getEmail(), request.getPassword())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body(Map.of("message", "Invalid email or password")));
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerAdmin(@RequestBody com.example.attendance_Backend.model.Admin admin) {
        try {
            com.example.attendance_Backend.model.Admin savedAdmin = adminService.registerAdmin(admin);
            savedAdmin.setPassword(null);
            return ResponseEntity.ok(savedAdmin);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Get all teachers
    @GetMapping("/teachers")
    public List<Teacher> getAllTeachers(@RequestParam(defaultValue = "300") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null) {
            return List.of();
        }
        PageRequest pageable = PageRequest.of(0, safeLimit, Sort.by("id").descending());
        return teacherRepository.findByAdminId(adminId, pageable);
    }

    @GetMapping(value = "/teachers", params = { "page", "size" })
    public Map<String, Object> getTeachersPaged(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String q) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null) {
            return toPagedResponse(List.of(), Page.empty(PageRequest.of(safePage, safeSize)));
        }

        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("id").descending());
        Page<AdminTeacherListDTO> teacherPage = teacherRepository.searchTeachersForAdminProjected(
                adminId,
                departmentId,
                normalizeQuery(q),
                pageable);

        List<Map<String, Object>> content = teacherPage.getContent().stream()
                .map(this::toTeacherMap)
                .toList();

        return toPagedResponse(content, teacherPage);
    }

    // Get teacher by ID
    @GetMapping("/teachers/{id}")
    public ResponseEntity<?> getTeacherById(@PathVariable Integer id) {
        Optional<Teacher> teacherOpt = teacherService.getTeacherById(id);

        if (teacherOpt.isPresent()) {
            return ResponseEntity.ok(teacherOpt.get()); // Teacher object
        } else {
            return ResponseEntity.status(404).body("Teacher not found"); // String error
        }
    }

    // Delete teacher
    @DeleteMapping("/teachers/{id}")
    public ResponseEntity<?> deleteTeacher(@PathVariable Integer id) {
        try {
            teacherService.deleteTeacher(id);
            return ResponseEntity.ok("Teacher deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTeachers", adminService.getTotalTeachers());
        stats.put("totalStudents", adminService.getTotalStudents());
        stats.put("totalClasses", adminService.getTotalClasses());
        stats.put("todaysAttendancePercent", adminService.getTodaysAttendancePercent());
        return stats;
    }

    // Get all students as flat maps (no circular reference)
    @GetMapping("/students")
    public List<Map<String, Object>> getAllStudents(@RequestParam(defaultValue = "500") int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null) {
            return List.of();
        }
        PageRequest pageable = PageRequest.of(0, safeLimit, Sort.by("id").descending());
        List<User> users = userRepository.findByAdminId(adminId, pageable);

        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : users) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("rollNo", u.getRollNo());
            m.put("email", u.getEmail());
            m.put("mobilenumber", u.getMobilenumber());
            m.put("address", u.getAddress());
            if (u.getClassMaster() != null) {
                m.put("classId", u.getClassMaster().getId());
                m.put("className", u.getClassMaster().getClassName());
            } else {
                m.put("classId", null);
                m.put("className", null);
            }
            if (u.getDivisionMaster() != null) {
                m.put("divisionId", u.getDivisionMaster().getId());
                m.put("divisionName", u.getDivisionMaster().getDivisionName());
            } else {
                m.put("divisionId", null);
                m.put("divisionName", null);
            }
            result.add(m);
        }
        return result;
    }

    @GetMapping(value = "/students", params = { "page", "size" })
    public Map<String, Object> getStudentsPaged(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer divisionId,
            @RequestParam(required = false) String q) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null) {
            return toPagedResponse(List.of(), Page.empty(PageRequest.of(safePage, safeSize)));
        }

        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("id").descending());
        Page<AdminStudentListDTO> usersPage = userRepository.searchStudentsForAdminProjected(
                adminId,
                departmentId,
                classId,
                divisionId,
                normalizeQuery(q),
                pageable);

        List<Map<String, Object>> content = usersPage.getContent().stream()
                .map(this::toStudentMap)
                .toList();
        return toPagedResponse(content, usersPage);
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getAdminProfile() {
        try {
            com.example.attendance_Backend.model.Admin admin = adminService.getAdminProfile();
            admin.setPassword(null);
            return ResponseEntity.ok(admin);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        }
    }

    private String normalizeQuery(String q) {
        if (q == null) {
            return null;
        }
        String trimmed = q.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Map<String, Object> toStudentMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("rollNo", u.getRollNo());
        m.put("email", u.getEmail());
        m.put("mobilenumber", u.getMobilenumber());
        m.put("address", u.getAddress());

        if (u.getClassMaster() != null) {
            m.put("classId", u.getClassMaster().getId());
            m.put("className", u.getClassMaster().getClassName());
        } else {
            m.put("classId", null);
            m.put("className", null);
        }
        if (u.getDivisionMaster() != null) {
            m.put("divisionId", u.getDivisionMaster().getId());
            m.put("divisionName", u.getDivisionMaster().getDivisionName());
        } else {
            m.put("divisionId", null);
            m.put("divisionName", null);
        }
        return m;
    }

    private Map<String, Object> toStudentMap(AdminStudentListDTO u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("rollNo", u.getRollNo());
        m.put("email", u.getEmail());
        m.put("mobilenumber", u.getMobilenumber());
        m.put("address", u.getAddress());
        m.put("classId", u.getClassId());
        m.put("className", u.getClassName());
        m.put("divisionId", u.getDivisionId());
        m.put("divisionName", u.getDivisionName());
        return m;
    }

    private Map<String, Object> toTeacherMap(Teacher t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("email", t.getEmail());
        m.put("mobilenumber", t.getMobilenumber());
        if (t.getDepartment() != null) {
            Map<String, Object> dept = new LinkedHashMap<>();
            dept.put("id", t.getDepartment().getId());
            dept.put("departmentName", t.getDepartment().getDepartmentName());
            m.put("department", dept);
        } else {
            m.put("department", null);
        }
        return m;
    }

    private Map<String, Object> toTeacherMap(AdminTeacherListDTO t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("name", t.getName());
        m.put("email", t.getEmail());
        m.put("mobilenumber", t.getMobilenumber());
        if (t.getDepartmentId() != null) {
            Map<String, Object> dept = new LinkedHashMap<>();
            dept.put("id", t.getDepartmentId());
            dept.put("departmentName", t.getDepartmentName());
            m.put("department", dept);
        } else {
            m.put("department", null);
        }
        return m;
    }

    private <T> Map<String, Object> toPagedResponse(List<T> content, Page<?> pageData) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("page", pageData.getNumber());
        response.put("size", pageData.getSize());
        response.put("totalElements", pageData.getTotalElements());
        response.put("totalPages", pageData.getTotalPages());
        response.put("isFirst", pageData.isFirst());
        response.put("isLast", pageData.isLast());
        return response;
    }

}

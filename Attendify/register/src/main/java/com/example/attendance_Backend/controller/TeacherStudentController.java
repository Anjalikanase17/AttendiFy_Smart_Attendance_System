package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.StudentAttendanceDTO;
import com.example.attendance_Backend.model.Attendance;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.AttendanceRepository;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.repository.SubjectMasterRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.attendance_Backend.security.AdminContextHolder;
import com.example.attendance_Backend.model.Admin;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class TeacherStudentController {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final SubjectMasterRepository subjectMasterRepository;

    public TeacherStudentController(AttendanceRepository attendanceRepository, UserRepository userRepository,
            SubjectMasterRepository subjectMasterRepository) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.subjectMasterRepository = subjectMasterRepository;
    }

    // ✅ Get All Students
    @GetMapping
    public List<User> getAllStudents(@RequestParam(defaultValue = "500") int limit) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        return userRepository.findByAdminId(adminId, PageRequest.of(0, safeLimit, Sort.by("id").descending()));
    }

    @GetMapping(params = { "page", "size" })
    public Map<String, Object> getAllStudentsPaged(@RequestParam int page, @RequestParam int size) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null) {
            return Collections.emptyMap();
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Page<User> usersPage = userRepository.searchStudentsForAdmin(
                adminId,
                null,
                null,
                null,
                null,
                PageRequest.of(safePage, safeSize, Sort.by("id").descending()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", usersPage.getContent());
        response.put("page", usersPage.getNumber());
        response.put("size", usersPage.getSize());
        response.put("totalElements", usersPage.getTotalElements());
        response.put("totalPages", usersPage.getTotalPages());
        response.put("isFirst", usersPage.isFirst());
        response.put("isLast", usersPage.isLast());
        return response;
    }

    // ✅ View Single Student by ID
    @GetMapping("/{id}")
    public User getStudentById(@PathVariable int id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            throw new RuntimeException("Unauthorized");
        return userRepository.findByIdAndAdminId(id, adminId)
                .orElseThrow(() -> new RuntimeException("Student not found or unauthorized"));
    }

    // ✅ View Single Student by RollNo
    @GetMapping("/rollno/{rollNo}")
    public User getStudentByRollNo(@PathVariable String rollNo) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            throw new RuntimeException("Unauthorized");
        return userRepository.findByRollNoAndAdminId(rollNo, adminId)
                .orElseThrow(() -> new RuntimeException("Student not found or unauthorized"));
    }

    // ✅ Update Student Attendance by RollNo
    @Caching(evict = {
            @CacheEvict(cacheNames = {
                    "analytics_subject", "analytics_department", "analytics_date", "analytics_monthly_report",
                    "report_class_attendance", "report_low_attendance"
            }, allEntries = true)
    })
    @PutMapping("/teacher/update/{rollNo}")
    public ResponseEntity<?> updateStudent(
            @PathVariable String rollNo,
            @RequestBody StudentAttendanceDTO dto) {

        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Optional<Attendance> optionalAttendance = attendanceRepository.findByRollNoAndAdminId(rollNo, adminId);

        if (optionalAttendance.isEmpty()) {
            return ResponseEntity.badRequest().body("Attendance record not found");
        }

        Attendance attendance = optionalAttendance.get();
        if (dto.getSubject() != null && dto.getSubject().matches("\\d+")) {
            attendance.setSubjectMaster(
                    subjectMasterRepository.findById(Integer.parseInt(dto.getSubject())).orElse(null));
        }
        attendance.setStatus(dto.getStatus());

        attendanceRepository.save(attendance);

        return ResponseEntity.ok("Updated successfully");
    }

    // ✅ Delete Student by ID
    @Caching(evict = {
            @CacheEvict(cacheNames = {
                    "analytics_subject", "analytics_department", "analytics_date", "analytics_monthly_report",
                    "report_class_attendance", "report_low_attendance"
            }, allEntries = true)
    })
    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable int id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        Optional<User> user = userRepository.findByIdAndAdminId(id, adminId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Student not found or unauthorized");
        }
        userRepository.delete(user.get());
        return ResponseEntity.ok("Student deleted successfully");
    }

    // ✅ Add New Student
    @Caching(evict = {
            @CacheEvict(cacheNames = {
                    "analytics_subject", "analytics_department", "analytics_date", "analytics_monthly_report",
                    "report_class_attendance", "report_low_attendance"
            }, allEntries = true)
    })
    @PostMapping("/add-student")
    public ResponseEntity<String> addStudent(@RequestBody User user) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        Optional<User> existingUser = userRepository.findByRollNoAndAdminId(user.getRollNo(), adminId);

        if (existingUser.isPresent()) {
            return ResponseEntity.badRequest().body("Roll number already exists in your organization");
        }

        Admin admin = new Admin();
        admin.setId(adminId);
        user.setAdmin(admin);
        user.setRole("STUDENT"); // ✅ Automatically assign role
        userRepository.save(user);
        return ResponseEntity.ok("Student added successfully");
    }
}

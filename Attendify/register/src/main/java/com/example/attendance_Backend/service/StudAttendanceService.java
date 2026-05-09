package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.AttendanceDTO;
import com.example.attendance_Backend.dto.DashboardDTO;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.AttendanceRepository;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.security.AdminContextHolder;
import com.example.attendance_Backend.model.Admin;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StudAttendanceService {

    private final AttendanceRepository repository;
    private final UserRepository userRepository;
    private final PasswordService passwordService;

    public StudAttendanceService(
            AttendanceRepository repository,
            UserRepository userRepository,
            PasswordService passwordService) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    // =========================
    // Dashboard / Attendance
    // =========================
    public int getTotalClasses(int userId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return 0;
        return repository.totalClasses(userId, adminId);
    }

    public int getPresentClasses(int userId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return 0;
        return repository.presentCount(userId, adminId);
    }

    public List<AttendanceDTO> getAttendanceList(int userId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();
        return repository.attendanceList(userId, adminId);
    }

    public int getAbsentClasses(int userId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return 0;
        return repository.absentCount(userId, adminId);
    }

    public List<com.example.attendance_Backend.dto.StudentSubjectSummaryDTO> getSubjectWiseSummary(int studentId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();

        Optional<User> user = userRepository.findById(studentId);
        if (user.isPresent() && user.get().getClassMaster() != null) {
            return repository.getStudentSubjectSummary(studentId, adminId, user.get().getClassMaster().getId());
        }
        return java.util.Collections.emptyList();
    }

    public DashboardDTO getDashboardData(int userId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return new DashboardDTO(0, 0, 0, 0);

        int total = repository.totalClasses(userId, adminId);
        int present = repository.presentCount(userId, adminId);
        int absent = repository.absentCount(userId, adminId);
        int percentage = total == 0 ? 0 : (present * 100) / total;

        return new DashboardDTO(total, present, absent, percentage);
    }

    public DashboardDTO getDashboardData(int total, int present, int absent, int percentage) {
        return new DashboardDTO(total, present, absent, percentage);
    }

    // =========================
    // Student CRUD
    // =========================

    // =========================
    // Student CRUD
    // =========================

    // Get all students
    public List<User> getAllStudents() {
        return getAllStudents(500);
    }

    public List<User> getAllStudents(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 2000);
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return userRepository.findByAdminId(adminId, PageRequest.of(0, safeLimit, Sort.by("id").descending()));
        }
        return userRepository.findAll(PageRequest.of(0, safeLimit, Sort.by("id").descending())).getContent();
    }

    public Page<User> getAllStudentsPaged(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 200);
        Long adminId = AdminContextHolder.getAdminId();
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by("id").descending());
        if (adminId != null) {
            return userRepository.searchStudentsForAdmin(adminId, null, null, null, null, pageable);
        }
        return userRepository.searchStudentsForAdmin(null, null, null, null, null, pageable);
    }

    // Get student by rollNo
    public Optional<User> getStudentByRollNo(String rollNo) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return userRepository.findByRollNoAndAdminId(rollNo, adminId);
        }
        return userRepository.findByRollNo(rollNo);
    }

    // Save new student
    public User saveStudent(User student) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            Admin admin = new Admin();
            admin.setId(adminId);
            student.setAdmin(admin);
        }
        if (student.getPassword() != null && !student.getPassword().isBlank()) {
            student.setPassword(passwordService.encode(student.getPassword()));
        }
        return userRepository.save(student);
    }

    // Update student
    public Optional<User> updateStudent(String rollNo, User updatedStudent) {
        Long adminId = AdminContextHolder.getAdminId();
        Optional<User> optionalUser;
        if (adminId != null) {
            optionalUser = userRepository.findByRollNoAndAdminId(rollNo, adminId);
        } else {
            optionalUser = userRepository.findByRollNo(rollNo);
        }

        return optionalUser.map(student -> {
            student.setName(updatedStudent.getName());
            student.setClassMaster(updatedStudent.getClassMaster());
            student.setDivisionMaster(updatedStudent.getDivisionMaster());
            student.setEmail(updatedStudent.getEmail());
            student.setMobilenumber(updatedStudent.getMobilenumber());
            student.setAddress(updatedStudent.getAddress());
            return userRepository.save(student);
        });
    }

    // Delete student
    public boolean deleteStudent(String rollNo) {
        Long adminId = AdminContextHolder.getAdminId();
        Optional<User> optionalUser;
        if (adminId != null) {
            optionalUser = userRepository.findByRollNoAndAdminId(rollNo, adminId);
        } else {
            optionalUser = userRepository.findByRollNo(rollNo);
        }

        return optionalUser.map(student -> {
            userRepository.delete(student);
            return true;
        }).orElse(false);
    }

    // ===========================
    // New Methods for v2 APIs
    // ===========================

    public List<com.example.attendance_Backend.model.Attendance> getAllAttendance() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return repository.findAllWithDetails(adminId);
        }
        return repository.findAllWithDetails();
    }

    public com.example.attendance_Backend.model.Attendance getAttendanceByID(Long attendanceID) {
        return repository.findById(attendanceID.intValue()).orElse(null);
    }

    public List<com.example.attendance_Backend.model.Attendance> getAttendanceByStudentID(Long studentID) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return repository.findTop1000ByUser_IdAndAdmin_IdOrderByDateDesc(studentID.intValue(), adminId);
        }
        return repository.findTop1000ByUser_IdOrderByDateDesc(studentID.intValue());
    }

    public com.example.attendance_Backend.model.Attendance saveAttendance(com.example.attendance_Backend.model.Attendance attendance) {
        return repository.save(attendance);
    }
}

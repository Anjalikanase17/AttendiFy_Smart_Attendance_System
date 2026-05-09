package com.example.attendance_Backend.service;

import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.AttendanceRepository;
import com.example.attendance_Backend.repository.TeacherRepository;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AdminService {

    @Autowired
    private TeacherRepository teacherRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private AttendanceRepository attendanceRepo;

    @Autowired
    private com.example.attendance_Backend.repository.ClassMasterRepository classRepo;

    @Autowired
    private com.example.attendance_Backend.repository.AdminRepository adminRepo;

    @Autowired
    private PasswordService passwordService;

    public com.example.attendance_Backend.model.Admin registerAdmin(com.example.attendance_Backend.model.Admin admin) {
        if (adminRepo.findByEmail(admin.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }
        admin.setPassword(passwordService.encode(admin.getPassword()));
        admin.setSchoolCode(generateUniqueSchoolCode());
        return adminRepo.save(admin);
    }

    private String generateUniqueSchoolCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        String code = sb.toString();
        // Check collision
        if (adminRepo.findBySchoolCode(code).isPresent()) {
            return generateUniqueSchoolCode(); // retry
        }
        return code;
    }

    public long getTotalTeachers() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return teacherRepo.countByAdminId(adminId);
        }
        return teacherRepo.count();
    }

    public long getTotalStudents() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return userRepo.countByAdminId(adminId);
        }
        return userRepo.count();
    }

    public long getTotalClasses() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return classRepo.countByAdminId(adminId);
        }
        return classRepo.count();
    }

    public int getTodaysAttendancePercent() {
        LocalDate today = LocalDate.now();
        Long adminId = AdminContextHolder.getAdminId();

        int present;
        int total;

        if (adminId != null) {
            present = attendanceRepo.countPresentByDateAndAdminId(today, adminId);
            total = attendanceRepo.countTotalByDateAndAdminId(today, adminId);
        } else {
            present = attendanceRepo.countPresentByDate(today);
            total = attendanceRepo.countTotalByDate(today);
        }

        return total == 0 ? 0 : (present * 100 / total);
    }

    public List<Teacher> getRecentTeachers(int limit) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return teacherRepo.findByAdminId(adminId, PageRequest.of(0, limit, Sort.by("id").descending()));
        }
        return teacherRepo.findAll(PageRequest.of(0, limit, Sort.by("id").descending())).getContent();
    }

    public List<User> getRecentStudents(int limit) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return userRepo.findByAdminId(adminId, PageRequest.of(0, limit, Sort.by("id").descending()));
        }
        return userRepo.findAll(PageRequest.of(0, limit, Sort.by("id").descending())).getContent();
    }

    public com.example.attendance_Backend.model.Admin getAdminProfile() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null) {
            throw new RuntimeException("Unauthorized: No admin context found");
        }
        return adminRepo.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));
    }
}

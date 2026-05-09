package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.DateAnalyticsDTO;
import com.example.attendance_Backend.dto.SubjectAnalyticsDTO;
import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.repository.AttendanceRepository;
import com.example.attendance_Backend.repository.DepartmentRepository;
import com.example.attendance_Backend.repository.TeacherRepository;
import org.springframework.stereotype.Service;

import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.Optional;

@Service
public class TeacherService {

    private final TeacherRepository repository;
    private final AttendanceRepository attendanceRepository;
    private final PasswordService passwordService;
    private final DepartmentRepository departmentRepository;
    private final com.example.attendance_Backend.repository.AdminRepository adminRepository;

    public TeacherService(
            TeacherRepository repository,
            AttendanceRepository attendanceRepository,
            PasswordService passwordService,
            DepartmentRepository departmentRepository,
            com.example.attendance_Backend.repository.AdminRepository adminRepository) {
        this.repository = repository;
        this.attendanceRepository = attendanceRepository;
        this.passwordService = passwordService;
        this.departmentRepository = departmentRepository;
        this.adminRepository = adminRepository;
    }

    public Teacher registerTeacher(Teacher teacher) {
        Long adminId = AdminContextHolder.getAdminId();

        if (adminId != null) {
            Admin admin = new Admin();
            admin.setId(adminId);
            teacher.setAdmin(admin);
        } else if (teacher.getSchoolCode() != null && !teacher.getSchoolCode().isBlank()) {
            Admin admin = adminRepository.findBySchoolCode(teacher.getSchoolCode())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid School Code"));
            teacher.setAdmin(admin);
        } else if (teacher.getAdmin() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Admin context or School Code required for teacher registration");
        }

        if (teacher.getRole() == null || teacher.getRole().isBlank()) {
            teacher.setRole("TEACHER");
        }
        teacher.setPassword(passwordService.encode(teacher.getPassword()));
        return repository.save(teacher);
    }

    public boolean emailExists(String email) {
        return repository.existsByEmail(email);
    }

    public Optional<Teacher> login(String email, String password) {
        return repository.findByEmail(email)
                .filter(teacher -> passwordService.matches(password, teacher.getPassword()));
    }

    public Optional<Teacher> getTeacherByEmail(String email) {
        return repository.findByEmail(email);
    }

    public List<SubjectAnalyticsDTO> getSubjectAnalytics(Integer classId, Integer divisionId, Integer subjectId) {
        Long adminId = AdminContextHolder.getAdminId();
        Integer teacherId = getOptionalTeacherId();
        if (adminId != null)
            return attendanceRepository.getSubjectAnalyticsByAdminId(adminId, classId, divisionId, subjectId, teacherId);
        return java.util.Collections.emptyList();
    }

    public List<SubjectAnalyticsDTO> getDepartmentAnalytics(Integer classId, Integer divisionId, Integer subjectId) {
        Long adminId = AdminContextHolder.getAdminId();
        Integer teacherId = getOptionalTeacherId();
        if (adminId != null)
            return attendanceRepository.getDepartmentAnalyticsByAdminId(adminId, classId, divisionId, subjectId, teacherId);
        return java.util.Collections.emptyList();
    }

    public List<DateAnalyticsDTO> getDateAnalytics(Integer classId, Integer divisionId, Integer subjectId) {
        Long adminId = AdminContextHolder.getAdminId();
        Integer teacherId = getOptionalTeacherId();
        if (adminId != null)
            return attendanceRepository.getDateAnalyticsByAdminId(adminId, classId, divisionId, subjectId, teacherId);
        return java.util.Collections.emptyList();
    }

    private Integer getOptionalTeacherId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_teacher") || a.getAuthority().equals("ROLE_TEACHER"))) {
            return repository.findByEmail(auth.getName()).map(Teacher::getId).orElse(null);
        }
        return null;
    }

    public List<Teacher> getAllTeachers() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return repository.findByAdminId(adminId);
        }
        return repository.findAll(PageRequest.of(0, 1000, Sort.by("id").descending())).getContent();
    }

    public Page<Teacher> searchTeachersForAdmin(Long adminId, Integer departmentId, String q, Pageable pageable) {
        return repository.searchTeachersForAdmin(adminId, departmentId, q, pageable);
    }

    public Optional<Teacher> getTeacherById(Integer id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return repository.findByIdAndAdminId(id, adminId);
        }
        return repository.findById(id);
    }

    public Teacher updateTeacher(Integer id, Teacher teacherDetails) {
        Teacher teacher = getTeacherById(id)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (teacherDetails.getName() != null && !teacherDetails.getName().isBlank()) {
            teacher.setName(teacherDetails.getName());
        }
        if (teacherDetails.getDepartment() != null) {
            teacher.setDepartment(teacherDetails.getDepartment());
        }
        if (teacherDetails.getEmail() != null && !teacherDetails.getEmail().isBlank()) {
            teacher.setEmail(teacherDetails.getEmail());
        }
        if (teacherDetails.getMobilenumber() != null) {
            teacher.setMobilenumber(teacherDetails.getMobilenumber());
        }
        if (teacherDetails.getRole() != null && !teacherDetails.getRole().isBlank()) {
            teacher.setRole(teacherDetails.getRole());
        }
        if (teacherDetails.getPassword() != null && !teacherDetails.getPassword().isBlank()) {
            teacher.setPassword(passwordService.encode(teacherDetails.getPassword()));
        }

        // Enhanced Profile Fields
        if (teacherDetails.getPermanentAddress() != null) {
            teacher.setPermanentAddress(teacherDetails.getPermanentAddress());
        }
        if (teacherDetails.getDateOfBirth() != null) {
            teacher.setDateOfBirth(teacherDetails.getDateOfBirth());
        }
        if (teacherDetails.getEducation() != null) {
            teacher.setEducation(teacherDetails.getEducation());
        }
        if (teacherDetails.getBio() != null) {
            teacher.setBio(teacherDetails.getBio());
        }
        if (teacherDetails.getDateOfAppointment() != null) {
            teacher.setDateOfAppointment(teacherDetails.getDateOfAppointment());
        }
        if (teacherDetails.getSpouseName() != null) {
            teacher.setSpouseName(teacherDetails.getSpouseName());
        }
        if (teacherDetails.getSpouseContact() != null) {
            teacher.setSpouseContact(teacherDetails.getSpouseContact());
        }

        return repository.save(teacher);
    }

    public Teacher updateTeacherDepartment(Integer teacherId, Integer departmentId) {
        Teacher teacher = getTeacherById(teacherId)
                .orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (departmentId != null) {
            teacher.setDepartment(departmentRepository.findById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found")));
        } else {
            teacher.setDepartment(null);
        }

        return repository.save(teacher);
    }

    public void deleteTeacher(Integer id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            Teacher teacher = repository.findByIdAndAdminId(id, adminId)
                    .orElseThrow(() -> new RuntimeException("Teacher not found or unauthorized"));
            repository.delete(teacher);
        } else {
            if (!repository.existsById(id)) {
                throw new RuntimeException("Teacher not found");
            }
            repository.deleteById(id);
        }
    }

    public void saveDeviceId(Integer teacherId, String deviceId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return;
        repository.findByIdAndAdminId(teacherId, adminId).ifPresent(teacher -> {
            teacher.setDeviceId(deviceId);
            repository.save(teacher);
        });
    }

    public void updatePhotoUrl(Integer teacherId, String photoUrl) {
        Long adminId = AdminContextHolder.getAdminId();
        Teacher teacher;
        if (adminId != null) {
            teacher = repository.findByIdAndAdminId(teacherId, adminId)
                    .orElseGet(() -> repository.findById(teacherId).orElse(null));
        } else {
            teacher = repository.findById(teacherId).orElse(null);
        }
        if (teacher != null) {
            teacher.setPhotoUrl(photoUrl);
            repository.save(teacher);
        }
    }

    public Optional<String> getDeviceId(Integer teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Optional.empty();
        return repository.findByIdAndAdminId(teacherId, adminId).map(Teacher::getDeviceId);
    }

    public List<com.example.attendance_Backend.model.User> getStudentsForTeacher(int teacherId, Integer classId,
            Integer divisionId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();

        if (classId == null && divisionId == null) {
            return attendanceRepository.getStudentsForTeacher(teacherId, adminId);
        }
        return attendanceRepository.getFilteredStudentsForTeacher(teacherId, classId, divisionId, adminId);
    }

    public void updatePassword(Integer id, String currentPassword, String newPassword) {
        Long adminId = AdminContextHolder.getAdminId();
        Teacher teacher = (adminId != null)
                ? repository.findByIdAndAdminId(id, adminId)
                        .orElseThrow(() -> new RuntimeException("Teacher not found or unauthorized"))
                : repository.findById(id).orElseThrow(() -> new RuntimeException("Teacher not found"));

        if (!passwordService.matches(currentPassword, teacher.getPassword())) {
            throw new RuntimeException("Current password does not match");
        }

        teacher.setPassword(passwordService.encode(newPassword));
        repository.save(teacher);
    }

    public void resetPassword(String email, String mobile, String newPassword) {
        Teacher teacher = repository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Teacher not found with this email"));

        if (teacher.getMobilenumber() == null || !teacher.getMobilenumber().equals(mobile)) {
            throw new RuntimeException("Mobile number does not match our records");
        }

        teacher.setPassword(passwordService.encode(newPassword));
        repository.save(teacher);
    }
}

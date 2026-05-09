package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.AuthResponse;
import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.AdminRepository;
import com.example.attendance_Backend.repository.TeacherRepository;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.security.JwtService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final AdminRepository adminRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordService passwordService;

    public AuthService(
            AdminRepository adminRepository,
            TeacherRepository teacherRepository,
            UserRepository userRepository,
            JwtService jwtService,
            PasswordService passwordService) {
        this.adminRepository = adminRepository;
        this.teacherRepository = teacherRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
    }

    public Optional<AuthResponse> loginAdmin(String email, String rawPassword) {
        Optional<Admin> adminOptional = adminRepository.findByEmail(email);
        if (adminOptional.isEmpty()) {
            return Optional.empty();
        }

        Admin admin = adminOptional.get();
        if (!passwordService.matches(rawPassword, admin.getPassword())) {
            return Optional.empty();
        }

        if (passwordService.needsMigration(admin.getPassword())) {
            admin.setPassword(passwordService.encode(rawPassword));
            adminRepository.save(admin);
        }

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("id", admin.getId());
        userPayload.put("name", admin.getName());
        userPayload.put("email", admin.getEmail());
        userPayload.put("role", "admin");
        userPayload.put("adminId", admin.getId()); // Admin is their own tenant

        String token = jwtService.generateToken(admin.getEmail(), "ADMIN", userPayload);
        return Optional.of(new AuthResponse(token, "admin", userPayload));
    }

    public Optional<AuthResponse> loginTeacher(String email, String rawPassword) {
        Optional<Teacher> teacherOptional = teacherRepository.findByEmail(email);
        if (teacherOptional.isEmpty()) {
            return Optional.empty();
        }

        Teacher teacher = teacherOptional.get();
        if (!passwordService.matches(rawPassword, teacher.getPassword())) {
            return Optional.empty();
        }

        if (passwordService.needsMigration(teacher.getPassword())) {
            teacher.setPassword(passwordService.encode(rawPassword));
            teacherRepository.save(teacher);
        }

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("id", teacher.getId());
        userPayload.put("name", teacher.getName());
        if (teacher.getDepartment() != null) {
            userPayload.put("department", teacher.getDepartment().getDepartmentName());
            userPayload.put("departmentId", teacher.getDepartment().getId());
        }
        userPayload.put("email", teacher.getEmail());
        if (teacher.getMobilenumber() != null)
            userPayload.put("mobilenumber", teacher.getMobilenumber());
        userPayload.put("role", "teacher");
        if (teacher.getAdmin() != null) {
            userPayload.put("adminId", teacher.getAdmin().getId());
        }

        String token = jwtService.generateToken(teacher.getEmail(), "TEACHER", userPayload);
        return Optional.of(new AuthResponse(token, "teacher", userPayload));
    }

    public Optional<AuthResponse> loginUser(String email, String rawPassword) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();
        if (!passwordService.matches(rawPassword, user.getPassword())) {
            return Optional.empty();
        }

        if (passwordService.needsMigration(user.getPassword())) {
            user.setPassword(passwordService.encode(rawPassword));
            userRepository.save(user);
        }

        Map<String, Object> userPayload = new HashMap<>();
        userPayload.put("id", user.getId());
        userPayload.put("name", user.getName());
        if (user.getRollNo() != null)
            userPayload.put("rollNo", user.getRollNo());
        if (user.getClassMaster() != null) {
            userPayload.put("className", user.getClassMaster().getClassName());
            userPayload.put("classId", user.getClassMaster().getId());
        }
        if (user.getDivisionMaster() != null) {
            userPayload.put("divisionName", user.getDivisionMaster().getDivisionName());
            userPayload.put("divisionId", user.getDivisionMaster().getId());
        }
        if (user.getMobilenumber() != null)
            userPayload.put("mobilenumber", user.getMobilenumber());
        if (user.getAddress() != null)
            userPayload.put("address", user.getAddress());
        userPayload.put("email", user.getEmail());
        userPayload.put("role", "user");
        if (user.getAdmin() != null) {
            userPayload.put("adminId", user.getAdmin().getId());
        }

        String token = jwtService.generateToken(user.getEmail(), "USER", userPayload);
        return Optional.of(new AuthResponse(token, "user", userPayload));
    }
}

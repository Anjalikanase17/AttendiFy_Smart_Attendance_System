package com.example.attendance_Backend.service;

import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordService passwordService;
    @Autowired
    private com.example.attendance_Backend.repository.AdminRepository adminRepository;

    public User registerUser(User user) {
        Long adminId = AdminContextHolder.getAdminId();

        if (adminId != null) {
            Admin admin = new Admin();
            admin.setId(adminId);
            user.setAdmin(admin);
        } else if (user.getSchoolCode() != null && !user.getSchoolCode().isBlank()) {
            Admin admin = adminRepository.findBySchoolCode(user.getSchoolCode())
                    .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid School Code"));
            user.setAdmin(admin);
        } else if (user.getAdmin() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Admin context or School Code required for student registration");
        }

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Email already registered");
        }
        user.setPassword(passwordService.encode(user.getPassword()));
        return userRepository.save(user);
    }

    public Optional<User> loginUser(String email, String password) {
        return userRepository.findByEmail(email)
                .filter(user -> passwordService.matches(password, user.getPassword()));
    }

    public User updateClassAndDivision(int userId, com.example.attendance_Backend.model.ClassMaster classMaster,
            com.example.attendance_Backend.model.DivisionMaster divisionMaster) {

        Long adminId = AdminContextHolder.getAdminId();
        User user = null;

        if (adminId != null) {
            user = userRepository.findByIdAndAdminId(userId, adminId)
                    .orElseThrow(() -> new RuntimeException("User not found or unauthorized"));
        } else {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }

        user.setClassMaster(classMaster);
        user.setDivisionMaster(divisionMaster);
        return userRepository.save(user);
    }

    public void resetPassword(String email, String mobile, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with this email"));

        if (user.getMobilenumber() == null || !user.getMobilenumber().equals(mobile)) {
            throw new RuntimeException("Mobile number does not match our records");
        }

        user.setPassword(passwordService.encode(newPassword));
        userRepository.save(user);
    }
}

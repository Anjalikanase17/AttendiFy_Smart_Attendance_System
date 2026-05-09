package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.AuthRequest;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.service.AuthService;
import com.example.attendance_Backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        User savedUser = userService.registerUser(user);
        savedUser.setPassword(null);
        return savedUser;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        return authService.loginUser(request.getEmail(), request.getPassword())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid email or password")));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String mobile = body.get("mobile");
            String newPassword = body.get("password");

            if (email == null || mobile == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "All fields are required"));
            }

            userService.resetPassword(email, mobile, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/class-division")
    public ResponseEntity<?> updateClassAndDivision(@PathVariable int id, @RequestBody User updateData) {
        try {
            User updatedUser = userService.updateClassAndDivision(id, updateData.getClassMaster(),
                    updateData.getDivisionMaster());
            return ResponseEntity.ok(updatedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}

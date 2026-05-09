package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.AuthRequest;
import com.example.attendance_Backend.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.service.TeacherService;

@RestController
@RequestMapping("/api/teachers")
@CrossOrigin(origins = "*") // allow CORS for frontend
public class TeacherController {

    private final TeacherService service;
    private final AuthService authService;

    public TeacherController(TeacherService service, AuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerTeacher(@RequestBody Teacher teacher) {

        // Check if email already exists
        if (service.emailExists(teacher.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(new ApiResponse(false, "Email already registered"));
        }

        // enforce default role for normal signups
        teacher.setRole("TEACHER");

        service.registerTeacher(teacher);

        return ResponseEntity.ok(new ApiResponse(true, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginTeacher(@RequestBody AuthRequest request) {
        return authService.loginTeacher(request.getEmail(), request.getPassword())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(401).body("Invalid email or password"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody java.util.Map<String, String> body) {
        try {
            String email = body.get("email");
            String mobile = body.get("mobile");
            String newPassword = body.get("password");

            if (email == null || mobile == null || newPassword == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "All fields are required"));
            }

            service.resetPassword(email, mobile, newPassword);
            return ResponseEntity.ok(new ApiResponse(true, "Password reset successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/{email:.+}")
    public ResponseEntity<?> getTeacherByEmail(@PathVariable String email) {
        return service.getTeacherByEmail(email)
                .map(teacher -> {
                    teacher.setPassword(null);
                    return ResponseEntity.ok().body((Object) teacher);
                })
                .orElseGet(() -> ResponseEntity.status(404).body("Teacher not found"));
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}")
    public ResponseEntity<?> updateTeacher(
            @PathVariable Integer id,
            @RequestBody Teacher teacherDetails) {
        try {
            Teacher updated = service.updateTeacher(id, teacherDetails);
            updated.setPassword(null);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/department")
    public ResponseEntity<?> updateTeacherDepartment(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, Integer> body) {
        try {
            Integer departmentId = body.get("departmentId");
            Teacher updated = service.updateTeacherDepartment(id, departmentId);
            updated.setPassword(null);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    public static class ApiResponse {
        private boolean success;
        private String message;

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Teacher opens dashboard → browser saves its fingerprint to the DB.
     * PUT /api/teachers/{id}/device-id
     * Body: { "deviceId": "abc123fingerprint" }
     */
    @org.springframework.web.bind.annotation.PutMapping("/{id}/password")
    public ResponseEntity<?> updatePassword(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            String currentPassword = body.get("currentPassword");
            String newPassword = body.get("newPassword");

            if (currentPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Passwords required"));
            }

            service.updatePassword(id, currentPassword, newPassword);
            return ResponseEntity.ok(new ApiResponse(true, "Password updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.PutMapping("/{id}/device-id")
    public ResponseEntity<String> saveDeviceId(
            @PathVariable Integer id,
            @RequestBody java.util.Map<String, String> body) {
        String deviceId = body.get("deviceId");
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body("deviceId is required");
        }
        service.saveDeviceId(id, deviceId);
        return ResponseEntity.ok("Device ID saved");
    }

    @PostMapping(value = "/{id}/photo", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadPhoto(
            @PathVariable Integer id,
            @RequestParam("photo") MultipartFile photo) {
        try {
            if (photo == null || photo.isEmpty()) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "No file selected"));
            }

            String contentType = photo.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Only image files are allowed"));
            }

            String uploadDir = System.getProperty("user.dir") + File.separator + "uploads";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String ext = "";
            String originalName = photo.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            String uniqueFileName = "teacher_" + id + "_" + UUID.randomUUID() + ext;
            File dest = new File(uploadDir + File.separator + uniqueFileName);
            photo.transferTo(dest);

            String photoUrl = "/uploads/" + uniqueFileName;
            service.updatePhotoUrl(id, photoUrl);

            return ResponseEntity.ok(java.util.Map.of("photoUrl", photoUrl));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ApiResponse(false, "Photo upload failed: " + e.getMessage()));
        }
    }

}

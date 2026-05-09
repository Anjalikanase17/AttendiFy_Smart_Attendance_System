package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.model.Notice;
import com.example.attendance_Backend.repository.NoticeRepository;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notices")
@CrossOrigin(origins = "*")
public class NoticeController {

    @Autowired
    private NoticeRepository noticeRepository;

    private static final String UPLOAD_DIR = "uploads";

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadNotice(
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "noticeFile", required = false) MultipartFile noticeFile) {

        try {
            Long adminId = AdminContextHolder.getAdminId();
            if (adminId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            Notice notice = new Notice();
            notice.setTitle(title);
            notice.setContent(content);
            notice.setCreatedAt(LocalDateTime.now());

            Admin admin = new Admin();
            admin.setId(adminId);
            notice.setAdmin(admin);

            MultipartFile attachment = (file != null && !file.isEmpty()) ? file : noticeFile;
            if (attachment != null && !attachment.isEmpty()) {
                Path uploadDir = Paths.get(System.getProperty("user.dir"), UPLOAD_DIR);
                Files.createDirectories(uploadDir);

                String originalName = StringUtils.cleanPath(
                        attachment.getOriginalFilename() == null ? "attachment" : attachment.getOriginalFilename());
                String safeFileName = Paths.get(originalName).getFileName().toString();
                if (safeFileName.isBlank()) {
                    safeFileName = "attachment";
                }

                String uniqueFileName = UUID.randomUUID() + "_" + safeFileName;
                Path filePath = uploadDir.resolve(uniqueFileName).normalize();

                Files.copy(attachment.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                notice.setFileName(safeFileName);
                notice.setFilePath(filePath.toString());
                notice.setFileUrl("/uploads/" + uniqueFileName);
            }

            noticeRepository.save(notice);
            return ResponseEntity.ok("Notice posted successfully");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to post notice: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    public List<Notice> getAllNotices() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null) {
            return Collections.emptyList();
        }
        return noticeRepository.findByAdminIdOrderByCreatedAtDesc(adminId);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteNotice(@PathVariable Long id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }

        return noticeRepository.findById(id)
                .map(notice -> {
                    if (notice.getAdmin() == null || !notice.getAdmin().getId().equals(adminId)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized to delete this notice");
                    }
                    if (notice.getFilePath() != null) {
                        File file = new File(notice.getFilePath());
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    noticeRepository.deleteById(id);
                    return ResponseEntity.ok("Notice deleted successfully");
                })
                .orElse(ResponseEntity.badRequest().body("Notice not found"));
    }
}

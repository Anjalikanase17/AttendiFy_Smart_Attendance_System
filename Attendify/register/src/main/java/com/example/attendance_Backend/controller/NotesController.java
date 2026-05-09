package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.model.Notes;
import com.example.attendance_Backend.repository.NotesRepository;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.repository.DepartmentRepository;
import com.example.attendance_Backend.repository.ClassMasterRepository;
import com.example.attendance_Backend.repository.DivisionMasterRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.attendance_Backend.security.AdminContextHolder;
import com.example.attendance_Backend.model.Admin;
import org.springframework.http.HttpStatus;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Collections;

@RestController
@RequestMapping("/api/notes")
@CrossOrigin(origins = "*")
public class NotesController {

    @Autowired
    private NotesRepository notesRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ClassMasterRepository classMasterRepository;

    @Autowired
    private DivisionMasterRepository divisionMasterRepository;

    @Autowired
    private UserRepository userRepository;

    // Folder where files will be stored
    private static final String UPLOAD_DIR = "uploads/";

    // ✅ 1️⃣ Upload Notes (Teacher)
    @PostMapping("/upload")
    public ResponseEntity<String> uploadNotes(
            @RequestParam("file") MultipartFile file,
            @RequestParam("subject") String subject,
            @RequestParam(value = "departmentId", required = false) Integer departmentId,
            @RequestParam(value = "classId", required = false) Integer classId,
            @RequestParam(value = "divisionId", required = false) Integer divisionId) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Please select a file");
            }

            Long adminId = AdminContextHolder.getAdminId();
            if (adminId == null)
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

            String uploadDir = System.getProperty("user.dir") + File.separator + UPLOAD_DIR;
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String filePath = uploadDir + uniqueFileName;
            file.transferTo(new File(filePath));

            Notes notes = new Notes();
            notes.setSubject(subject);
            notes.setFileName(file.getOriginalFilename());
            notes.setFilePath(filePath);
            notes.setFileUrl("/uploads/" + uniqueFileName); // Relative URL for frontend
            notes.setUploadTime(LocalDateTime.now());

            if (departmentId != null) {
                notes.setDepartment(departmentRepository.findById(departmentId).orElse(null));
            }
            if (classId != null) {
                notes.setClassMaster(classMasterRepository.findById(classId).orElse(null));
            }
            if (divisionId != null) {
                notes.setDivisionMaster(divisionMasterRepository.findById(divisionId).orElse(null));
            }

            Admin admin = new Admin();
            admin.setId(adminId);
            notes.setAdmin(admin);

            notesRepository.save(notes);
            return ResponseEntity.ok("Notes uploaded successfully ✅");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("File upload failed ❌");
        }
    }

    // ✅ 2️⃣ Get All Notes (Admin View)
    @GetMapping("/all")
    public List<Notes> getAllNotes() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();
        return notesRepository.findByAdminId(adminId);
    }

    // ✅ 3️⃣ Get Filtered Notes (Student View)
    @GetMapping("/student/{studentId}")
    public List<Notes> getStudentNotes(@PathVariable int studentId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();

        return userRepository.findByIdAndAdminId(studentId, adminId).map(user -> {
            Integer deptId = (user.getClassMaster() != null && user.getClassMaster().getDepartment() != null)
                    ? user.getClassMaster().getDepartment().getId()
                    : null;
            Integer classId = user.getClassMaster() != null ? user.getClassMaster().getId() : null;
            Integer divId = user.getDivisionMaster() != null ? user.getDivisionMaster().getId() : null;

            if (deptId != null && classId != null && divId != null) {
                return notesRepository.findByDepartment_IdAndClassMaster_IdAndDivisionMaster_IdAndAdminId(deptId,
                        classId, divId, adminId);
            }
            return notesRepository.findByAdminId(adminId); // Fallback to all notes for this admin
        }).orElse(Collections.emptyList());
    }

    // ✅ 4️⃣ Delete Notes (Optional - Teacher)
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteNotes(@PathVariable Long id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");

        return notesRepository.findById(id)
                .map(note -> {
                    if (note.getAdmin() == null || !note.getAdmin().getId().equals(adminId)) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized to delete this note");
                    }
                    // Delete file from folder
                    File file = new File(note.getFilePath());
                    if (file.exists()) {
                        file.delete();
                    }
                    notesRepository.deleteById(id);
                    return ResponseEntity.ok("Notes deleted successfully ✅");
                })
                .orElse(ResponseEntity.badRequest().body("Notes not found ❌"));
    }
}
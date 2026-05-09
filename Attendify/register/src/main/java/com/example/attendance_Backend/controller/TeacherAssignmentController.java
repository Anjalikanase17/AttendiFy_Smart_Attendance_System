package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.TeacherAssignmentDTO;
import com.example.attendance_Backend.service.TeacherAssignmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/assignments")
@CrossOrigin(origins = "*")
public class TeacherAssignmentController {

    private final TeacherAssignmentService assignmentService;

    public TeacherAssignmentController(TeacherAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    /**
     * Admin creates a teacher assignment
     * POST /api/admin/assignments
     * Body: { "teacherId": 1, "subject": "DSA", "className": "CSE", "division":
     * "A", "roomNumber": "101" }
     */
    @PostMapping
    public ResponseEntity<TeacherAssignmentDTO> createAssignment(@RequestBody Map<String, Object> body) {
        int teacherId = (int) body.get("teacherId");
        String subject = (String) body.get("subject");
        String className = (String) body.get("className");
        String division = (String) body.getOrDefault("division", null);
        String roomNumber = (String) body.getOrDefault("roomNumber", null);

        TeacherAssignmentDTO dto = assignmentService.createAssignment(
                teacherId, subject, className, division, roomNumber);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get all assignments for a teacher
     * GET /api/admin/assignments/teacher/{teacherId}
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TeacherAssignmentDTO>> getAssignments(@PathVariable int teacherId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsForTeacher(teacherId));
    }

    /**
     * Delete an assignment
     * DELETE /api/admin/assignments/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAssignment(@PathVariable int id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.ok("Assignment deleted");
    }
}

package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.model.TeacherTimetable;
import com.example.attendance_Backend.model.TimetableStructure;
import com.example.attendance_Backend.service.TeacherTimetableService;
import com.example.attendance_Backend.service.TimetableStructureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Teacher manages their weekly timetable.
 */
@RestController
@RequestMapping("/api/teacher/timetable")
@CrossOrigin(origins = "*")
public class TeacherTimetableController {

    private final TeacherTimetableService service;
    private final TimetableStructureService structureService;

    public TeacherTimetableController(TeacherTimetableService service,
            TimetableStructureService structureService) {
        this.service = service;
        this.structureService = structureService;
    }

    /**
     * GET timetable structure (period/break rows — admin defined).
     * Accessible by teachers so they can build their grid.
     * GET /api/teacher/timetable/structure
     */
    @GetMapping("/structure")
    public ResponseEntity<List<TimetableStructure>> getStructure() {
        return ResponseEntity.ok(structureService.getAll());
    }

    /**
     * GET full week grid for a teacher.
     * GET /api/teacher/timetable/{teacherId}
     */
    @GetMapping("/{teacherId}")
    public ResponseEntity<List<TeacherTimetable>> getFullTimetable(
            @PathVariable int teacherId) {
        return ResponseEntity.ok(service.getFullTimetable(teacherId));
    }

    /**
     * GET today's LECTURE slots (for QR generator dropdown).
     * GET /api/teacher/timetable/{teacherId}/today
     */
    @GetMapping("/{teacherId}/today")
    public ResponseEntity<List<TeacherTimetable>> getTodayLectures(
            @PathVariable int teacherId) {
        return ResponseEntity.ok(service.getTodayLectures(teacherId));
    }

    /**
     * PUT — save/update a single timetable cell.
     * PUT /api/teacher/timetable/{teacherId}/slot/{slotId}/{day}
     * Body: { "className": "MCA I", "division": "A", "subject": "Java", "roomNo":
     * "201" }
     */
    @PutMapping("/{teacherId}/slot/{slotId}/{day}")
    public ResponseEntity<TeacherTimetable> saveSlot(
            @PathVariable int teacherId,
            @PathVariable int slotId,
            @PathVariable String day,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(service.saveSlot(teacherId, slotId, day, body));
    }
}

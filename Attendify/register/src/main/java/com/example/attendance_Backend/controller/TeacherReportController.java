package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.*;
import com.example.attendance_Backend.service.TeacherReportService;
import com.example.attendance_Backend.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance/reports")
@CrossOrigin(origins = "*")
public class TeacherReportController {

    private static final Logger logger = LoggerFactory.getLogger(TeacherReportController.class);

    private final TeacherReportService reportService;
    private final EmailService emailService;

    public TeacherReportController(TeacherReportService reportService, EmailService emailService) {
        this.reportService = reportService;
        this.emailService = emailService;
    }

    @GetMapping("/subjects")
    public ResponseEntity<List<TeacherSubjectReportDTO>> getSubjectReport(@RequestParam int teacherId) {
        return ResponseEntity.ok(reportService.getSubjectReport(teacherId));
    }

    @GetMapping("/classes")
    public ResponseEntity<List<ClassAttendanceDTO>> getClassReport(@RequestParam int teacherId) {
        return ResponseEntity.ok(reportService.getClassReport(teacherId));
    }

    @GetMapping("/consolidated")
    public ResponseEntity<ConsolidatedAttendanceDTO> getConsolidatedReport(@RequestParam int classId) {
        logger.info("Fetching consolidated report for classId: {}", classId);
        return ResponseEntity.ok(reportService.getConsolidatedClassReport(classId));
    }

    @PostMapping("/email")
    public ResponseEntity<String> sendReportToEmail(@RequestParam int classId, @RequestParam String email) {
        logger.info("Sending report for classId: {} to {}", classId, email);
        try {
            ConsolidatedAttendanceDTO report = reportService.getConsolidatedClassReport(classId);

            StringBuilder body = new StringBuilder("Consolidated Attendance Report\n\n");
            body.append(String.format("%-20s %-15s", "Student Name", "Roll No"));
            for (String sub : report.getSubjects()) {
                body.append(String.format(" %-15s", sub));
            }
            body.append(" Overall%\n");
            body.append("-".repeat(50 + report.getSubjects().size() * 16)).append("\n");

            for (StudentConsolidatedDTO s : report.getStudents()) {
                body.append(String.format("%-20s %-15s", s.getName(), s.getRollNo()));
                for (String sub : report.getSubjects()) {
                    Double pct = s.getSubjectPercentages().getOrDefault(sub, 0.0);
                    body.append(String.format(" %-15.1f%%", pct));
                }
                body.append(String.format(" %-10.1f%%\n", s.getOverallPercentage()));
            }

            emailService.sendEmail(email, "Consolidated Attendance Report", body.toString());
            return ResponseEntity.ok("Report sent successfully to " + email);
        } catch (Exception e) {
            logger.error("Email error: ", e);
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/students")
    public ResponseEntity<List<Map<String, Object>>> getStudents(
            @RequestParam int teacherId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer divisionId) {
        return ResponseEntity.ok(reportService.getStudentsForTeacher(teacherId, classId, divisionId));
    }

    @GetMapping("/student/{studentId}/summary")
    public ResponseEntity<List<StudentSubjectSummaryDTO>> getStudentSummary(
            @PathVariable int studentId,
            @RequestParam(required = false) Integer classId) {
        return ResponseEntity.ok(reportService.getStudentSubjectSummary(studentId, classId));
    }

    @GetMapping("/student/{studentId}/records")
    public ResponseEntity<List<StudentDateRecordDTO>> getStudentRecords(
            @PathVariable int studentId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer classId) {
        return ResponseEntity.ok(reportService.getStudentDateRecords(studentId, subject, from, to, classId));
    }

    @GetMapping("/student/{studentId}/monthly")
    public ResponseEntity<List<MonthlyAttendanceDTO>> getStudentMonthly(@PathVariable int studentId) {
        return ResponseEntity.ok(reportService.getMonthlyAttendance(studentId));
    }
}

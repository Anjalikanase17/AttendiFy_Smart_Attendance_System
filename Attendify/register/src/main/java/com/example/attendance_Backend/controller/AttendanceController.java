package com.example.attendance_Backend.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.example.attendance_Backend.dto.AttendanceDTO;
import com.example.attendance_Backend.dto.DateAnalyticsDTO;
import com.example.attendance_Backend.dto.StudentAttendanceDTO;
import com.example.attendance_Backend.dto.SubjectAnalyticsDTO;
import com.example.attendance_Backend.model.Attendance;
import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.model.AttendanceSession;
import com.example.attendance_Backend.repository.AttendanceRepository;
import com.example.attendance_Backend.repository.AttendanceSessionRepository;
import com.example.attendance_Backend.repository.SubjectMasterRepository;
import com.example.attendance_Backend.repository.UserRepository;
import com.example.attendance_Backend.service.TeacherAttendanceService;
import com.example.attendance_Backend.service.TeacherService;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private TeacherService teacherService;

    @Autowired
    private AttendanceSessionRepository sessionRepository;

    @Autowired
    private SubjectMasterRepository subjectMasterRepository;

    private final TeacherAttendanceService attendanceService;

    public AttendanceController(TeacherAttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    // 1️⃣ STUDENT MARKS ATTENDANCE
    @PostMapping("/mark")
    public Map<String, String> markAttendance(
            @RequestParam String rollNo,
            @RequestParam String deviceId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam String sessionId) {
        Map<String, String> response = new HashMap<>();

        if (sessionId == null || sessionId.isEmpty()) {
            response.put("message", "Session ID required ❌");
            return response;
        }

        AttendanceSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) {
            response.put("message", "Invalid QR session ❌");
            return response;
        }

        Admin admin = session.getAdmin();
        if (admin == null) {
            response.put("message", "Session not associated with any admin ❌");
            return response;
        }
        Long adminId = admin.getId();

        User user = userRepository.findByRollNoAndAdminId(rollNo, adminId).orElse(null);
        if (user == null) {
            response.put("message", "Invalid Roll Number for this organization");
            return response;
        }

        // ⏳ check expiry
        if (session.getExpiryTime().isBefore(LocalDateTime.now())) {
            response.put("message", "QR expired ❌");
            return response;
        }
        // geofence check
        double teacherLat = session.getTeacherLat();
        double teacherLng = session.getTeacherLng();
        double distance = calculateDistance(teacherLat, teacherLng, latitude, longitude);
        if (distance > session.getRadiusKm()) { // default 0.1 km
            response.put("message", "You are outside classroom range ❌");
            return response;
        }
        LocalDate today = LocalDate.now();
        // ✅ Check if there's a pre-generated row (or existing row)
        Attendance attendance = attendanceRepository
                .findByUser_IdAndSessionIdAndAdminId(user.getId(), sessionId, adminId).orElse(null);

        if (attendance != null && "Present".equalsIgnoreCase(attendance.getStatus())) {
            response.put("message", "Attendance already marked for this session");
            return response;
        }

        // 🔒 Block same device for THIS specific session (proxy prevention)
        boolean deviceUsed = attendanceRepository.existsByDeviceIdAndSessionIdAndAdminId(deviceId, sessionId, adminId);
        if (deviceUsed) {
            response.put("message", "Attendance already marked from this device ❌");
            return response;
        }

        if (attendance == null) {
            // Unmapped student scanning the QR code
            attendance = new Attendance();
            attendance.setUser(user);
            attendance.setSubjectMaster(session.getSubjectMaster());
            attendance.setDate(today);
            attendance.setSessionId(sessionId);
            attendance.setAdmin(admin);
            attendance.setClassMaster(session.getClassMaster());
            attendance.setDivisionMaster(session.getDivisionMaster());
        }

        attendance.setStatus("Present");
        attendance.setDeviceId(deviceId);

        attendanceRepository.save(attendance);
        response.put("message", "Attendance marked successfully ✅");
        return response;
    }

    // helper for geofence
    private double calculateDistance(double lat1, double lon1,
            double lat2, double lon2) {
        final int R = 6371; // Earth radius in KM
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1))
                        * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2)
                        * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // returns distance in KM
    }

    // 2️⃣ TEACHER FINALIZE (MARK ABSENT)
    @PostMapping("/finalize")
    public String finalizeAttendance(
            @RequestParam String subject,
            @RequestParam String className,
            @RequestParam(required = false) String divisionId) {
        LocalDate today = LocalDate.now();

        Integer cId = null;
        try {
            cId = Integer.parseInt(className);
        } catch (Exception e) {
        }

        Integer sId = null;
        try {
            sId = Integer.parseInt(subject);
        } catch (Exception e) {
        }

        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null)
            return "Admin context required";
        Admin admin = new Admin();
        admin.setId(adminId);

        Integer dId = null;
        try {
            if (divisionId != null && !divisionId.isEmpty() && !divisionId.equals("undefined")) {
                dId = Integer.parseInt(divisionId);
            }
        } catch (Exception e) {
        }

        if (cId == null || sId == null)
            return "Invalid IDs";

        List<User> students;
        if (dId != null) {
            students = userRepository.findByClassMaster_IdAndDivisionMaster_IdAndAdminId(cId, dId, adminId);
        } else {
            students = userRepository.findByClassMaster_IdAndAdminId(cId, adminId);
        }

        for (User user : students) {

            boolean alreadyMarked = attendanceRepository.existsByUser_IdAndDateAndSubjectMaster_IdAndAdminId(
                    user.getId(), today, sId, adminId);

            if (!alreadyMarked) {
                Attendance attendance = new Attendance();
                attendance.setUser(user);
                attendance.setSubjectMaster(subjectMasterRepository.findById(sId).orElse(null));
                attendance.setDate(today);
                attendance.setStatus("Absent");
                attendance.setAdmin(admin);

                // Fix: Set class and division from student profile for reporting
                attendance.setClassMaster(user.getClassMaster());
                attendance.setDivisionMaster(user.getDivisionMaster());

                attendanceRepository.save(attendance);
            }
        }
        return "Attendance finalized successfully";
    }

    // TEACHER VIEW
    @GetMapping("/teacher/students")
    public List<User> getTeacherStudents(
            @RequestParam int teacherId,
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer divisionId) {
        return teacherService.getStudentsForTeacher(teacherId, classId, divisionId);
    }

    @GetMapping("/teacher/student-list")
    public List<StudentAttendanceDTO> studentTab(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer divisionId,
            @RequestParam(required = false) Integer subjectId) {
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();

        List<Object[]> rows;
        if (classId == null && divisionId == null && subjectId == null) {
            rows = attendanceRepository.getStudentTabDataNative(adminId);
        } else {
            rows = attendanceRepository.getFilteredStudentTabDataNative(classId, divisionId, subjectId, adminId);
        }

        // Map Object[] rows -> StudentAttendanceDTO
        List<StudentAttendanceDTO> result = new ArrayList<>();
        for (Object[] row : rows) {
            int id = row[0] instanceof Number ? ((Number) row[0]).intValue() : 0;
            String rollNo = row[1] != null ? row[1].toString() : "";
            String name = row[2] != null ? row[2].toString() : "";
            String className = row[3] != null ? row[3].toString() : "-";
            String subject = row[4] != null ? row[4].toString() : "-";
            String status = row[5] != null ? row[5].toString() : "Absent";
            result.add(new StudentAttendanceDTO(id, rollNo, name, className, subject, status));
        }
        return result;
    }

    @GetMapping("/report")
    public List<AttendanceDTO> generateReport(
            @RequestParam String className) {
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();
        return attendanceRepository.attendanceReportByClass(className, adminId);
    }

    @GetMapping("/analytics/subject")
    public List<SubjectAnalyticsDTO> getSubjectAnalytics(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer divisionId,
            @RequestParam(required = false) Integer subjectId) {
        return teacherService.getSubjectAnalytics(classId, divisionId, subjectId);
    }

    @GetMapping("/analytics/department")
    public List<SubjectAnalyticsDTO> departmentAnalytics(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer divisionId,
            @RequestParam(required = false) Integer subjectId) {
        return teacherService.getDepartmentAnalytics(classId, divisionId, subjectId);
    }

    @GetMapping("/analytics/date")
    public List<DateAnalyticsDTO> dateAnalytics(
            @RequestParam(required = false) Integer classId,
            @RequestParam(required = false) Integer divisionId,
            @RequestParam(required = false) Integer subjectId) {
        return teacherService.getDateAnalytics(classId, divisionId, subjectId);
    }

    @GetMapping("/check/{id}")
    public String checkAttendance(@PathVariable int id) {
        return attendanceService.checkAttendanceAndNotify(id);
    }

    @PostMapping("/manual")
    public Map<String, String> markManualAttendance(
            @RequestParam String rollNo,
            @RequestParam String subject,
            @RequestParam String status) {

        Map<String, String> response = new HashMap<>();

        Optional<User> studentOptional = userRepository.findByRollNo(rollNo);

        if (studentOptional.isEmpty()) {
            response.put("message", "Student not found ❌");
            return response;
        }

        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null) {
            response.put("message", "Admin context required ❌");
            return response;
        }
        Admin admin = new Admin();
        admin.setId(adminId);

        User student = studentOptional.get();
        if (!adminId.equals(student.getAdmin() != null ? student.getAdmin().getId() : null)) {
            response.put("message", "Student does not belong to your organization ❌");
            return response;
        }

        LocalDate today = LocalDate.now();

        Integer subjectId = null;
        try {
            subjectId = Integer.parseInt(subject);
        } catch (Exception e) {
        }
        if (subjectId == null) {
            response.put("message", "Invalid subject ID");
            return response;
        }

        // Check if already marked
        boolean alreadyMarked = attendanceRepository.existsByUser_IdAndDateAndSubjectMaster_IdAndAdminId(
                student.getId(), today, subjectId, adminId);

        if (alreadyMarked) {
            response.put("message", "Attendance already marked for today ❌");
            return response;
        }

        Attendance attendance = new Attendance();
        attendance.setUser(student);
        attendance.setSubjectMaster(subjectMasterRepository.findById(subjectId).orElse(null));
        attendance.setDate(today);
        attendance.setStatus(status); // Present or Absent
        attendance.setDeviceId("MANUAL"); // mark as manual entry
        attendance.setAdmin(admin);

        // Fix: Set class and division from student profile for reporting
        attendance.setClassMaster(student.getClassMaster());
        attendance.setDivisionMaster(student.getDivisionMaster());

        attendanceRepository.save(attendance);

        response.put("message", "Manual attendance saved successfully ✅");
        return response;
    }

    static class BulkAttendanceRequest {
        public Integer timetableSlotId;
        public Integer classId;
        public Integer divisionId;
        public Integer subjectId;
        public Integer teacherId;
        public List<StudentStatus> attendance;
    }

    static class StudentStatus {
        public String rollNo;
        public String status;
    }

    @GetMapping("/session/{timetableSlotId}/present-rolls")
    public ResponseEntity<?> getSessionPresentRolls(@PathVariable Integer timetableSlotId) {
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null) {
            return ResponseEntity.status(401).body(Collections.singletonList("Admin context required"));
        }

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<AttendanceSession> existingSessions = sessionRepository
                .findByTimetableSlotIdAndExpiryTimeBetweenAndAdminId(
                        timetableSlotId, adminId, startOfDay, endOfDay);

        if (existingSessions.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        String sessionId = existingSessions.get(0).getId();
        List<String> presentRollNos = attendanceRepository.findPresentRollNosBySessionIdAndAdminId(sessionId, adminId);
        return ResponseEntity.ok(presentRollNos);
    }

    @PostMapping("/manual/bulk")
    public Map<String, String> markManualAttendanceBulk(@RequestBody BulkAttendanceRequest req) {
        Map<String, String> response = new HashMap<>();

        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null) {
            response.put("message", "Admin context required ❌");
            return response;
        }
        Admin admin = new Admin();
        admin.setId(adminId);

        if (req.timetableSlotId == null || req.subjectId == null || req.classId == null || req.attendance == null) {
            response.put("message", "Missing required fields ❌");
            return response;
        }

        LocalDate today = LocalDate.now();

        // 1. Find existing session for this slot today, or create a new minimal one
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<AttendanceSession> existingSessions = sessionRepository
                .findByTimetableSlotIdAndExpiryTimeBetweenAndAdminId(
                        req.timetableSlotId, adminId, startOfDay, endOfDay);

        AttendanceSession session;
        if (!existingSessions.isEmpty()) {
            session = existingSessions.get(0);
        } else {
            session = new AttendanceSession();
            session.setAdmin(admin);
            session.setTimetableSlotId(req.timetableSlotId);
            session.setSubjectMaster(subjectMasterRepository.findByIdAndAdminId(req.subjectId, adminId).orElse(null));
            // Class and Division Master
            // Note: need ClassMasterRepository and DivisionMasterRepository injected in
            // AttendanceController OR we can omit them from session if not strictly
            // required, but let's try to get them. We don't have those repos in
            // AttendanceController but we can just skip setting them on the new Session
            // object since they are optional for basic manual records, or we get them from
            // the first student's profile.
            session.setTeacherId(req.teacherId);
            session.setTeacherLat(0.0);
            session.setTeacherLng(0.0);
            session.setRadiusKm(0.0);
            session.setExpiryTime(endOfDay); // expire end of day since it's manual
            sessionRepository.save(session);
        }

        int successCount = 0;

        for (StudentStatus ss : req.attendance) {
            Optional<User> studentOpt = userRepository.findByRollNoAndAdminId(ss.rollNo, adminId);
            if (studentOpt.isEmpty())
                continue;
            User student = studentOpt.get();

            Attendance attendance = attendanceRepository
                    .findByUser_IdAndSessionIdAndAdminId(student.getId(), session.getId(), adminId).orElse(null);

            if (attendance != null) {
                // If it exists, ALWAYS sync its status to the teacher's bulk grid selection
                attendance.setStatus(ss.status);
                attendance.setDeviceId("MANUAL");
                attendanceRepository.save(attendance);
                successCount++;
            } else {
                // If it doesn't exist (e.g. unassigned student), create the row natively
                attendance = new Attendance();
                attendance.setUser(student);
                attendance.setSubjectMaster(session.getSubjectMaster() != null ? session.getSubjectMaster()
                        : subjectMasterRepository.findByIdAndAdminId(req.subjectId, adminId).orElse(null));
                attendance.setDate(today);
                attendance.setStatus(ss.status);
                attendance.setDeviceId("MANUAL");
                attendance.setSessionId(session.getId());
                attendance.setAdmin(admin);
                attendance.setClassMaster(student.getClassMaster());
                attendance.setDivisionMaster(student.getDivisionMaster());
                attendanceRepository.save(attendance);
                successCount++;
            }
        }

        response.put("message", "Bulk attendance logged: " + successCount + " new entries ✅");
        return response;
    }

    // ==========================================
    // MONTHLY EXCEL REPORT ENDPOINT
    // ==========================================

    /**
     * Returns a structured monthly attendance report for Excel download.
     * Response: { subjects: [...], students: [{rollNo, name,
     * subjects:{s1:{present,total,pct}}, overall:{present,total,pct}}] }
     */
    @GetMapping("/monthly-report")
    public Map<String, Object> getMonthlyReport(
            @RequestParam int classId,
            @RequestParam int divisionId,
            @RequestParam int month,
            @RequestParam int year) {

        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyMap();

        List<Object[]> rows = attendanceRepository.getMonthlyReportRaw(classId, divisionId, month, year, adminId);

        // Pivot: studentId -> subject -> {present, total}
        LinkedHashMap<Integer, Map<String, Object>> studentMap = new LinkedHashMap<>();
        java.util.TreeSet<String> subjectSet = new java.util.TreeSet<>();

        for (Object[] row : rows) {
            int userId = ((Number) row[0]).intValue();
            String rollNo = (String) row[1];
            String name = (String) row[2];
            String subject = (String) row[3];
            long present = ((Number) row[4]).longValue();
            long total = ((Number) row[5]).longValue();

            subjectSet.add(subject);

            studentMap.computeIfAbsent(userId, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("rollNo", rollNo);
                m.put("name", name);
                m.put("subjects", new LinkedHashMap<String, Object>());
                return m;
            });

            @SuppressWarnings("unchecked")
            Map<String, Object> subjectData = (Map<String, Object>) studentMap.get(userId).get("subjects");
            Map<String, Object> subEntry = new LinkedHashMap<>();
            subEntry.put("present", present);
            subEntry.put("total", total);
            subEntry.put("pct", total == 0 ? 0 : Math.round(present * 100.0 / total));
            subjectData.put(subject, subEntry);
        }

        // Compute overall per student
        List<Map<String, Object>> studentList = new ArrayList<>();
        for (Map<String, Object> s : studentMap.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subs = (Map<String, Object>) s.get("subjects");
            long totalPresent = 0, totalTotal = 0;
            for (Object sv : subs.values()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> sv2 = (Map<String, Object>) sv;
                totalPresent += ((Number) sv2.get("present")).longValue();
                totalTotal += ((Number) sv2.get("total")).longValue();
            }
            Map<String, Object> overall = new LinkedHashMap<>();
            overall.put("present", totalPresent);
            overall.put("total", totalTotal);
            overall.put("pct", totalTotal == 0 ? 0 : Math.round(totalPresent * 100.0 / totalTotal));
            s.put("overall", overall);
            studentList.add(s);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("subjects", new ArrayList<>(subjectSet));
        result.put("students", studentList);
        return result;
    }

}

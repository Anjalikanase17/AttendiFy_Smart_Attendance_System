package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.repository.*;
import com.example.attendance_Backend.model.AttendanceSession;
import com.example.attendance_Backend.model.Attendance;
import com.example.attendance_Backend.model.User;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendance_Backend.repository.AttendanceSessionRepository;
import com.example.attendance_Backend.repository.SubjectMasterRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "*")
public class SessionController {

    @Autowired
    private AttendanceSessionRepository sessionRepository;
    @Autowired
    private SubjectMasterRepository subjectMasterRepository;
    @Autowired
    private ClassMasterRepository classMasterRepository;
    @Autowired
    private DivisionMasterRepository divisionMasterRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/create")
    @Transactional
    public Map<String, String> createSession(@RequestBody Map<String, Object> data) {

        AttendanceSession session = new AttendanceSession();

        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId != null) {
            com.example.attendance_Backend.model.Admin admin = new com.example.attendance_Backend.model.Admin();
            admin.setId(adminId);
            session.setAdmin(admin);
        }

        Integer subjectId = extractId(data, "subjectId", "subject");
        if (subjectId != null) {
            session.setSubjectMaster(subjectMasterRepository.findByIdAndAdminId(subjectId, adminId).orElse(null));
        }

        Integer classId = extractId(data, "classId", "class");
        if (classId != null) {
            session.setClassMaster(classMasterRepository.findByIdAndAdminId(classId, adminId).orElse(null));
        }

        Integer divisionId = extractId(data, "divisionId", "division");
        if (divisionId != null) {
            session.setDivisionMaster(divisionMasterRepository.findByIdAndAdminId(divisionId, adminId).orElse(null));
        }

        session.setTeacherLat(asDouble(data.get("teacherLat")));
        session.setTeacherLng(asDouble(data.get("teacherLng")));
        if (data.containsKey("radiusKm")) {
            session.setRadiusKm(asDouble(data.get("radiusKm")));
        }
        if (data.containsKey("teacherDeviceId")) {
            session.setTeacherDeviceId((String) data.get("teacherDeviceId"));
        }
        if (data.containsKey("teacherId") && data.get("teacherId") != null) {
            Object tidVal = data.get("teacherId");
            try {
                Integer tid = tidVal instanceof Integer ? (Integer) tidVal : Integer.parseInt(tidVal.toString());
                session.setTeacherId(tid);
            } catch (Exception ignored) {
            }
        }

        int duration = (Integer) data.get("duration");
        session.setExpiryTime(LocalDateTime.now().plusMinutes(duration));

        if (data.containsKey("timetableSlotId") && data.get("timetableSlotId") != null) {
            Integer slotId = (Integer) data.get("timetableSlotId");
            session.setTimetableSlotId(slotId);

            // DELETE ANY PREVIOUS SESSIONS CREATED TODAY FOR THIS SLOT
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            List<AttendanceSession> existingSessions = sessionRepository
                    .findByTimetableSlotIdAndExpiryTimeBetweenAndAdminId(
                            slotId, adminId, startOfDay, endOfDay);

            for (AttendanceSession es : existingSessions) {
                // Delete associated attendance records first
                attendanceRepository.deleteBySessionIdAndAdminId(es.getId(), adminId);
                // Delete the session itself
                sessionRepository.delete(es);
            }
        } else if (classId != null && divisionId != null && subjectId != null && session.getTeacherId() != null) {
            // IF NO SLOT ID, BUT WE HAVE LECTURE DETAILS, DELETE PREVIOUS SESSIONS FOR SAME
            // LECTURE TODAY
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            List<AttendanceSession> existingSessions = sessionRepository
                    .findByLectureDetails(
                            classId, divisionId, subjectId, session.getTeacherId(), adminId, startOfDay, endOfDay);

            for (AttendanceSession es : existingSessions) {
                // Delete associated attendance records first
                attendanceRepository.deleteBySessionIdAndAdminId(es.getId(), adminId);
                // Delete the session itself
                sessionRepository.delete(es);
            }
        }

        sessionRepository.save(session);

        // PRE-GENERATE ABSENT RECORDS
        if (classId != null) {
            List<User> students;
            if (divisionId != null) {
                students = userRepository.findByClassMaster_IdAndDivisionMaster_IdAndAdminId(classId, divisionId, adminId);
            } else {
                students = userRepository.findByClassMaster_IdAndAdminId(classId, adminId);
            }

            LocalDate today = LocalDate.now();
            List<Attendance> initialRecords = new ArrayList<>();
            for (User student : students) {
                // To be ultra safe, check if somehow one exists (though deleted above)
                if (!attendanceRepository.existsByUser_IdAndSessionIdAndAdminId(student.getId(), session.getId(), adminId)) {
                    Attendance a = new Attendance();
                    a.setUser(student);
                    a.setSubjectMaster(session.getSubjectMaster());
                    a.setClassMaster(session.getClassMaster());
                    a.setDivisionMaster(session.getDivisionMaster());
                    a.setDate(today);
                    a.setStatus("Absent"); 
                    a.setSessionId(session.getId());
                    a.setAdmin(session.getAdmin());
                    initialRecords.add(a);
                }
            }
            if (!initialRecords.isEmpty()) {
                attendanceRepository.saveAll(initialRecords);
            }
        }
        Map<String, String> response = new HashMap<>();
        response.put("sessionId", session.getId());
        return response;
    }

    private Integer extractId(Map<String, Object> data, String key1, String key2) {
        Object val = data.get(key1);
        if (val == null)
            val = data.get(key2);
        if (val == null)
            return null;

        if (val instanceof Integer)
            return (Integer) val;
        try {
            return Integer.parseInt(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private double asDouble(Object val) {
        if (val instanceof Double)
            return (Double) val;
        if (val instanceof Integer)
            return ((Integer) val).doubleValue();
        if (val instanceof String)
            return Double.parseDouble((String) val);
        return 0.0;
    }
}

package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.AttendanceDTO;
import com.example.attendance_Backend.model.Setting;
import com.example.attendance_Backend.repository.AttendanceRepository;
import com.example.attendance_Backend.repository.SettingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherAttendanceService {

    private final AttendanceRepository repository;
    private final AttendanceRepository attendanceRepository;
    private final SettingRepository settingRepository;

    public TeacherAttendanceService(AttendanceRepository repository, AttendanceRepository attendanceRepository,
            SettingRepository settingRepository) {
        this.repository = repository;
        this.attendanceRepository = attendanceRepository;
        this.settingRepository = settingRepository;
    }

    // Get attendance for a specific subject (teacher view)
    public List<AttendanceDTO> getAttendanceBySubject(String subject) {
        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();
        return repository.attendanceListForTeacher(subject, adminId);
    }

    public String checkAttendanceAndNotify(int id) {

        long totalClasses = attendanceRepository.countByUserId(id);
        long presentClasses = attendanceRepository.countByUserIdAndStatus(id, "PRESENT");

        if (totalClasses == 0) {
            return "No attendance records found.";
        }

        double percentage = (presentClasses * 100.0) / totalClasses;

        Long adminId = com.example.attendance_Backend.security.AdminContextHolder.getAdminId();
        Setting setting = null;
        if (adminId != null) {
            setting = settingRepository.findByAdminId(adminId).orElse(null);
        }

        if (setting == null) {
            // Fallback to default threshold if no settings found
            if (percentage < 75.0) {
                return "⚠ Warning: Your attendance is below 75.0%";
            }
            return "✅ Attendance is safe.";
        }

        if (percentage < setting.getAttendanceThreshold()) {
            return "⚠ Warning: Your attendance is below "
                    + setting.getAttendanceThreshold() + "%";
        } else {
            return "✅ Attendance is safe.";
        }
    }

}

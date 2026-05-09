package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.model.Setting;
import com.example.attendance_Backend.repository.SettingRepository;
import org.springframework.web.bind.annotation.*;

import com.example.attendance_Backend.security.AdminContextHolder;
import com.example.attendance_Backend.model.Admin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/settings")
@CrossOrigin
public class SettingController {

    private final SettingRepository settingRepository;

    public SettingController(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    // Save or Update Settings
    @PostMapping("/save")
    public ResponseEntity<Setting> saveSettings(@RequestBody Setting newSetting) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Setting existing = settingRepository.findByAdminId(adminId).orElse(new Setting());

        Admin admin = new Admin();
        admin.setId(adminId);
        existing.setAdmin(admin);

        existing.setAttendanceThreshold(newSetting.getAttendanceThreshold());
        existing.setLateArrivalMinutes(newSetting.getLateArrivalMinutes());
        existing.setAutoMarkAbsentMinutes(newSetting.getAutoMarkAbsentMinutes());
        existing.setManualOverride(newSetting.isManualOverride());
        existing.setSendAlerts(newSetting.isSendAlerts());

        if (existing.getId() == null) {
            // If it's a new record, we might need a unique ID strategy if ID isn't
            // auto-generated
            // But let's assume save handles it or we use adminId as ID if OneToOne
            existing.setId(adminId);
        }

        return ResponseEntity.ok(settingRepository.save(existing));
    }

    // Get Settings
    @GetMapping
    public ResponseEntity<Setting> getSettings() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(settingRepository.findByAdminId(adminId)
                .orElseGet(() -> {
                    Setting defaultSetting = new Setting();
                    defaultSetting.setId(adminId);
                    Admin admin = new Admin();
                    admin.setId(adminId);
                    defaultSetting.setAdmin(admin);
                    defaultSetting.setAttendanceThreshold(75);
                    defaultSetting.setLateArrivalMinutes(10);
                    defaultSetting.setAutoMarkAbsentMinutes(30);
                    defaultSetting.setManualOverride(false);
                    defaultSetting.setSendAlerts(false);
                    return settingRepository.save(defaultSetting);
                }));
    }

}

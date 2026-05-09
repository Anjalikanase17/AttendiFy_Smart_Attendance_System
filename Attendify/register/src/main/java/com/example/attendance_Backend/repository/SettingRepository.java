package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, Long> {
    Optional<Setting> findByAdminId(Long adminId);
}

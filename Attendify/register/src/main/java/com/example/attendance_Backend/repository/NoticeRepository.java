package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByAdminIdOrderByCreatedAtDesc(Long adminId);
    List<Notice> findByCreatedAtBefore(LocalDateTime dateTime);
}

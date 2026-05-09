package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.Notes;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;
import java.time.LocalDateTime;

public interface NotesRepository extends JpaRepository<Notes, Long> {
    List<Notes> findByAdminId(Long adminId);

    List<Notes> findByDepartment_IdAndClassMaster_IdAndDivisionMaster_IdAndAdminId(
            Integer departmentId, Integer classId, Integer divisionId, Long adminId);

    List<Notes> findByUploadTimeBefore(LocalDateTime dateTime);
}
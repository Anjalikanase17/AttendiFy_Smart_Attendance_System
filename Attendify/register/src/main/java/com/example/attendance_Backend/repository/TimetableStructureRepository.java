package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.TimetableStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface TimetableStructureRepository extends JpaRepository<TimetableStructure, Integer> {

    /** All slots ordered for display */
    @Query("SELECT t FROM TimetableStructure t WHERE t.admin.id = :adminId ORDER BY t.slotOrder ASC")
    List<TimetableStructure> findAllOrderedByAdminId(@Param("adminId") Long adminId);

    Optional<TimetableStructure> findByIdAndAdminId(int id, Long adminId);

    // Legacy method
    @Query("SELECT t FROM TimetableStructure t ORDER BY t.slotOrder ASC")
    List<TimetableStructure> findAllOrdered();

    Optional<TimetableStructure> findByLabelAndAdminId(String label, Long adminId);

    // Legacy method
    Optional<TimetableStructure> findByLabel(String label);
}

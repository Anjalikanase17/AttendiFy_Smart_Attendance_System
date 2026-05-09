package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.DivisionMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import java.util.Optional;

@Repository
public interface DivisionMasterRepository extends JpaRepository<DivisionMaster, Integer> {
    List<DivisionMaster> findByAdminId(Long adminId);

    Optional<DivisionMaster> findByIdAndAdminId(Integer id, Long adminId);

    List<DivisionMaster> findByClassMasterIdAndAdminId(Integer classId, Long adminId);

    Optional<DivisionMaster> findByDivisionNameAndAdminId(String divisionName, Long adminId);

    Optional<DivisionMaster> findByDivisionNameAndClassMasterIdAndAdminId(String divisionName, Integer classId,
            Long adminId);

    // Legacy methods during migration
    List<DivisionMaster> findByClassMasterId(Integer classId);

    Optional<DivisionMaster> findByDivisionName(String divisionName);
}

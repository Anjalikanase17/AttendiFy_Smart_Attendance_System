package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.User;
import com.example.attendance_Backend.dto.AdminStudentListDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    // Admin-filtered methods
    long countByDivisionMaster_IdAndAdminId(Integer divisionId, Long adminId);

    long countByAdminId(Long adminId);

    Optional<User> findByIdAndAdminId(Integer id, Long adminId);

    Optional<User> findByEmailAndPasswordAndAdminId(String email, String password, Long adminId);

    Optional<User> findByEmailAndAdminId(String email, Long adminId);

    Optional<User> findByRollNoAndAdminId(String rollNo, Long adminId);

    List<User> findByClassMaster_IdAndAdminId(Integer classId, Long adminId);

    List<User> findByClassMaster_IdAndDivisionMaster_IdAndAdminId(Integer classId, Integer divisionId, Long adminId);

    List<User> findByClassMasterIsNotNullAndAdminId(Long adminId);

    List<User> findByAdminId(Long adminId);

    List<User> findByAdminId(Long adminId, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT u
            FROM User u
            WHERE (:adminId IS NULL OR u.admin.id = :adminId)
            AND (u.role IS NULL OR UPPER(u.role) IN ('STUDENT', 'USER'))
            AND (:departmentId IS NULL OR (u.classMaster IS NOT NULL AND u.classMaster.department.id = :departmentId))
            AND (:classId IS NULL OR (u.classMaster IS NOT NULL AND u.classMaster.id = :classId))
            AND (:divisionId IS NULL OR (u.divisionMaster IS NOT NULL AND u.divisionMaster.id = :divisionId))
            AND (
                :q IS NULL OR :q = ''
                OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.rollNo, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.mobilenumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<User> searchStudentsForAdmin(
            @Param("adminId") Long adminId,
            @Param("departmentId") Integer departmentId,
            @Param("classId") Integer classId,
            @Param("divisionId") Integer divisionId,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT new com.example.attendance_Backend.dto.AdminStudentListDTO(
                u.id,
                u.name,
                u.rollNo,
                u.email,
                u.mobilenumber,
                u.address,
                c.id,
                c.className,
                d.id,
                d.divisionName
            )
            FROM User u
            LEFT JOIN u.classMaster c
            LEFT JOIN u.divisionMaster d
            WHERE (:adminId IS NULL OR u.admin.id = :adminId)
            AND (u.role IS NULL OR UPPER(u.role) IN ('STUDENT', 'USER'))
            AND (:departmentId IS NULL OR (c IS NOT NULL AND c.department.id = :departmentId))
            AND (:classId IS NULL OR (c IS NOT NULL AND c.id = :classId))
            AND (:divisionId IS NULL OR (d IS NOT NULL AND d.id = :divisionId))
            AND (
                :q IS NULL OR :q = ''
                OR LOWER(COALESCE(u.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.rollNo, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(u.mobilenumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(c.className, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(d.divisionName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<AdminStudentListDTO> searchStudentsForAdminProjected(
            @Param("adminId") Long adminId,
            @Param("departmentId") Integer departmentId,
            @Param("classId") Integer classId,
            @Param("divisionId") Integer divisionId,
            @Param("q") String q,
            Pageable pageable);

    // Legacy methods
    long countByDivisionMaster_Id(Integer divisionId);

    Optional<User> findByEmailAndPassword(String email, String password);

    Optional<User> findByEmail(String email);

    Optional<User> findByRollNo(String rollNo);

    List<User> findByClassMaster_Id(Integer classId);

    List<User> findByClassMaster_IdAndDivisionMaster_Id(Integer classId, Integer divisionId);

    List<User> findByClassMasterIsNotNull();

    @Modifying
    @Query("""
            UPDATE User u
            SET u.role = 'STUDENT'
            WHERE u.rollNo IS NOT NULL
            AND (u.role IS NULL OR UPPER(u.role) = 'USER')
            """)
    int backfillStudentRoles();

}

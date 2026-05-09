package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.dto.AdminTeacherListDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Integer> {

    // Admin-filtered methods
    List<Teacher> findByAdminId(Long adminId);

    List<Teacher> findByAdminId(Long adminId, org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT t
            FROM Teacher t
            WHERE (:adminId IS NULL OR t.admin.id = :adminId)
            AND (:departmentId IS NULL OR (t.department IS NOT NULL AND t.department.id = :departmentId))
            AND (
                :q IS NULL OR :q = ''
                OR LOWER(COALESCE(t.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(t.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(t.mobilenumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<Teacher> searchTeachersForAdmin(
            @Param("adminId") Long adminId,
            @Param("departmentId") Integer departmentId,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT new com.example.attendance_Backend.dto.AdminTeacherListDTO(
                t.id,
                t.name,
                t.email,
                t.mobilenumber,
                d.id,
                d.departmentName
            )
            FROM Teacher t
            LEFT JOIN t.department d
            WHERE (:adminId IS NULL OR t.admin.id = :adminId)
            AND (:departmentId IS NULL OR (d IS NOT NULL AND d.id = :departmentId))
            AND (
                :q IS NULL OR :q = ''
                OR LOWER(COALESCE(t.name, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(t.email, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(t.mobilenumber, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(COALESCE(d.departmentName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
            )
            """)
    Page<AdminTeacherListDTO> searchTeachersForAdminProjected(
            @Param("adminId") Long adminId,
            @Param("departmentId") Integer departmentId,
            @Param("q") String q,
            Pageable pageable);

    Optional<Teacher> findByIdAndAdminId(Integer id, Long adminId);

    @Query("SELECT t.admin.id FROM Teacher t WHERE t.id = :teacherId")
    Optional<Long> findAdminIdByTeacherId(@Param("teacherId") Integer teacherId);

    long countByAdminId(Long adminId);

    Optional<Teacher> findByEmailAndPasswordAndAdminId(String email, String password, Long adminId);

    Optional<Teacher> findByEmailAndAdminId(String email, Long adminId);

    boolean existsByEmailAndAdminId(String email, Long adminId);

    // Legacy methods
    Optional<Teacher> findByEmailAndPassword(String email, String password);

    Optional<Teacher> findByEmail(String email);

    boolean existsByEmail(String email);
}

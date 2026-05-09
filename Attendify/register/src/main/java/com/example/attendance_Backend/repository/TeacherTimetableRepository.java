package com.example.attendance_Backend.repository;

import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.model.TeacherTimetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherTimetableRepository extends JpaRepository<TeacherTimetable, Integer> {
    List<TeacherTimetable> findByDivisionMaster_Id(Integer divisionId);

    List<TeacherTimetable> findBySubjectMaster_Id(Integer subjectId);

    long countByDivisionMaster_Id(Integer divisionId);

    long countBySubjectMaster_Id(Integer subjectId);

    List<TeacherTimetable> findByDivisionMaster_IdAndAdmin_Id(Integer divisionId, Long adminId);

    List<TeacherTimetable> findBySubjectMaster_IdAndAdmin_Id(Integer subjectId, Long adminId);

    /** Full week timetable for a teacher (all days, all slots) */
    @Query("""
            SELECT t FROM TeacherTimetable t
            JOIN FETCH t.slot s
            WHERE t.teacher.id = :teacherId
            AND t.admin.id = :adminId
            ORDER BY s.slotOrder ASC
            """)
    List<TeacherTimetable> findByTeacherIdAndAdminId(@Param("teacherId") int teacherId, @Param("adminId") Long adminId);

    // Legacy method for migration
    @Query("""
            SELECT t FROM TeacherTimetable t
            JOIN FETCH t.slot s
            WHERE t.teacher.id = :teacherId
            ORDER BY s.slotOrder ASC
            """)
    List<TeacherTimetable> findByTeacherId(@Param("teacherId") int teacherId);

    /** A specific day's LECTURE slots for a teacher (for QR dropdown) */
    @Query("""
            SELECT t FROM TeacherTimetable t
            JOIN FETCH t.slot s
            WHERE t.teacher.id = :teacherId
              AND t.dayOfWeek = :day
              AND s.slotType = 'LECTURE'
              AND t.admin.id = :adminId
            ORDER BY s.slotOrder ASC
            """)
    List<TeacherTimetable> findByTeacherIdAndDayAndAdminId(
            @Param("teacherId") int teacherId,
            @Param("day") String day,
            @Param("adminId") Long adminId);

    // Legacy method
    @Query("""
            SELECT t FROM TeacherTimetable t
            JOIN FETCH t.slot s
            WHERE t.teacher.id = :teacherId
              AND t.dayOfWeek = :day
              AND s.slotType = 'LECTURE'
            ORDER BY s.slotOrder ASC
            """)
    List<TeacherTimetable> findByTeacherIdAndDay(
            @Param("teacherId") int teacherId,
            @Param("day") String day);

    /** Find a specific teacher+slot+day combination (upsert) */
    Optional<TeacherTimetable> findByTeacherIdAndSlotIdAndDayOfWeekAndAdminId(
            Integer teacherId, Integer slotId, String dayOfWeek, Long adminId);

    // Legacy method
    Optional<TeacherTimetable> findByTeacherIdAndSlotIdAndDayOfWeek(
            Integer teacherId, Integer slotId, String dayOfWeek);

    /**
     * Find all unique teachers for a specific class and division from the timetable
     */
    @Query("SELECT DISTINCT t.teacher FROM TeacherTimetable t WHERE t.classMaster.id = :classId AND t.divisionMaster.id = :divisionId AND t.admin.id = :adminId")
    List<Teacher> findDistinctTeachersByClassMasterIdAndDivisionMasterIdAndAdminId(
            @Param("classId") int classId,
            @Param("divisionId") int divisionId,
            @Param("adminId") Long adminId);

    // Legacy method
    @Query("SELECT DISTINCT t.teacher FROM TeacherTimetable t WHERE t.classMaster.id = :classId AND t.divisionMaster.id = :divisionId")
    List<Teacher> findDistinctTeachersByClassMasterIdAndDivisionMasterId(
            @Param("classId") int classId,
            @Param("divisionId") int divisionId);
}

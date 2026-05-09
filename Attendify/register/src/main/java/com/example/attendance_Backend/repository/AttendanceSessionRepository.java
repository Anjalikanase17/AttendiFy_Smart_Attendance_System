package com.example.attendance_Backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

import com.example.attendance_Backend.model.AttendanceSession;

public interface AttendanceSessionRepository
                extends JpaRepository<AttendanceSession, String> {

        List<AttendanceSession> findByDivisionMaster_Id(Integer divisionId);

        List<AttendanceSession> findBySubjectMaster_Id(Integer subjectId);

        long countByDivisionMaster_Id(Integer divisionId);

        long countBySubjectMaster_Id(Integer subjectId);

        long countByDivisionMaster_IdAndExpiryTimeAfter(Integer divisionId, LocalDateTime now);

        long countBySubjectMaster_IdAndExpiryTimeAfter(Integer subjectId, LocalDateTime now);

        @Query("SELECT s FROM AttendanceSession s WHERE s.timetableSlotId = :timetableSlotId AND s.admin.id = :adminId AND s.expiryTime BETWEEN :startOfDay AND :endOfDay")
        List<AttendanceSession> findByTimetableSlotIdAndExpiryTimeBetweenAndAdminId(
                        @org.springframework.data.repository.query.Param("timetableSlotId") Integer timetableSlotId,
                        @org.springframework.data.repository.query.Param("adminId") Long adminId,
                        @org.springframework.data.repository.query.Param("startOfDay") java.time.LocalDateTime startOfDay,
                        @org.springframework.data.repository.query.Param("endOfDay") java.time.LocalDateTime endOfDay);

        @Query("SELECT s FROM AttendanceSession s WHERE s.classMaster.id = :classId AND s.divisionMaster.id = :divisionId AND s.subjectMaster.id = :subjectId AND s.teacherId = :teacherId AND s.admin.id = :adminId AND s.expiryTime BETWEEN :startOfDay AND :endOfDay")
        List<AttendanceSession> findByLectureDetails(
                        @org.springframework.data.repository.query.Param("classId") Integer classId,
                        @org.springframework.data.repository.query.Param("divisionId") Integer divisionId,
                        @org.springframework.data.repository.query.Param("subjectId") Integer subjectId,
                        @org.springframework.data.repository.query.Param("teacherId") Integer teacherId,
                        @org.springframework.data.repository.query.Param("adminId") Long adminId,
                        @org.springframework.data.repository.query.Param("startOfDay") java.time.LocalDateTime startOfDay,
                        @org.springframework.data.repository.query.Param("endOfDay") java.time.LocalDateTime endOfDay);
}

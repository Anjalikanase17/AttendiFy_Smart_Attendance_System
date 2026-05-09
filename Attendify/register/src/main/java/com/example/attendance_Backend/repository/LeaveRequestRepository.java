package com.example.attendance_Backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.attendance_Backend.model.LeaveRequest;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Integer> {

    List<LeaveRequest> findByAdminId(Long adminId);

    List<LeaveRequest> findByStudentIdAndAdminId(int studentId, Long adminId);

    List<LeaveRequest> findByTeacherIdAndAdminId(int teacherId, Long adminId);

    // Keep legacy for now if needed by internal tasks, but controller should use
    // tenant-aware ones
    List<LeaveRequest> findByStudentId(int studentId);

    List<LeaveRequest> findByTeacherId(int teacherId);
}

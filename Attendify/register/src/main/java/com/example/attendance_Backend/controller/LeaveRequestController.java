package com.example.attendance_Backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendance_Backend.security.AdminContextHolder;
import com.example.attendance_Backend.model.Admin;
import java.util.Collections;
import com.example.attendance_Backend.model.LeaveRequest;
import com.example.attendance_Backend.repository.LeaveRequestRepository;

@RestController
@RequestMapping("/api/leave")
@CrossOrigin(origins = "*")
public class LeaveRequestController {

    @Autowired
    private LeaveRequestRepository leaveRepo;

    // STUDENT SUBMIT
    @PostMapping("/submit")
    public String submitLeave(@RequestBody LeaveRequest leave) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            Admin admin = new Admin();
            admin.setId(adminId);
            leave.setAdmin(admin);
        }

        leave.setStatus("Pending"); // default status
        leaveRepo.save(leave);
        return "Leave submitted successfully";
    }

    // STUDENT VIEW OWN LEAVE
    @GetMapping("/student/{studentId}")
    public List<LeaveRequest> getStudentLeaves(@PathVariable int studentId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();
        return leaveRepo.findByStudentIdAndAdminId(studentId, adminId);
    }

    // TEACHER VIEW ALL
    @GetMapping("/all")
    public List<LeaveRequest> getAllLeaves() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();
        return leaveRepo.findByAdminId(adminId);
    }

    // TEACHER VIEW ASSIGNED
    @GetMapping("/teacher/{teacherId}")
    public List<LeaveRequest> getTeacherLeaves(@PathVariable int teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return Collections.emptyList();
        return leaveRepo.findByTeacherIdAndAdminId(teacherId, adminId);
    }

    // TEACHER APPROVE
    @PutMapping("/approve/{id}")
    public ResponseEntity<String> approveLeave(@PathVariable int id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(401).body("Unauthorized");

        LeaveRequest leave = leaveRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getAdmin() == null || !leave.getAdmin().getId().equals(adminId)) {
            return ResponseEntity.status(401).body("Unauthorized to approve this leave");
        }

        leave.setStatus("Approved");
        leaveRepo.save(leave);

        return ResponseEntity.ok("Leave Approved");
    }

    // TEACHER REJECT
    @PutMapping("/reject/{id}")
    public ResponseEntity<String> rejectLeave(@PathVariable int id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(401).body("Unauthorized");

        LeaveRequest leave = leaveRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Leave not found"));

        if (leave.getAdmin() == null || !leave.getAdmin().getId().equals(adminId)) {
            return ResponseEntity.status(401).body("Unauthorized to reject this leave");
        }

        leave.setStatus("Rejected");
        leaveRepo.save(leave);

        return ResponseEntity.ok("Leave Rejected");
    }
}
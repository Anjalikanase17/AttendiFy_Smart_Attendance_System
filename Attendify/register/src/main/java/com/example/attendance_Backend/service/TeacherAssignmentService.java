package com.example.attendance_Backend.service;

import com.example.attendance_Backend.dto.TeacherAssignmentDTO;
import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.model.TeacherAssignment;
import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.repository.TeacherAssignmentRepository;
import com.example.attendance_Backend.repository.TeacherRepository;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TeacherAssignmentService {

    private final TeacherAssignmentRepository assignmentRepository;
    private final TeacherRepository teacherRepository;

    public TeacherAssignmentService(TeacherAssignmentRepository assignmentRepository,
            TeacherRepository teacherRepository) {
        this.assignmentRepository = assignmentRepository;
        this.teacherRepository = teacherRepository;
    }

    public TeacherAssignmentDTO createAssignment(int teacherId, String subject,
            String className, String division, String roomNumber) {

        Long adminId = AdminContextHolder.getAdminId();

        Teacher teacher = null;
        if (adminId != null) {
            teacher = teacherRepository.findByIdAndAdminId(teacherId, adminId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Teacher not found or unauthorized"));
            if (assignmentRepository.existsByTeacherIdAndSubjectAndClassNameAndAdminId(teacherId, subject, className,
                    adminId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment already exists");
            }
        } else {
            teacher = teacherRepository.findById(teacherId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Teacher not found"));
            if (assignmentRepository.existsByTeacherIdAndSubjectAndClassName(teacherId, subject, className)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Assignment already exists");
            }
        }

        TeacherAssignment assignment = new TeacherAssignment();
        if (adminId != null) {
            Admin admin = new Admin();
            admin.setId(adminId);
            assignment.setAdmin(admin);
        }

        assignment.setTeacher(teacher);
        assignment.setSubject(subject);
        assignment.setClassName(className);
        assignment.setDivision(division);
        assignment.setRoomNumber(roomNumber);

        assignment = assignmentRepository.save(assignment);

        return toDTO(assignment);
    }

    public List<TeacherAssignmentDTO> getAssignmentsForTeacher(int teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        List<TeacherAssignment> assignments;
        if (adminId != null) {
            assignments = assignmentRepository.findByTeacherIdAndAdminId(teacherId, adminId);
        } else {
            assignments = assignmentRepository.findByTeacherId(teacherId);
        }

        return assignments.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public void deleteAssignment(int assignmentId) {
        Long adminId = AdminContextHolder.getAdminId();
        TeacherAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found"));

        if (adminId != null) {
            if (assignment.getAdmin() == null || !assignment.getAdmin().getId().equals(adminId)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized to delete this assignment");
            }
        }
        assignmentRepository.delete(assignment);
    }

    private TeacherAssignmentDTO toDTO(TeacherAssignment a) {
        return new TeacherAssignmentDTO(
                a.getId(),
                a.getTeacher().getId(),
                a.getTeacher().getName(),
                a.getSubject(),
                a.getClassName(),
                a.getDivision(),
                a.getRoomNumber());
    }
}

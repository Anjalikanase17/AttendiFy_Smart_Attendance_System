package com.example.attendance_Backend.controller;

import com.example.attendance_Backend.dto.TeacherDTO;
import com.example.attendance_Backend.model.*;
import com.example.attendance_Backend.repository.*;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/master")
@CrossOrigin(origins = "*")
public class MasterDataController {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private ClassMasterRepository classMasterRepository;
    @Autowired
    private DivisionMasterRepository divisionMasterRepository;
    @Autowired
    private SubjectMasterRepository subjectMasterRepository;
    @Autowired
    private ClassSubjectRepository classSubjectRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AttendanceSessionRepository attendanceSessionRepository;
    @Autowired
    private TeacherTimetableRepository teacherTimetableRepository;

    // ==========================================
    // 1. DEPARTMENT CRUD
    // ==========================================
    @Cacheable(cacheNames = "master_departments", key = "#principal != null ? #principal.name : 'anon'")
    @GetMapping("/departments")
    public ResponseEntity<List<Department>> getAllDepartments(Principal principal) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(departmentRepository.findByAdminId(adminId));
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @PostMapping("/departments")
    public ResponseEntity<?> createDepartment(@RequestBody Department department) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (departmentRepository.findByDepartmentNameAndAdminId(department.getDepartmentName(), adminId).isPresent()) {
            return ResponseEntity.badRequest().body("Department with this name already exists in your organization.");
        }

        Admin admin = new Admin();
        admin.setId(adminId);
        department.setAdmin(admin);

        return ResponseEntity.ok(departmentRepository.save(department));
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @DeleteMapping("/departments/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable int id) {
        if (!classMasterRepository.findByDepartmentId(id).isEmpty() ||
                !subjectMasterRepository.findByDepartmentId(id).isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot delete department. It is linked to classes or subjects.");
        }
        departmentRepository.deleteById(id);
        return ResponseEntity.ok("Deleted successfully.");
    }

    // ==========================================
    // 2. CLASS MASTER CRUD
    // ==========================================
    @Cacheable(cacheNames = "master_classes", key = "#principal != null ? #principal.name : 'anon'")
    @GetMapping("/classes")
    public ResponseEntity<List<ClassMaster>> getAllClasses(Principal principal) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(classMasterRepository.findByAdminId(adminId));
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @PostMapping("/classes")
    public ResponseEntity<?> createClass(@RequestBody ClassMaster classMaster) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (classMasterRepository.findByClassNameAndAdminId(classMaster.getClassName(), adminId).isPresent()) {
            return ResponseEntity.badRequest().body("Class with this name already exists in your organization.");
        }

        Admin admin = new Admin();
        admin.setId(adminId);
        classMaster.setAdmin(admin);

        return ResponseEntity.ok(classMasterRepository.save(classMaster));
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @DeleteMapping("/classes/{id}")
    public ResponseEntity<?> deleteClass(@PathVariable int id) {
        if (!divisionMasterRepository.findByClassMasterId(id).isEmpty() ||
                !classSubjectRepository.findByClassMasterId(id).isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot delete class. It is linked to divisions or subjects.");
        }
        classMasterRepository.deleteById(id);
        return ResponseEntity.ok("Deleted successfully.");
    }

    @Cacheable(cacheNames = "master_class_divisions", key = "(#principal != null ? #principal.name : 'anon') + ':' + #id")
    @GetMapping("/classes/{id}/divisions")
    public ResponseEntity<List<DivisionMaster>> getDivisionsByClass(@PathVariable int id, Principal principal) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(divisionMasterRepository.findByClassMasterIdAndAdminId(id, adminId));
    }

    @Cacheable(cacheNames = "master_class_subjects", key = "(#principal != null ? #principal.name : 'anon') + ':' + #id")
    @GetMapping("/classes/{id}/subjects")
    public ResponseEntity<List<SubjectMaster>> getSubjectsByClass(@PathVariable int id, Principal principal) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ClassSubject> mappings = classSubjectRepository.findByClassMasterIdAndAdminId(id, adminId);
        List<SubjectMaster> subjects = mappings.stream().map(ClassSubject::getSubjectMaster)
                .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(subjects);
    }

    // ==========================================
    // 3. DIVISION MASTER CRUD
    // ==========================================
    @Cacheable(cacheNames = "master_divisions", key = "#principal != null ? #principal.name : 'anon'")
    @GetMapping("/divisions")
    public ResponseEntity<List<DivisionMaster>> getAllDivisions(Principal principal) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(divisionMasterRepository.findByAdminId(adminId));
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @PostMapping("/divisions")
    public ResponseEntity<?> createDivision(@RequestBody DivisionMaster divisionMaster) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (divisionMasterRepository.findByDivisionNameAndAdminId(divisionMaster.getDivisionName(), adminId)
                .isPresent()) {
            // Check if it's the same class too? Usually divisions are unique per class.
            // But let's check global uniqueness for simplicity if that's the requirement.
            // Actually, natural constraint is per class.
            if (divisionMasterRepository.findByDivisionNameAndClassMasterIdAndAdminId(
                    divisionMaster.getDivisionName(), divisionMaster.getClassMaster().getId(), adminId).isPresent()) {
                return ResponseEntity.badRequest().body("Division with this name already exists for this class.");
            }
        }

        Admin admin = new Admin();
        admin.setId(adminId);
        divisionMaster.setAdmin(admin);

        return ResponseEntity.ok(divisionMasterRepository.save(divisionMaster));
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @DeleteMapping("/divisions/{id}")
    @Transactional
    public ResponseEntity<?> deleteDivision(@PathVariable int id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (userRepository.countByDivisionMaster_Id(id) > 0) {
            return ResponseEntity.badRequest().body("Cannot delete division. It is linked to students.");
        }
        if (attendanceRepository.countByDivisionMaster_Id(id) > 0) {
            return ResponseEntity.badRequest().body("Cannot delete division. It is linked to attendance records.");
        }
        if (attendanceSessionRepository.countByDivisionMaster_IdAndExpiryTimeAfter(id, LocalDateTime.now()) > 0) {
            return ResponseEntity.badRequest()
                    .body("Cannot delete division. It is linked to active attendance sessions.");
        }
        List<TeacherTimetable> linkedTimetables = teacherTimetableRepository.findByDivisionMaster_Id(id);
        if (!linkedTimetables.isEmpty()) {
            teacherTimetableRepository.deleteAll(linkedTimetables);
            teacherTimetableRepository.flush();
        }
        List<AttendanceSession> linkedSessions = attendanceSessionRepository.findByDivisionMaster_Id(id);
        if (!linkedSessions.isEmpty()) {
            attendanceSessionRepository.deleteAll(linkedSessions);
            attendanceSessionRepository.flush();
        }
        divisionMasterRepository.deleteById(id);
        return ResponseEntity.ok("Deleted successfully.");
    }

    // ==========================================
    // 4. SUBJECT MASTER CRUD
    // ==========================================
    @Cacheable(cacheNames = "master_subjects", key = "#principal != null ? #principal.name : 'anon'")
    @GetMapping("/subjects")
    public ResponseEntity<List<SubjectMaster>> getAllSubjects(Principal principal) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(subjectMasterRepository.findByAdminId(adminId));
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @PostMapping("/subjects")
    public ResponseEntity<?> createSubject(@RequestBody SubjectMaster subjectMaster) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (subjectMasterRepository.findBySubjectNameAndAdminId(subjectMaster.getSubjectName(), adminId).isPresent()) {
            return ResponseEntity.badRequest().body("Subject with this name already exists in your organization.");
        }

        Admin admin = new Admin();
        admin.setId(adminId);
        subjectMaster.setAdmin(admin);

        return ResponseEntity.ok(subjectMasterRepository.save(subjectMaster));
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @DeleteMapping("/subjects/{id}")
    @Transactional
    public ResponseEntity<?> deleteSubject(@PathVariable int id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (!classSubjectRepository.findBySubjectMasterId(id).isEmpty()) {
            return ResponseEntity.badRequest().body("Cannot delete subject. It is linked to class mappings.");
        }
        if (attendanceRepository.countBySubjectMaster_Id(id) > 0) {
            return ResponseEntity.badRequest().body("Cannot delete subject. It is linked to attendance records.");
        }
        if (attendanceSessionRepository.countBySubjectMaster_IdAndExpiryTimeAfter(id, LocalDateTime.now()) > 0) {
            return ResponseEntity.badRequest()
                    .body("Cannot delete subject. It is linked to active attendance sessions.");
        }
        List<TeacherTimetable> linkedTimetables = teacherTimetableRepository.findBySubjectMaster_Id(id);
        if (!linkedTimetables.isEmpty()) {
            teacherTimetableRepository.deleteAll(linkedTimetables);
            teacherTimetableRepository.flush();
        }
        List<AttendanceSession> linkedSessions = attendanceSessionRepository.findBySubjectMaster_Id(id);
        if (!linkedSessions.isEmpty()) {
            attendanceSessionRepository.deleteAll(linkedSessions);
            attendanceSessionRepository.flush();
        }
        subjectMasterRepository.deleteById(id);
        return ResponseEntity.ok("Deleted successfully.");
    }

    // ==========================================
    // 5. CLASS-SUBJECT MAPPING CRUD
    // ==========================================
    @Cacheable(cacheNames = "master_class_subject_mappings", key = "#principal != null ? #principal.name : 'anon'")
    @GetMapping("/class-subjects")
    public List<ClassSubject> getAllClassSubjects(Principal principal) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();
        return classSubjectRepository.findByAdminId(adminId);
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @PostMapping("/class-subjects")
    public ResponseEntity<?> createClassSubjectMapping(@RequestBody ClassSubject classSubject) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        if (classSubjectRepository
                .findByClassMasterIdAndSubjectMasterIdAndAdminId(classSubject.getClassMaster().getId(),
                        classSubject.getSubjectMaster().getId(), adminId)
                .isPresent()) {
            return ResponseEntity.badRequest().body("Mapping already exists");
        }

        Admin admin = new Admin();
        admin.setId(adminId);
        classSubject.setAdmin(admin);

        return ResponseEntity.ok(classSubjectRepository.save(classSubject));
    }

    @CacheEvict(cacheNames = {
            "master_departments", "master_classes", "master_class_divisions", "master_class_subjects",
            "master_divisions", "master_subjects", "master_class_subject_mappings", "master_class_division_teachers"
    }, allEntries = true)
    @DeleteMapping("/class-subjects/{id}")
    public ResponseEntity<?> deleteClassSubjectMapping(@PathVariable int id) {
        classSubjectRepository.deleteById(id);
        return ResponseEntity.ok("Deleted successfully.");
    }

    @Cacheable(cacheNames = "master_class_division_teachers", key = "(#principal != null ? #principal.name : 'anon') + ':' + #classId + ':' + #divisionId")
    @GetMapping("/classes/{classId}/divisions/{divisionId}/teachers")
    public List<TeacherDTO> getTeachersByClassAndDivision(
            @PathVariable int classId,
            @PathVariable int divisionId,
            Principal principal) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            return java.util.Collections.emptyList();

        List<Teacher> teachers = teacherTimetableRepository
                .findDistinctTeachersByClassMasterIdAndDivisionMasterIdAndAdminId(
                        classId,
                        divisionId,
                        adminId);
        return teachers.stream()
                .map(t -> new TeacherDTO(t.getId(), t.getName()))
                .collect(java.util.stream.Collectors.toList());
    }
}

package com.example.attendance_Backend.service;

import com.example.attendance_Backend.model.Teacher;
import com.example.attendance_Backend.model.TeacherTimetable;
import com.example.attendance_Backend.model.TimetableStructure;
import com.example.attendance_Backend.repository.TeacherTimetableRepository;
import com.example.attendance_Backend.repository.TeacherRepository;
import com.example.attendance_Backend.repository.TimetableStructureRepository;
import com.example.attendance_Backend.repository.ClassMasterRepository;
import com.example.attendance_Backend.repository.DivisionMasterRepository;
import com.example.attendance_Backend.repository.SubjectMasterRepository;
import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TeacherTimetableService {

    private final TeacherTimetableRepository ttRepo;
    private final TeacherRepository teacherRepo;
    private final TimetableStructureRepository structureRepo;
    private final ClassMasterRepository classMasterRepo;
    private final DivisionMasterRepository divisionMasterRepo;
    private final SubjectMasterRepository subjectMasterRepo;

    public TeacherTimetableService(
            TeacherTimetableRepository ttRepo,
            TeacherRepository teacherRepo,
            TimetableStructureRepository structureRepo,
            ClassMasterRepository classMasterRepo,
            DivisionMasterRepository divisionMasterRepo,
            SubjectMasterRepository subjectMasterRepo) {
        this.ttRepo = ttRepo;
        this.teacherRepo = teacherRepo;
        this.structureRepo = structureRepo;
        this.classMasterRepo = classMasterRepo;
        this.divisionMasterRepo = divisionMasterRepo;
        this.subjectMasterRepo = subjectMasterRepo;
    }

    public List<TeacherTimetable> getFullTimetable(int teacherId) {
        Long adminId = resolveAdminIdForTeacher(teacherId);
        if (adminId != null) {
            return ttRepo.findByTeacherIdAndAdminId(teacherId, adminId);
        }
        return ttRepo.findByTeacherId(teacherId);
    }

    /**
     * Today's LECTURE slots for QR dropdown.
     * Returns list with label like "Period 1 — Java - MCA I-A — Room 201"
     */
    public List<TeacherTimetable> getTodayLectures(int teacherId) {
        String today = LocalDate.now()
                .getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toUpperCase(); // e.g. MONDAY

        Long adminId = resolveAdminIdForTeacher(teacherId);
        if (adminId != null) {
            return ttRepo.findByTeacherIdAndDayAndAdminId(teacherId, today, adminId);
        }
        return ttRepo.findByTeacherIdAndDay(teacherId, today);
    }

    /** Save or update a single timetable cell */
    public TeacherTimetable saveSlot(int teacherId, int slotId, String day,
            Map<String, Object> body) {
        String normalizedDay = day.toUpperCase();
        Long adminId = resolveAdminIdForTeacher(teacherId);

        TeacherTimetable tt = (adminId != null)
                ? ttRepo.findByTeacherIdAndSlotIdAndDayOfWeekAndAdminId(teacherId, slotId, normalizedDay, adminId)
                        .or(() -> ttRepo.findByTeacherIdAndSlotIdAndDayOfWeek(teacherId, slotId, normalizedDay))
                        .orElse(new TeacherTimetable())
                : ttRepo.findByTeacherIdAndSlotIdAndDayOfWeek(teacherId, slotId, normalizedDay)
                        .orElse(new TeacherTimetable());

        Teacher teacher = (adminId != null)
                ? teacherRepo.findByIdAndAdminId(teacherId, adminId)
                        .or(() -> teacherRepo.findById(teacherId))
                        .orElseThrow(() -> new RuntimeException("Teacher not found"))
                : teacherRepo.findById(teacherId)
                        .orElseThrow(() -> new RuntimeException("Teacher not found"));

        TimetableStructure slot = (adminId != null)
                ? structureRepo.findByIdAndAdminId(slotId, adminId)
                        .or(() -> structureRepo.findById(slotId))
                        .orElseThrow(() -> new RuntimeException("Slot not found"))
                : structureRepo.findById(slotId)
                        .orElseThrow(() -> new RuntimeException("Slot not found"));

        tt.setTeacher(teacher);
        tt.setSlot(slot);
        tt.setDayOfWeek(normalizedDay);

        if (tt.getAdmin() == null && adminId != null) {
            Admin admin = new Admin();
            admin.setId(adminId);
            tt.setAdmin(admin);
        }

        Integer classId = extractId(body, "classMasterId", "classId", "classMaster");
        if (classId != null) {
            tt.setClassMaster((adminId != null)
                    ? classMasterRepo.findByIdAndAdminId(classId, adminId).or(() -> classMasterRepo.findById(classId))
                            .orElse(null)
                    : classMasterRepo.findById(classId).orElse(null));
        } else {
            tt.setClassMaster(null);
        }

        Integer divisionId = extractId(body, "divisionMasterId", "divisionId", "divisionMaster");
        if (divisionId != null) {
            tt.setDivisionMaster((adminId != null)
                    ? divisionMasterRepo.findByIdAndAdminId(divisionId, adminId)
                            .or(() -> divisionMasterRepo.findById(divisionId)).orElse(null)
                    : divisionMasterRepo.findById(divisionId).orElse(null));
        } else {
            tt.setDivisionMaster(null);
        }

        Integer subjectId = extractId(body, "subjectMasterId", "subjectId", "subjectMaster");
        if (subjectId != null) {
            tt.setSubjectMaster((adminId != null)
                    ? subjectMasterRepo.findByIdAndAdminId(subjectId, adminId)
                            .or(() -> subjectMasterRepo.findById(subjectId)).orElse(null)
                    : subjectMasterRepo.findById(subjectId).orElse(null));
        } else {
            tt.setSubjectMaster(null);
        }

        Object room = body.get("roomNo");
        tt.setRoomNo(room == null ? "" : String.valueOf(room));
        return ttRepo.save(tt);
    }

    private Long resolveAdminIdForTeacher(int teacherId) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return adminId;
        }
        return teacherRepo.findAdminIdByTeacherId(teacherId).orElse(null);
    }

    private Integer extractId(Map<String, Object> body, String idKey1, String idKey2, String objKey) {
        Object val = body.get(idKey1);
        if (val == null)
            val = body.get(idKey2);

        if (val instanceof Integer)
            return (Integer) val;
        if (val instanceof String && !((String) val).isEmpty())
            return Integer.parseInt((String) val);

        Object obj = body.get(objKey);
        if (obj instanceof Map) {
            Object idVal = ((Map<?, ?>) obj).get("id");
            if (idVal instanceof Integer)
                return (Integer) idVal;
            if (idVal instanceof String && !((String) idVal).isEmpty())
                return Integer.parseInt((String) idVal);
        }
        return null;
    }
}

package com.example.attendance_Backend.service;

import com.example.attendance_Backend.model.TimetableStructure;
import com.example.attendance_Backend.model.Admin;
import com.example.attendance_Backend.repository.TimetableStructureRepository;
import com.example.attendance_Backend.security.AdminContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class TimetableStructureService {

    private final TimetableStructureRepository repo;

    public TimetableStructureService(TimetableStructureRepository repo) {
        this.repo = repo;
    }

    public List<TimetableStructure> getAll() {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            return repo.findAllOrderedByAdminId(adminId);
        }
        return repo.findAllOrdered();
    }

    public TimetableStructure save(TimetableStructure structure) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId != null) {
            Admin admin = new Admin();
            admin.setId(adminId);
            structure.setAdmin(admin);
        }
        return repo.save(structure);
    }

    public TimetableStructure update(Integer id, TimetableStructure updated) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            throw new RuntimeException("Unauthorized");

        TimetableStructure existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));

        if (existing.getAdmin() == null || !existing.getAdmin().getId().equals(adminId)) {
            throw new RuntimeException("Unauthorized to update this slot");
        }

        existing.setSlotOrder(updated.getSlotOrder());
        existing.setLabel(updated.getLabel());
        existing.setSlotType(updated.getSlotType());
        existing.setStartTime(updated.getStartTime());
        existing.setEndTime(updated.getEndTime());
        return repo.save(existing);
    }

    public void delete(Integer id) {
        Long adminId = AdminContextHolder.getAdminId();
        if (adminId == null)
            throw new RuntimeException("Unauthorized");

        TimetableStructure existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Slot not found: " + id));

        if (existing.getAdmin() == null || !existing.getAdmin().getId().equals(adminId)) {
            throw new RuntimeException("Unauthorized to delete this slot");
        }

        repo.delete(existing);
    }
}

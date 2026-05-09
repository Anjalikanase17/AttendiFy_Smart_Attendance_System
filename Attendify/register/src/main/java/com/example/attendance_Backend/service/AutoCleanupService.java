package com.example.attendance_Backend.service;

import com.example.attendance_Backend.model.Notes;
import com.example.attendance_Backend.model.Notice;
import com.example.attendance_Backend.repository.NotesRepository;
import com.example.attendance_Backend.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AutoCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(AutoCleanupService.class);

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NotesRepository notesRepository;

    // Run every day at midnight
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldRecords() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        logger.info("Starting automatic cleanup for records older than: {}", threshold);

        cleanupNotices(threshold);
        cleanupNotes(threshold);

        logger.info("Automatic cleanup completed.");
    }

    private void cleanupNotices(LocalDateTime threshold) {
        List<Notice> oldNotices = noticeRepository.findByCreatedAtBefore(threshold);
        if (!oldNotices.isEmpty()) {
            logger.info("Found {} expired notices to delete.", oldNotices.size());
            for (Notice notice : oldNotices) {
                deleteFile(notice.getFilePath());
            }
            noticeRepository.deleteAll(oldNotices);
        }
    }

    private void cleanupNotes(LocalDateTime threshold) {
        List<Notes> oldNotes = notesRepository.findByUploadTimeBefore(threshold);
        if (!oldNotes.isEmpty()) {
            logger.info("Found {} expired notes to delete.", oldNotes.size());
            for (Notes note : oldNotes) {
                deleteFile(note.getFilePath());
            }
            notesRepository.deleteAll(oldNotes);
        }
    }

    private void deleteFile(String filePath) {
        if (filePath != null) {
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    if (file.delete()) {
                        logger.info("Deleted physical file: {}", filePath);
                    } else {
                        logger.warn("Failed to delete physical file: {}", filePath);
                    }
                }
            } catch (Exception e) {
                logger.error("Error deleting file at {}: {}", filePath, e.getMessage());
            }
        }
    }
}

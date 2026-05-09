package com.example.attendance_Backend.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "master_departments",
                "master_classes",
                "master_class_divisions",
                "master_class_subjects",
                "master_divisions",
                "master_subjects",
                "master_class_subject_mappings",
                "master_class_division_teachers",
                "analytics_subject",
                "analytics_department",
                "analytics_date",
                "analytics_monthly_report",
                "report_class_attendance",
                "report_low_attendance");
    }
}

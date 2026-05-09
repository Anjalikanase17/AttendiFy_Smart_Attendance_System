package com.example.attendance_Backend.security;

public class AdminContextHolder {

    private static final ThreadLocal<Long> adminIdHolder = new ThreadLocal<>();

    public static void setAdminId(Long adminId) {
        adminIdHolder.set(adminId);
    }

    public static Long getAdminId() {
        return adminIdHolder.get();
    }

    public static void clear() {
        adminIdHolder.remove();
    }
}

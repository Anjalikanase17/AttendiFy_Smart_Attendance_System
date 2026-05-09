package com.example.attendance_Backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    private final PasswordEncoder passwordEncoder;

    public PasswordService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        if (isBcrypt(storedPassword)) {
            // Strip the `{bcrypt}` prefix if it exists, so BCryptPasswordEncoder can parse
            // the $2a$ format natively
            String cleanStoredPassword = storedPassword;
            if (cleanStoredPassword.startsWith("{bcrypt}")) {
                cleanStoredPassword = cleanStoredPassword.substring("{bcrypt}".length());
            }
            return passwordEncoder.matches(rawPassword, cleanStoredPassword);
        }

        // fallback to plain text if not recognized as bcrypt
        return storedPassword.equals(rawPassword);
    }

    public boolean needsMigration(String storedPassword) {
        return storedPassword != null && !isBcrypt(storedPassword);
    }

    private boolean isBcrypt(String value) {
        if (value.startsWith("{bcrypt}")) {
            return true;
        }
        return value.startsWith("$2a$") || value.startsWith("$2b$") || value.startsWith("$2y$");
    }
}

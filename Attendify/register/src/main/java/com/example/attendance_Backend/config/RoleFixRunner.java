package com.example.attendance_Backend.config;

import com.example.attendance_Backend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;

@Component
public class RoleFixRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    public RoleFixRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("Checking for users with missing roles...");
        long fixCount = userRepository.backfillStudentRoles();

        if (fixCount > 0) {
            System.out.println("Successfully updated " + fixCount + " students with the correct 'STUDENT' role.");
        } else {
            System.out.println("No student roles needed fixing.");
        }
    }
}

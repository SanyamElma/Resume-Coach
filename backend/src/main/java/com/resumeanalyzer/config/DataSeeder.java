package com.resumeanalyzer.config;

import com.resumeanalyzer.user.domain.Role;
import com.resumeanalyzer.user.domain.User;
import com.resumeanalyzer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a default administrator account on first boot so the admin panel is reachable
 * out of the box. Credentials are configurable via environment variables and should be
 * changed immediately in any shared environment.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ADMIN_EMAIL:admin@resume-analyzer.dev}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:Admin@12345}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmailIgnoreCase(adminEmail)) {
            return;
        }
        User admin = User.builder()
                .name("Platform Admin")
                .email(adminEmail.toLowerCase())
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);
        log.warn("Seeded default ADMIN account [{}]. CHANGE THE PASSWORD IMMEDIATELY.", adminEmail);
    }
}

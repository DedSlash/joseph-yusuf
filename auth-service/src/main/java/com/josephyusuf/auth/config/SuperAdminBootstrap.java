package com.josephyusuf.auth.config;

import com.josephyusuf.auth.entity.Plan;
import com.josephyusuf.auth.entity.Role;
import com.josephyusuf.auth.entity.User;
import com.josephyusuf.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin-emails:}")
    private String superAdminEmails;

    @Value("${app.super-admin-default-password:#{null}}")
    private String defaultPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (superAdminEmails == null || superAdminEmails.isBlank()) {
            log.info("Aucun super-admin configuré (app.super-admin-emails vide)");
            return;
        }

        List<String> emails = Arrays.stream(superAdminEmails.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        for (String email : emails) {
            userRepository.findByEmail(email).ifPresentOrElse(
                    this::promoteIfNeeded,
                    () -> createSuperAdmin(email)
            );
        }
    }

    private void promoteIfNeeded(User user) {
        if (user.getRole() != Role.ADMIN) {
            user.setRole(Role.ADMIN);
            userRepository.save(user);
            log.info("Super-admin existant promu ADMIN : {}", user.getEmail());
        } else {
            log.info("Super-admin déjà ADMIN : {}", user.getEmail());
        }
    }

    private void createSuperAdmin(String email) {
        String rawPassword = defaultPassword != null ? defaultPassword : UUID.randomUUID().toString();
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .firstName("Super")
                .lastName("Admin")
                .plan(Plan.PREMIUM_PLUS)
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(user);
        if (defaultPassword != null) {
            log.warn("Super-admin créé : {} (mot de passe défini via APP_SUPER_ADMIN_DEFAULT_PASSWORD)", email);
        } else {
            log.warn("Super-admin créé : {} — mot de passe temporaire non affiché. " +
                    "Utilisez le flux de reset password pour définir un mot de passe.", email);
        }
    }
}

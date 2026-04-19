package com.nmcnpm.Homestay.config;

import org.springframework.beans.factory.annotation.Value;
import com.nmcnpm.Homestay.entity.Account;
import com.nmcnpm.Homestay.enums.AccountRole;
import com.nmcnpm.Homestay.enums.AccountStatus;
import com.nmcnpm.Homestay.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DemoDataInitializer {

    private static final DateTimeFormatter CREATED_LABEL_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email:}")
    private String adminEmail;

    @Value("${app.seed.admin-password:}")
    private String adminPassword;

    @Value("${app.seed.admin-name:System Admin}")
    private String adminName;

    @Bean
    public CommandLineRunner seedDemoData() {
        return args -> {
            String email = normalize(adminEmail);
            String password = normalize(adminPassword);

            if (email.isEmpty() || password.isEmpty()) {
                throw new IllegalStateException(
                        "Admin seed credentials are missing. Please set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD in backend .env"
                );
            }

            if (accountRepository.existsByEmail(email)) {
                return;
            }

            OffsetDateTime now = OffsetDateTime.now();
            Account admin = Account.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .fullName(adminName)
                    .role(AccountRole.MANAGER)
                    .status(AccountStatus.ACTIVE)
                    .createdLabel(now.format(CREATED_LABEL_FMT))
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            accountRepository.save(admin);
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}

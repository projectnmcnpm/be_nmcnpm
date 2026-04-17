package com.nmcnpm.Homestay.mapper;

import com.nmcnpm.Homestay.dto.response.AccountResponse;
import com.nmcnpm.Homestay.entity.Account;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Mapper thủ công Account entity -> AccountResponse DTO.
 *
 * Mapping spec:
 *   accounts.id           -> account.id   (UUID -> String)
 *   accounts.email        -> account.email
 *   accounts.full_name    -> account.name
 *   accounts.role         -> account.role  (lowercase enum)
 *   accounts.created_label hoặc format(created_at) -> account.created  ("dd/MM/yyyy")
 *   accounts.status       -> account.status (lowercase enum)
 */
@Component
public class AccountMapper {

    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public AccountResponse toResponse(Account account) {
        if (account == null) return null;

        // Ưu tiên dùng created_label nếu có, fallback format từ created_at
        String created = account.getCreatedLabel();
        if ((created == null || created.isBlank()) && account.getCreatedAt() != null) {
            created = account.getCreatedAt().format(LABEL_FMT);
        }

        return AccountResponse.builder()
                .id(account.getId() != null ? account.getId().toString() : null)
                .email(account.getEmail())
                .name(account.getFullName())
                .role(account.getRole() != null
                        ? account.getRole().name().toLowerCase() : null)
                .created(created)
                .status(account.getStatus() != null
                        ? account.getStatus().name().toLowerCase() : null)
                .build();
    }
}
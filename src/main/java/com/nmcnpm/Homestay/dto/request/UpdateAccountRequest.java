package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Body cho PATCH /api/accounts/{id}  — tất cả field optional (partial update)
 */
@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateAccountRequest {

    String name;

    @Email(message = "Email is invalid")
    String email;

    @Pattern(
            regexp = "^(manager|receptionist|cleaner|customer)$",
            message = "Role must be one of: manager, receptionist, cleaner, customer"
    )
    String role;

    @Pattern(
            regexp = "^(active|disabled)$",
            message = "Status must be active or disabled"
    )
    String status;

    @Size(min = 8, message = "Password must be at least 8 characters")
    String password;
}
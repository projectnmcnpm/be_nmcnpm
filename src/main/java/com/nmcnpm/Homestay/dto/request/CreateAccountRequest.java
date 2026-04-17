package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Body cho POST /api/accounts
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAccountRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    String email;

    @NotBlank(message = "Name is required")
    String name;

    @NotBlank(message = "Role is required")
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

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password;
}
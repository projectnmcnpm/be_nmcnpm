package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    String name;

    @Email(message = "Email is invalid")
    @NotBlank(message = "Email is required")
    String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8 , message = "Password must be at least 8 characters")
    String password;
}

package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Body cho POST /api/customers
 */
@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateCustomerRequest {

    @NotBlank(message = "Name is required")
    String name;

    @NotBlank(message = "Phone is required")
    @Pattern(
            regexp = "^(0\\d{9,10}|\\+84\\d{9,10})$",
            message = "Phone number format invalid"
    )
    String phone;

    @NotBlank(message = "CCCD is required")
    @Pattern(
            regexp = "^\\d{12}$",
            message = "CCCD must be 12 digits"
    )
    String cccd;

    @Email(message = "Email is invalid")
    String email;   // optional

    // Frontend có thể gửi "created" dạng string — bỏ qua, backend tự set created_at
    String created;

    // ISO-8601 string hoặc null
    String lastVisit;

    // Tailwind color class: "bg-blue-500", "bg-accent-neon"...
    String color;
}
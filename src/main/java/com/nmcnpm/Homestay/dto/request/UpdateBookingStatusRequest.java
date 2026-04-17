package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Body cho PATCH /api/bookings/{id}/status
 * { "status": "active" }
 */
@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateBookingStatusRequest {

    @NotBlank(message = "status is required")
    String status;
}
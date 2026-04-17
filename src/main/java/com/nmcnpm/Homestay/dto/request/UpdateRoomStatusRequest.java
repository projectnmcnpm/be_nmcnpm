package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Body cho PATCH /api/rooms/{id}/status
 * { "status": "available" }
 */
@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateRoomStatusRequest {

    @NotBlank(message = "Status is required")
    String status;
}
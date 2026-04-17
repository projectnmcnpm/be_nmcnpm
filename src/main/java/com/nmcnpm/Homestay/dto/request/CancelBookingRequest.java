package com.nmcnpm.Homestay.dto.request;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Body cho PATCH /api/bookings/{id}/cancel
 * { "reason": "Khong the di duoc" }
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelBookingRequest {
    String reason;
}
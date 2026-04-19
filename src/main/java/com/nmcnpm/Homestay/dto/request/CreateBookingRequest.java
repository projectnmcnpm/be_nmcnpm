package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Body cho POST /api/bookings
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateBookingRequest {

    @NotBlank(message = "roomId is required")
    String roomId;

    String roomName;

    // userId = email của customer (frontend đang dùng email làm userId)
    String userId;

    @NotBlank(message = "customerName is required")
    String customerName;

    @NotBlank(message = "customerEmail is required")
    @Email(message = "customerEmail format invalid")
    String customerEmail;

    @NotBlank(message = "customerPhone is required")
    @Pattern(
            regexp = "^(0\\d{9,10}|\\+84\\d{9,10})$",
            message = "customerPhone format invalid"
    )
    String customerPhone;

    @NotBlank(message = "customerIdNumber is required")
    @Pattern(
            regexp = "^\\d{12}$",
            message = "customerIdNumber must be 12 digits"
    )
    String customerIdNumber;

    @NotBlank(message = "checkIn is required")
    String checkIn;   // yyyy-MM-dd

    @NotBlank(message = "checkOut is required")
    String checkOut;  // yyyy-MM-dd

    String checkInTime;   // HH:mm
    String checkOutTime;  // HH:mm

    // "day" hoặc "hour"
    String bookingType;

    @NotNull(message = "total is required")
    @DecimalMin(value = "0", message = "total must be >= 0")
    BigDecimal total;

    // status ban đầu frontend có thể gửi, mặc định UPCOMING
    String status;

    String image;

    String paymentMethod;

    // "30" hoặc "100"
    String paymentAmount;

    String cancelReason;

    String note;
}
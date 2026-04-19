package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateBookingRequest {

    @NotBlank(message = "roomId is required")
    String roomId;

    String roomName;
    String userId;
    String customerName;
    String customerEmail;
    String customerPhone;
    String customerIdNumber;

    @NotBlank(message = "checkIn is required")
    String checkIn;

    @NotBlank(message = "checkOut is required")
    String checkOut;

    String checkInTime;
    String checkOutTime;
    String bookingType;

    @NotNull(message = "total is required")
    @DecimalMin(value = "0", message = "total must be >= 0")
    BigDecimal total;

    String note;
}
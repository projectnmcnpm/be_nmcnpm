package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateBookingPaymentStatusRequest {

    @NotBlank(message = "paymentStatus is required")
    String paymentStatus;
}

package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * DTO trả về cho frontend — tuân theo Booking DTO contract trong spec.
 * Mở rộng thêm các field QR payment (semi-dynamic).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookingResponse {

    String id;
    String roomId;
    String roomName;
    String userId;           // user_identifier (email)
    String customerName;
    String customerPhone;
    String customerIdNumber;
    String checkIn;          // yyyy-MM-dd
    String checkOut;         // yyyy-MM-dd
    String checkInTime;      // HH:mm
    String checkOutTime;     // HH:mm
    String bookingType;      // day | hour
    BigDecimal total;
    String status;           // upcoming | active | completed | cancelled
    String image;
    String paymentMethod;
    String paymentAmount;    // "30" hoặc "100" (phần trăm)
    String cancelReason;
    String createdAt;        // ISO-8601

    // ── QR Payment fields (chỉ có khi paymentMethod = bank) ──────────────────
    BigDecimal payAmountVnd;          // Số tiền thực tế cần chuyển (VND)
    String transferContent;           // Nội dung chuyển khoản đã khóa
    String paymentQrType;             // "semi_dynamic"
    Long paymentExpiresInSeconds;     // Số giây còn lại
    String paymentExpiresAt;          // ISO-8601 UTC
    String paymentQrUrl;              // URL ảnh QR
}
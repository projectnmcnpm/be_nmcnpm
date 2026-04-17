package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

/**
 * Response cho GET /api/bookings/{id}/payment-qr
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentQrResponse {

    String bookingId;
    String paymentMethod;        // bank / counter / cash / card / transfer
    Integer paymentPercent;      // 30 hoặc 100
    BigDecimal payAmountVnd;     // Số tiền thực tế cần chuyển (VND)
    String transferContent;      // Nội dung chuyển khoản cố định
    String paymentQrType;        // "semi_dynamic"
    Long paymentExpiresInSeconds;// Số giây còn lại (tính từ now)
    String paymentExpiresAt;     // ISO-8601 UTC

    // Bank metadata
    String bankBin;
    String bankAccountNo;
    String bankAccountName;

    String paymentQrUrl;         // URL ảnh QR (VietQR hoặc tương đương)
}
package com.nmcnpm.Homestay.mapper;

import com.nmcnpm.Homestay.dto.response.BookingResponse;
import com.nmcnpm.Homestay.entity.Booking;
import com.nmcnpm.Homestay.enums.BookingStatus;
import com.nmcnpm.Homestay.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Mapper thủ công Booking entity -> BookingResponse DTO.
 * Bao gồm cả QR payment fields khi paymentMethod = BANK.
 */
@Component
public class BookingMapper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Value("${payment.qr.bank-bin:970422}")
    private String bankBin;

    @Value("${payment.qr.account-no:123456789}")
    private String accountNo;

    @Value("${payment.qr.account-name:GENZ CINEMA}")
    private String accountName;

    @Value("${payment.qr.template:compact2}")
    private String qrTemplate;

    @Value("${payment.qr.provider-base-url:https://img.vietqr.io/image}")
    private String providerBaseUrl;

    public BookingResponse toResponse(Booking b) {
        if (b == null) return null;

        BookingResponse.BookingResponseBuilder builder = BookingResponse.builder()
                .id(b.getId() != null ? b.getId().toString() : null)
                .roomId(b.getRoom() != null ? b.getRoom().getId().toString() : null)
                .roomName(b.getRoomNameSnapshot())
                .userId(b.getUserIdentifier())
                .customerName(b.getCustomerNameSnapshot())
                .customerPhone(b.getCustomerPhone())
                .customerIdNumber(b.getCustomerIdNumber())
                .checkIn(b.getCheckInDate() != null ? b.getCheckInDate().format(DATE_FMT) : null)
                .checkOut(b.getCheckOutDate() != null ? b.getCheckOutDate().format(DATE_FMT) : null)
                .checkInTime(b.getCheckInTime() != null ? b.getCheckInTime().format(TIME_FMT) : null)
                .checkOutTime(b.getCheckOutTime() != null ? b.getCheckOutTime().format(TIME_FMT) : null)
                .bookingType(b.getBookingType() != null ? b.getBookingType().name().toLowerCase() : null)
                .total(b.getTotalAmount())
                .status(b.getStatus() != null ? b.getStatus().name().toLowerCase() : null)
                .paymentStatus(resolvePaymentStatus(b))
                .stayStatus(resolveStayStatus(b.getStatus()))
                .refundStatus(resolveRefundStatus(b))
                .image(b.getRoomImageSnapshot())
                .paymentMethod(b.getPaymentMethod() != null ? b.getPaymentMethod().name().toLowerCase() : null)
                .paymentAmount(b.getPaymentPercent() != null
                        ? String.valueOf(b.getPaymentPercent())
                        : (b.getPaymentAmount() != null ? b.getPaymentAmount().toPlainString() : null))
                .cancelReason(b.getCancelReason())
                .note(b.getNote())
                .createdAt(b.getCreatedAt() != null ? b.getCreatedAt().toString() : null);

        // Thêm QR fields nếu là thanh toán bank và có dữ liệu QR
        if (b.getPaymentMethod() == PaymentMethod.BANK && b.getPayAmountVnd() != null) {
            builder.payAmountVnd(b.getPayAmountVnd());
            builder.transferContent(b.getTransferContent());
            builder.paymentQrType("semi_dynamic");

            OffsetDateTime expiresAt = b.getPaymentExpiresAt();
            if (expiresAt != null) {
                long secondsLeft = ChronoUnit.SECONDS.between(OffsetDateTime.now(), expiresAt);
                builder.paymentExpiresInSeconds(Math.max(secondsLeft, 0L));
                builder.paymentExpiresAt(expiresAt.toString());
            }

            if (b.getPayAmountVnd() != null && b.getTransferContent() != null) {
                String encodedContent = java.net.URLEncoder.encode(
                        b.getTransferContent(), java.nio.charset.StandardCharsets.UTF_8);
                String encodedName = java.net.URLEncoder.encode(
                        accountName, java.nio.charset.StandardCharsets.UTF_8);
                String qrUrl = String.format("%s/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s",
                        providerBaseUrl, bankBin, accountNo, qrTemplate,
                        b.getPayAmountVnd().longValue(), encodedContent, encodedName);
                builder.paymentQrUrl(qrUrl);
            }
        }

        return builder.build();
    }

    private String resolvePaymentStatus(Booking b) {
        Integer percent = b.getPaymentPercent();
        if (percent == null || percent <= 0) {
            return "unpaid";
        }
        if (percent >= 100) {
            return "paid";
        }
        return "deposited";
    }

    private String resolveStayStatus(BookingStatus status) {
        if (status == null) {
            return "check_in";
        }
        return switch (status) {
            case UPCOMING -> "check_in";
            case CHECKED_IN -> "check_in";
            case IN_STAY -> "in_stay";
            case CHECKED_OUT -> "check_out";
            case CANCELLED -> "cancelled";
            case ACTIVE -> "in_stay";
            case COMPLETED -> "check_out";
        };
    }

    private String resolveRefundStatus(Booking b) {
        String note = b.getNote();
        if (note == null || note.isBlank()) {
            return "none";
        }
        if (note.contains("REFUND_STATUS=REFUNDED")) {
            return "refunded";
        }
        if (note.contains("REFUND_STATUS=ELIGIBLE")) {
            return "eligible";
        }
        if (note.contains("REFUND_STATUS=INELIGIBLE")) {
            return "ineligible";
        }
        return "none";
    }
}
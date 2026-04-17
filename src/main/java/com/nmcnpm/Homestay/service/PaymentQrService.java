package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.response.PaymentQrResponse;
import com.nmcnpm.Homestay.entity.Booking;
import com.nmcnpm.Homestay.enums.PaymentMethod;
import com.nmcnpm.Homestay.exception.AppException;
import com.nmcnpm.Homestay.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Service tạo semi-dynamic QR payment theo chuẩn VietQR.
 *
 * Chính sách:
 *  - QR chỉ sinh khi paymentMethod = BANK
 *  - Mỗi QR có hạn 5 phút kể từ thời điểm tạo booking
 *  - amount = totalAmount * paymentPercent / 100, làm tròn HALF_UP về đơn vị VND
 *  - transferContent = "Khach Hang dat phong {roomName}"
 *  - QR URL dùng VietQR img API: https://img.vietqr.io/image/{bankBin}-{accountNo}-{template}.png
 */
@Slf4j
@Service
public class PaymentQrService {

    // Thời gian hiệu lực QR (phút)
    private static final long QR_TTL_MINUTES = 5L;

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

    // -------------------------------------------------------------------------
    // Tính toán payment amount khi tạo booking
    // -------------------------------------------------------------------------

    /**
     * Tính số tiền VND cần thanh toán từ tổng tiền và phần trăm.
     * @param totalAmount  Tổng tiền booking
     * @param percent      30 hoặc 100
     * @return Số tiền VND làm tròn HALF_UP
     */
    public BigDecimal calcPayAmountVnd(BigDecimal totalAmount, int percent) {
        if (totalAmount == null) return BigDecimal.ZERO;
        return totalAmount
                .multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    /**
     * Tạo nội dung chuyển khoản cố định theo format chuẩn.
     * Loại bỏ dấu và ký tự đặc biệt để tương thích với hệ thống ngân hàng.
     * @param roomName  Tên phòng (vd: "RM-205", "Netflix & Chill Suite")
     */
    public String buildTransferContent(String roomName) {
        // Normalize: bỏ ký tự đặc biệt, chỉ giữ chữ/số/khoảng trắng/gạch ngang
        String normalizedRoom = roomName == null ? "ROOM"
                : roomName.replaceAll("[^a-zA-Z0-9\\s\\-]", "").trim();
        return "Khach Hang dat phong " + normalizedRoom;
    }

    /**
     * Tính thời điểm hết hạn QR (now + 5 phút).
     */
    public OffsetDateTime calcExpiresAt() {
        return OffsetDateTime.now().plusMinutes(QR_TTL_MINUTES);
    }

    // -------------------------------------------------------------------------
    // Sinh QR URL
    // -------------------------------------------------------------------------

    /**
     * Xây dựng URL QR semi-dynamic theo VietQR.
     * Format: {base}/{bankBin}-{accountNo}-{template}.png?amount={amount}&addInfo={content}&accountName={name}
     */
    public String buildQrUrl(BigDecimal amount, String transferContent) {
        String encodedContent = URLEncoder.encode(transferContent, StandardCharsets.UTF_8);
        String encodedName    = URLEncoder.encode(accountName, StandardCharsets.UTF_8);
        long   amountLong     = amount.longValue();

        return String.format("%s/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s",
                providerBaseUrl,
                bankBin,
                accountNo,
                qrTemplate,
                amountLong,
                encodedContent,
                encodedName
        );
    }

    // -------------------------------------------------------------------------
    // Tạo PaymentQrResponse từ Booking entity
    // -------------------------------------------------------------------------

    /**
     * Tạo response QR cho một booking.
     * Trả về null nếu paymentMethod không phải BANK.
     */
    public PaymentQrResponse buildQrResponse(Booking booking) {
        if (booking.getPaymentMethod() != PaymentMethod.BANK) {
            return null;
        }

        // Kiểm tra QR đã hết hạn chưa
        OffsetDateTime expiresAt = booking.getPaymentExpiresAt();
        if (expiresAt == null) {
            // Booking cũ chưa có expiresAt → tạo mới
            expiresAt = calcExpiresAt();
        }

        long secondsLeft = ChronoUnit.SECONDS.between(OffsetDateTime.now(), expiresAt);

        // Nếu hết hạn → gia hạn thêm 5 phút (refresh QR)
        if (secondsLeft <= 0) {
            expiresAt = calcExpiresAt();
            secondsLeft = QR_TTL_MINUTES * 60;
        }

        BigDecimal payAmount = booking.getPayAmountVnd() != null
                ? booking.getPayAmountVnd()
                : (booking.getPaymentPercent() != null
                ? calcPayAmountVnd(booking.getTotalAmount(), booking.getPaymentPercent())
                : booking.getTotalAmount());

        String transferContent = booking.getTransferContent() != null
                ? booking.getTransferContent()
                : buildTransferContent(booking.getRoomNameSnapshot());

        String qrUrl = buildQrUrl(payAmount, transferContent);

        return PaymentQrResponse.builder()
                .bookingId(booking.getId().toString())
                .paymentMethod(booking.getPaymentMethod().name().toLowerCase())
                .paymentPercent(booking.getPaymentPercent())
                .payAmountVnd(payAmount)
                .transferContent(transferContent)
                .paymentQrType("semi_dynamic")
                .paymentExpiresInSeconds(secondsLeft)
                .paymentExpiresAt(expiresAt.toString())
                .bankBin(bankBin)
                .bankAccountNo(accountNo)
                .bankAccountName(accountName)
                .paymentQrUrl(qrUrl)
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers để BookingService dùng khi tạo booking
    // -------------------------------------------------------------------------

    /**
     * Trả về true nếu paymentMethod là BANK và cần sinh QR.
     */
    public boolean requiresQr(String paymentMethodStr) {
        if (paymentMethodStr == null) return false;
        try {
            return PaymentMethod.valueOf(paymentMethodStr.toUpperCase()) == PaymentMethod.BANK;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
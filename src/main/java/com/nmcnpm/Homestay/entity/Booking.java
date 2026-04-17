package com.nmcnpm.Homestay.entity;

import com.nmcnpm.Homestay.enums.BookingStatus;
import com.nmcnpm.Homestay.enums.BookingType;
import com.nmcnpm.Homestay.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    UUID id;

    // -------------------------------------------------------------------------
    // Quan hệ
    // -------------------------------------------------------------------------

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_bookings_room"))
    Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id",
            foreignKey = @ForeignKey(name = "fk_bookings_customer"))
    Customer customer;

    /** Staff tạo booking thay cho khách (có thể null nếu khách tự đặt). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_account_id",
            foreignKey = @ForeignKey(name = "fk_bookings_created_by"))
    Account createdByAccount;

    // -------------------------------------------------------------------------
    // Định danh người dùng (frontend đang dùng email làm userId)
    // -------------------------------------------------------------------------

    @Column(name = "user_identifier", nullable = false, length = 255)
    String userIdentifier;

    // -------------------------------------------------------------------------
    // Snapshot thông tin tại thời điểm đặt (để hiển thị nhanh, giữ lịch sử)
    // -------------------------------------------------------------------------

    @Column(name = "room_name_snapshot", nullable = false, length = 255)
    String roomNameSnapshot;

    @Column(name = "room_image_snapshot", columnDefinition = "TEXT")
    String roomImageSnapshot;

    @Column(name = "customer_name_snapshot", length = 255)
    String customerNameSnapshot;

    @Column(name = "customer_phone", length = 20)
    String customerPhone;

    @Column(name = "customer_id_number", length = 20)
    String customerIdNumber;

    // -------------------------------------------------------------------------
    // Thời gian đặt
    // -------------------------------------------------------------------------

    @Column(name = "check_in_date", nullable = false)
    LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    LocalDate checkOutDate;

    /** Giờ check-in (HH:mm) — dùng cho bookingType = hour. */
    @Column(name = "check_in_time")
    LocalTime checkInTime;

    /** Giờ check-out (HH:mm) — dùng cho bookingType = hour. */
    @Column(name = "check_out_time")
    LocalTime checkOutTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_type", nullable = false)
    @Builder.Default
    BookingType bookingType = BookingType.DAY;

    // -------------------------------------------------------------------------
    // Thanh toán
    // -------------------------------------------------------------------------

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    BookingStatus status = BookingStatus.UPCOMING;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    PaymentMethod paymentMethod;

    /**
     * Phần trăm thanh toán trước: 30 hoặc 100.
     */
    @Column(name = "payment_percent")
    Integer paymentPercent;

    @Column(name = "payment_amount", precision = 14, scale = 2)
    BigDecimal paymentAmount;

    // -------------------------------------------------------------------------
    // QR Payment fields (semi-dynamic QR)
    // -------------------------------------------------------------------------

    /**
     * Số tiền VND thực tế cần chuyển khoản (totalAmount * paymentPercent / 100).
     * Được khóa cứng khi tạo booking, không thay đổi.
     */
    @Column(name = "pay_amount_vnd", precision = 14, scale = 2)
    BigDecimal payAmountVnd;

    /**
     * Nội dung chuyển khoản cố định, ví dụ: "Khach Hang dat phong RM-205".
     */
    @Column(name = "transfer_content", length = 255)
    String transferContent;

    /**
     * Thời điểm QR hết hạn (createdAt + 5 phút).
     * Null nếu paymentMethod != bank.
     */
    @Column(name = "payment_expires_at")
    OffsetDateTime paymentExpiresAt;

    // -------------------------------------------------------------------------
    // Khác
    // -------------------------------------------------------------------------

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    String cancelReason;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    OffsetDateTime deletedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
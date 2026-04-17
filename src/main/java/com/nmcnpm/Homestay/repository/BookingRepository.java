package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.Booking;
import com.nmcnpm.Homestay.enums.BookingStatus;
import com.nmcnpm.Homestay.enums.PaymentMethod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

    /**
     * Lọc bookings theo nhiều tiêu chí.
     * Tất cả param optional (null = bỏ qua điều kiện đó).
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE (:userIdentifier IS NULL OR b.userIdentifier = :userIdentifier)
              AND (:status        IS NULL OR b.status = :status)
              AND (:roomId        IS NULL OR b.room.id = :roomId)
              AND (:checkInFrom   IS NULL OR b.checkInDate >= :checkInFrom)
              AND (:checkInTo     IS NULL OR b.checkInDate <= :checkInTo)
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findAllByFilter(
            @Param("userIdentifier") String userIdentifier,
            @Param("status")         BookingStatus status,
            @Param("roomId")         UUID roomId,
            @Param("checkInFrom")    LocalDate checkInFrom,
            @Param("checkInTo")      LocalDate checkInTo,
            Pageable pageable
    );

    /**
     * Kiểm tra overlap lịch đặt phòng (dùng trước khi tạo booking mới).
     * Trả về true nếu có ít nhất 1 booking upcoming/active trùng khoảng ngày.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.room.id = :roomId
              AND b.deletedAt IS NULL
              AND b.status IN (
                  com.nmcnpm.Homestay.enums.BookingStatus.UPCOMING,
                  com.nmcnpm.Homestay.enums.BookingStatus.ACTIVE)
              AND b.checkInDate  <= :checkOut
              AND b.checkOutDate >= :checkIn
            """)
    boolean hasConflict(
            @Param("roomId")   UUID roomId,
            @Param("checkIn")  LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    /**
     * Đếm booking của 1 customer (dùng để tính booking_count).
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.customer.id = :customerId
              AND b.deletedAt IS NULL
              AND b.status IN (
                  com.nmcnpm.Homestay.enums.BookingStatus.UPCOMING,
                  com.nmcnpm.Homestay.enums.BookingStatus.ACTIVE,
                  com.nmcnpm.Homestay.enums.BookingStatus.COMPLETED)
            """)
    long countByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Đếm số booking UPCOMING (chưa thanh toán) của một user (theo email/userIdentifier).
     * Dùng để giới hạn số booking pending thanh toán đồng thời (chống giu phong ao).
     */
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.userIdentifier = :userIdentifier
              AND b.deletedAt IS NULL
              AND b.status = com.nmcnpm.Homestay.enums.BookingStatus.UPCOMING
              AND b.paymentMethod = :paymentMethod
            """)
    long countPendingPaymentByUser(
            @Param("userIdentifier") String userIdentifier,
            @Param("paymentMethod")  PaymentMethod paymentMethod
    );
}
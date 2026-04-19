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
import java.util.List;
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
                                                        AND (:onlyConfirmedPayment = false OR COALESCE(b.paymentPercent, 0) > 0)
              AND (:status        IS NULL OR b.status = :status)
              AND (:roomId        IS NULL OR b.room.id = :roomId)
              AND (:checkInFrom   IS NULL OR b.checkInDate >= :checkInFrom)
              AND (:checkInTo     IS NULL OR b.checkInDate <= :checkInTo)
            ORDER BY b.createdAt DESC
            """)
    Page<Booking> findAllByFilter(
            @Param("userIdentifier") String userIdentifier,
            @Param("onlyConfirmedPayment") boolean onlyConfirmedPayment,
            @Param("status")         BookingStatus status,
            @Param("roomId")         UUID roomId,
            @Param("checkInFrom")    LocalDate checkInFrom,
            @Param("checkInTo")      LocalDate checkInTo,
            Pageable pageable
    );

    /**
     * Kiểm tra overlap lịch đặt phòng (dùng trước khi tạo booking mới).
     * Trả về true nếu có ít nhất 1 booking upcoming/checked_in/in_stay trùng khoảng ngày.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.room.id = :roomId
              AND b.deletedAt IS NULL
              AND b.status IN (
                  com.nmcnpm.Homestay.enums.BookingStatus.UPCOMING,
                                  com.nmcnpm.Homestay.enums.BookingStatus.CHECKED_IN,
                                  com.nmcnpm.Homestay.enums.BookingStatus.IN_STAY,
                                  com.nmcnpm.Homestay.enums.BookingStatus.ACTIVE)
              AND b.checkInDate  <= :checkOut
              AND b.checkOutDate >= :checkIn
            """)
    boolean hasConflict(
            @Param("roomId")   UUID roomId,
            @Param("checkIn")  LocalDate checkIn,
            @Param("checkOut") LocalDate checkOut
    );

    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.id <> :bookingId
              AND b.room.id = :roomId
              AND b.deletedAt IS NULL
              AND b.status IN (
                  com.nmcnpm.Homestay.enums.BookingStatus.UPCOMING,
                  com.nmcnpm.Homestay.enums.BookingStatus.CHECKED_IN,
                  com.nmcnpm.Homestay.enums.BookingStatus.IN_STAY,
                  com.nmcnpm.Homestay.enums.BookingStatus.ACTIVE)
              AND b.checkInDate  <= :checkOut
              AND b.checkOutDate >= :checkIn
            """)
    boolean hasConflictExcludingBooking(
            @Param("bookingId") UUID bookingId,
            @Param("roomId") UUID roomId,
            @Param("checkIn") LocalDate checkIn,
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
                                  com.nmcnpm.Homestay.enums.BookingStatus.CHECKED_IN,
                                  com.nmcnpm.Homestay.enums.BookingStatus.IN_STAY,
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

    @Query("""
            SELECT b.userIdentifier FROM Booking b
            WHERE b.customer.id = :customerId
              AND b.deletedAt IS NULL
              AND b.userIdentifier IS NOT NULL
              AND b.userIdentifier <> ''
            ORDER BY b.createdAt DESC
            """)
    Page<String> findRecentUserIdentifiersByCustomerId(
            @Param("customerId") UUID customerId,
            Pageable pageable
    );

    @Query("""
            SELECT b FROM Booking b
            WHERE b.room.id = :roomId
              AND b.deletedAt IS NULL
              AND b.status IN (
                  com.nmcnpm.Homestay.enums.BookingStatus.UPCOMING,
                  com.nmcnpm.Homestay.enums.BookingStatus.CHECKED_IN,
                  com.nmcnpm.Homestay.enums.BookingStatus.IN_STAY,
                  com.nmcnpm.Homestay.enums.BookingStatus.ACTIVE)
              AND b.checkInDate <= :toDate
              AND b.checkOutDate >= :fromDate
            ORDER BY b.checkInDate ASC, b.checkInTime ASC, b.checkOutDate ASC, b.checkOutTime ASC
            """)
    List<Booking> findForRoomAvailability(
            @Param("roomId") UUID roomId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );
}
package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.Booking;
import com.nmcnpm.Homestay.enums.BookingStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Repository riêng chứa các query tổng hợp cho dashboard.
 *
 * FIX: Thêm LEFT JOIN FETCH b.room và LEFT JOIN FETCH b.customer vào
 * findRecentBookings và findByStatusOrdered để tránh N+1 query ngầm.
 * BookingMapper truy cập b.getRoom().getId() / b.getRoom().getRoomName()
 * và b.getCustomer() nên cần eager load trong cùng 1 query.
 */
@Repository
public interface DashboardRepository extends JpaRepository<Booking, UUID> {

    // ------------------------------------------------------------------
    // Tổng doanh thu từ booking đã thanh toán đủ và chưa hoàn tiền
    // ------------------------------------------------------------------
    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM Booking b
            WHERE b.deletedAt IS NULL
              AND b.paymentPercent = 100
              AND (b.note IS NULL OR UPPER(b.note) NOT LIKE '%REFUND_STATUS=REFUNDED%')
            """)
    BigDecimal sumCompletedRevenue();

    // ------------------------------------------------------------------
    // Tổng số booking (chưa xóa)
    // ------------------------------------------------------------------
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.deletedAt IS NULL
            """)
    long countAllBookings();

    // ------------------------------------------------------------------
    // Đếm booking theo status
    // ------------------------------------------------------------------
    @Query("""
            SELECT COUNT(b) FROM Booking b
            WHERE b.deletedAt IS NULL
              AND b.status = :status
            """)
    long countByStatus(@Param("status") BookingStatus status);

    // ------------------------------------------------------------------
    // Recent bookings (N bản ghi mới nhất)
    // FIX: Thêm LEFT JOIN FETCH room + customer để tránh N+1 query.
    // BookingMapper gọi b.getRoom().getId() và b.getRoom().getRoomName()
    // nên bắt buộc phải fetch room trong cùng query này.
    // ------------------------------------------------------------------
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.room
            LEFT JOIN FETCH b.customer
            WHERE b.deletedAt IS NULL
            ORDER BY b.createdAt DESC
            """)
    List<Booking> findRecentBookings(PageRequest pageRequest);

    // ------------------------------------------------------------------
    // Bookings theo status, sắp xếp theo checkInDate ASC (upcoming/active list)
    // FIX: Thêm LEFT JOIN FETCH room + customer để tránh N+1 query.
    // ------------------------------------------------------------------
    @Query("""
            SELECT DISTINCT b FROM Booking b
            LEFT JOIN FETCH b.room
            LEFT JOIN FETCH b.customer
            WHERE b.deletedAt IS NULL
              AND b.status = :status
            ORDER BY b.checkInDate ASC
            """)
    List<Booking> findByStatusOrdered(
            @Param("status") BookingStatus status,
            PageRequest pageRequest
    );
}
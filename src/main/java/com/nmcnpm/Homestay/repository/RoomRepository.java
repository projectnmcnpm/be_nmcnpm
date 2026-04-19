package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.Room;
import com.nmcnpm.Homestay.enums.RoomStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    /**
     * Lọc phòng theo status + keyword, trả về Page để hỗ trợ phân trang.
     * - status = null  -> không lọc theo status
     * - search = null  -> không lọc theo keyword
     */
    @Query("""
        SELECT r FROM Room r
        WHERE (:status IS NULL OR r.status = :status)
          AND (:search IS NULL OR
               LOWER(r.roomName) LIKE :search OR
               LOWER(r.roomType) LIKE :search)
        """)
    Page<Room> findAllByFilter(
            @Param("status") RoomStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Kiểm tra có booking upcoming/active cho room không.
     * Dùng trước khi soft-delete để tránh xóa phòng đang được đặt.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM Booking b
            WHERE b.room.id = :roomId
              AND b.deletedAt IS NULL
              AND b.status IN (
                  com.nmcnpm.Homestay.enums.BookingStatus.UPCOMING,
                  com.nmcnpm.Homestay.enums.BookingStatus.CHECKED_IN,
                  com.nmcnpm.Homestay.enums.BookingStatus.IN_STAY)
            """)
    boolean hasActiveOrUpcomingBookings(@Param("roomId") UUID roomId);
}
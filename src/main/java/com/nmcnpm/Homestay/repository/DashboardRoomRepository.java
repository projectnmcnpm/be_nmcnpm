package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.Room;
import com.nmcnpm.Homestay.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository bổ sung cho Room — dùng trong DashboardService.
 * Tách riêng để không làm "béo" RoomRepository chính.
 */
@Repository
public interface DashboardRoomRepository extends JpaRepository<Room, UUID> {

    // ------------------------------------------------------------------
    // Đếm phòng theo status (chưa bị xóa)
    // ------------------------------------------------------------------
    @Query("""
            SELECT COUNT(r) FROM Room r
            WHERE r.deletedAt IS NULL
              AND r.status = :status
            """)
    long countByStatus(@Param("status") RoomStatus status);

    // ------------------------------------------------------------------
    // Tổng số phòng chưa xóa
    // ------------------------------------------------------------------
    @Query("""
            SELECT COUNT(r) FROM Room r
            WHERE r.deletedAt IS NULL
            """)
    long countTotal();
}
package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.CleanerTask;
import com.nmcnpm.Homestay.enums.CleanerTaskState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CleanerTaskRepository extends JpaRepository<CleanerTask, Long> {

    /**
     * Lấy tất cả task chưa bị xóa, sắp xếp mới nhất trước.
     * Manager xem tất cả; Cleaner lọc thêm theo cleanerAccountId ở service layer.
     */
    @Query("""
            SELECT t FROM CleanerTask t
            LEFT JOIN FETCH t.room
            LEFT JOIN FETCH t.cleanerAccount
            WHERE t.deletedAt IS NULL
            ORDER BY t.createdAt DESC
            """)
    List<CleanerTask> findAllActive();

    /**
     * Lấy task theo cleaner account id (cho cleaner tự xem việc của mình).
     */
    @Query("""
            SELECT t FROM CleanerTask t
            LEFT JOIN FETCH t.room
            LEFT JOIN FETCH t.cleanerAccount
            WHERE t.deletedAt IS NULL
              AND t.cleanerAccount.id = :accountId
            ORDER BY t.createdAt DESC
            """)
    List<CleanerTask> findByCleanerAccountId(@Param("accountId") UUID accountId);

    /**
     * Lấy task theo state.
     */
    @Query("""
            SELECT t FROM CleanerTask t
            LEFT JOIN FETCH t.room
            LEFT JOIN FETCH t.cleanerAccount
            WHERE t.deletedAt IS NULL
              AND t.state = :state
            ORDER BY t.createdAt DESC
            """)
    List<CleanerTask> findByState(@Param("state") CleanerTaskState state);

    /**
     * Kiểm tra room đang có task chưa hoàn thành.
     */
    @Query("""
            SELECT COUNT(t) > 0 FROM CleanerTask t
            WHERE t.deletedAt IS NULL
              AND t.room.id = :roomId
              AND t.state IN (
                  com.nmcnpm.Homestay.enums.CleanerTaskState.PENDING,
                  com.nmcnpm.Homestay.enums.CleanerTaskState.IN_PROGRESS)
            """)
    boolean hasActiveTaskForRoom(@Param("roomId") UUID roomId);
}
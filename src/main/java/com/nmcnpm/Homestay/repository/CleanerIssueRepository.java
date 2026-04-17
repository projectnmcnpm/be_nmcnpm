package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.CleanerIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CleanerIssueRepository extends JpaRepository<CleanerIssue, Long> {

    /**
     * Lấy tất cả issue chưa bị xóa — dùng cho manager.
     */
    @Query("""
            SELECT i FROM CleanerIssue i
            LEFT JOIN FETCH i.room
            LEFT JOIN FETCH i.cleanerAccount
            LEFT JOIN FETCH i.cleanerTask
            WHERE i.deletedAt IS NULL
            ORDER BY i.reportedAt DESC
            """)
    List<CleanerIssue> findAllActive();

    /**
     * Lấy issue của một cleaner cụ thể.
     */
    @Query("""
            SELECT i FROM CleanerIssue i
            LEFT JOIN FETCH i.room
            LEFT JOIN FETCH i.cleanerAccount
            LEFT JOIN FETCH i.cleanerTask
            WHERE i.deletedAt IS NULL
              AND i.cleanerAccount.id = :accountId
            ORDER BY i.reportedAt DESC
            """)
    List<CleanerIssue> findByCleanerAccountId(@Param("accountId") UUID accountId);
}
package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Repository bổ sung cho Customer — dùng trong DashboardService.
 */
@Repository
public interface DashboardCustomerRepository extends JpaRepository<Customer, UUID> {

    // ------------------------------------------------------------------
    // Đếm customer tạo mới từ một mốc thời gian (dùng để tính newCustomers)
    // ------------------------------------------------------------------
    @Query("""
            SELECT COUNT(c) FROM Customer c
            WHERE c.deletedAt IS NULL
              AND c.createdAt >= :since
            """)
    long countNewCustomersSince(@Param("since") OffsetDateTime since);
}
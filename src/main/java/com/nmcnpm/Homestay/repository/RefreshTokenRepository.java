package com.nmcnpm.Homestay.repository;

import com.nmcnpm.Homestay.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    /** Thu hồi toàn bộ token của 1 account (dùng khi logout). */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.account.id = :accountId AND r.revoked = false")
    void revokeAllByAccountId(@Param("accountId") UUID accountId);

    /** Dọn dẹp token hết hạn hoặc đã revoked (có thể chạy định kỳ). */
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now OR r.revoked = true")
    void deleteExpiredAndRevoked(@Param("now") OffsetDateTime now);
}
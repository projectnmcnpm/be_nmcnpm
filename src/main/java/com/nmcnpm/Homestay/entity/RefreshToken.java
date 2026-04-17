package com.nmcnpm.Homestay.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Lưu refresh token vào DB để:
 *  - Kiểm tra hợp lệ khi cấp lại access token
 *  - Thu hồi (revoke) khi logout
 *  - Tự động hết hạn sau 7 ngày
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    UUID id;

    /** Token string (UUID ngẫu nhiên — không phải JWT). */
    @Column(name = "token", nullable = false, unique = true, length = 255)
    String token;

    /** Account sở hữu token này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_tokens_account"))
    Account account;

    /** Thời điểm hết hạn (mặc định: now + 7 ngày). */
    @Column(name = "expires_at", nullable = false)
    OffsetDateTime expiresAt;

    /** Đã bị thu hồi (logout). */
    @Column(name = "revoked", nullable = false)
    @Builder.Default
    boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    OffsetDateTime createdAt = OffsetDateTime.now();

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }
}
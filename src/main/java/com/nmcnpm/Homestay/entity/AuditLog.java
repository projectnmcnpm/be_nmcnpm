package com.nmcnpm.Homestay.entity;

import com.nmcnpm.Homestay.enums.AccountRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_account_id",
            foreignKey = @ForeignKey(name = "fk_audit_logs_account"))
    Account actorAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role")
    AccountRole actorRole;

    /** Hành động: CREATE, UPDATE, DELETE, CANCEL, STATUS_CHANGE... */
    @Column(name = "action", nullable = false, length = 50)
    String action;

    /** Tên entity: rooms, bookings, accounts, customers... */
    @Column(name = "entity_name", nullable = false, length = 50)
    String entityName;

    /** ID của entity bị tác động. */
    @Column(name = "entity_id", nullable = false, length = 50)
    String entityId;

    /** Trạng thái trước khi thay đổi — lưu JSONB. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data", columnDefinition = "jsonb")
    Map<String, Object> oldData;

    /** Trạng thái sau khi thay đổi — lưu JSONB. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data", columnDefinition = "jsonb")
    Map<String, Object> newData;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}

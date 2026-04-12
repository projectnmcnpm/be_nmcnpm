package com.nmcnpm.Homestay.entity;


import com.nmcnpm.Homestay.enums.AccountRole;
import com.nmcnpm.Homestay.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "accounts")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    String passwordHash;

    @Column(name = "full_name", nullable = false, length = 255)
    String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    AccountRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "created_label", length = 30)
    String createdLabel;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "deleted_at")
    OffsetDateTime deletedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}

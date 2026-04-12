package com.nmcnpm.Homestay.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "customers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_customers_phone", columnNames = "phone"),
                @UniqueConstraint(name = "uq_customers_cccd",  columnNames = "cccd")
        }
)
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "full_name", nullable = false, length = 255)
    String fullName;

    @Column(name = "phone", nullable = false, length = 20)
    String phone;

    @Column(name = "cccd", nullable = false, length = 20)
    String cccd;

    @Column(name = "email", length = 255)
    String email;

    @Column(name = "booking_count", nullable = false)
    @Builder.Default
    Integer bookingCount = 0;

    @Column(name = "joined_label", length = 30)
    String joinedLabel;

    @Column(name = "last_visit_at")
    OffsetDateTime lastVisitAt;

    @Column(name = "color_tag", length = 40)
    String colorTag;

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

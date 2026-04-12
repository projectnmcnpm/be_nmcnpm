package com.nmcnpm.Homestay.entity;

import com.nmcnpm.Homestay.enums.RoomStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    UUID id;

    @Column(name = "room_name", nullable = false, length = 255)
    String roomName;

    @Column(name = "room_type", nullable = false, length = 50)
    String roomType;

    @Column(name = "capacity")
    Integer capacity;

    @Column(name = "price_per_night", nullable = false, precision = 14, scale = 2)
    BigDecimal pricePerNight;

    @Column(name = "price_per_hour", precision = 14, scale = 2)
    BigDecimal pricePerHour;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    RoomStatus status = RoomStatus.AVAILABLE;

    @Column(name = "cover_image_url", nullable = false, columnDefinition = "TEXT")
    private String coverImageUrl;

    /** Danh sách URL ảnh gallery — lưu JSONB array. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gallery_urls", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    List<String> galleryUrls = new ArrayList<>();

    /** Danh sách tiện nghi — lưu JSONB array (vd: ["2 Người", "Máy chiếu 4K"]). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "amenities", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    List<String> amenities = new ArrayList<>();

    @Column(name = "description", columnDefinition = "TEXT")
    String description;

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

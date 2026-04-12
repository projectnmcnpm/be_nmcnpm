package com.nmcnpm.Homestay.entity;

import com.nmcnpm.Homestay.enums.CleanerTaskState;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cleaner_tasks")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CleanerTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cleaner_tasks_room"))
    Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleaner_account_id",
            foreignKey = @ForeignKey(name = "fk_cleaner_tasks_account"))
    Account cleanerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    @Builder.Default
    CleanerTaskState state = CleanerTaskState.PENDING;

    @Column(name = "started_at")
    OffsetDateTime startedAt;

    @Column(name = "completed_at")
    OffsetDateTime completedAt;

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

package com.nmcnpm.Homestay.entity;

import com.nmcnpm.Homestay.enums.CleanerIssueStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

@Entity
@Table(name = "cleaner_issues")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CleanerIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleaner_task_id",
            foreignKey = @ForeignKey(name = "fk_cleaner_issues_task"))
    CleanerTask cleanerTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cleaner_issues_room"))
    Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cleaner_account_id",
            foreignKey = @ForeignKey(name = "fk_cleaner_issues_account"))
    Account cleanerAccount;

    @Column(name = "reason", nullable = false, length = 255)
    String reason;

    @Column(name = "note", columnDefinition = "TEXT")
    String note;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    CleanerIssueStatus status = CleanerIssueStatus.REPORTED;

    @Column(name = "reported_at", nullable = false)
    @Builder.Default
    OffsetDateTime reportedAt = OffsetDateTime.now();

    @Column(name = "resolved_at")
    OffsetDateTime resolvedAt;

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

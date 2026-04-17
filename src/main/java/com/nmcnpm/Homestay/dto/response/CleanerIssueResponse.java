package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO trả về cho cleaner issue.
 * {
 *   "id": 1,
 *   "taskId": 2,
 *   "roomId": "uuid...",
 *   "roomName": "Netflix & Chill Suite",
 *   "cleanerAccountId": "uuid...",
 *   "cleanerName": "Nguyen Van A",
 *   "reason": "Can kiem tra thiet bi",
 *   "note": "Den hong",
 *   "status": "reported",
 *   "reportedAt": "2026-04-09T10:30:00Z",
 *   "resolvedAt": null
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CleanerIssueResponse {

    Long   id;
    Long   taskId;
    String roomId;
    String roomName;
    String cleanerAccountId;
    String cleanerName;
    String reason;
    String note;
    String status;      // reported | acknowledged | resolved
    String reportedAt;  // ISO-8601
    String resolvedAt;  // ISO-8601
}
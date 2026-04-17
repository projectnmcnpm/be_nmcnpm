package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * DTO trả về cho cleaner task.
 * {
 *   "id": 1,
 *   "roomId": "uuid...",
 *   "roomName": "Netflix & Chill Suite",
 *   "roomStatus": "cleaning",
 *   "cleanerAccountId": "uuid...",
 *   "cleanerName": "Nguyen Van A",
 *   "state": "pending",
 *   "startedAt": "2026-04-09T10:30:00Z",
 *   "completedAt": null,
 *   "createdAt": "2026-04-09T10:00:00Z"
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CleanerTaskResponse {

    Long   id;
    String roomId;
    String roomName;
    String roomStatus;        // current room status
    String cleanerAccountId;
    String cleanerName;
    String state;             // pending | in_progress | completed | issue
    String startedAt;         // ISO-8601
    String completedAt;       // ISO-8601
    String createdAt;         // ISO-8601
}
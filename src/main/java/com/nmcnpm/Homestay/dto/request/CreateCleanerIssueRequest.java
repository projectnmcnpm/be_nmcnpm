package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Body cho POST /api/cleaner/issues
 * {
 *   "roomId": "RM-101",
 *   "reason": "Can kiem tra thiet bi",
 *   "note": "Den hong"
 * }
 */
@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class CreateCleanerIssueRequest {

    @NotBlank(message = "roomId is required")
    String roomId;

    @NotBlank(message = "reason is required")
    String reason;

    String note;

    /** taskId tuỳ chọn — nếu cleaner báo issue gắn với 1 task cụ thể */
    Long taskId;
}
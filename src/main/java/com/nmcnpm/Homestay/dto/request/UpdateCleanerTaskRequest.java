package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Body cho PATCH /api/cleaner/tasks/{id}
 * { "state": "in_progress" }
 *
 * Valid states: pending | in_progress | completed | issue
 */
@Getter
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class UpdateCleanerTaskRequest {

    @NotBlank(message = "state is required")
    String state;
}
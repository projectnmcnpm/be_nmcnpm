package com.nmcnpm.Homestay.mapper;

import com.nmcnpm.Homestay.dto.response.CleanerIssueResponse;
import com.nmcnpm.Homestay.dto.response.CleanerTaskResponse;
import com.nmcnpm.Homestay.entity.CleanerIssue;
import com.nmcnpm.Homestay.entity.CleanerTask;
import org.springframework.stereotype.Component;

/**
 * Mapper thủ công CleanerTask / CleanerIssue entity -> Response DTO.
 */
@Component
public class CleanerMapper {

    // ------------------------------------------------------------------
    // CleanerTask -> CleanerTaskResponse
    // ------------------------------------------------------------------
    public CleanerTaskResponse toTaskResponse(CleanerTask task) {
        if (task == null) return null;

        return CleanerTaskResponse.builder()
                .id(task.getId())
                .roomId(task.getRoom() != null
                        ? task.getRoom().getId().toString() : null)
                .roomName(task.getRoom() != null
                        ? task.getRoom().getRoomName() : null)
                .roomStatus(task.getRoom() != null && task.getRoom().getStatus() != null
                        ? task.getRoom().getStatus().name().toLowerCase() : null)
                .cleanerAccountId(task.getCleanerAccount() != null
                        ? task.getCleanerAccount().getId().toString() : null)
                .cleanerName(task.getCleanerAccount() != null
                        ? task.getCleanerAccount().getFullName() : null)
                .state(task.getState() != null
                        ? task.getState().name().toLowerCase() : null)
                .startedAt(task.getStartedAt() != null
                        ? task.getStartedAt().toString() : null)
                .completedAt(task.getCompletedAt() != null
                        ? task.getCompletedAt().toString() : null)
                .createdAt(task.getCreatedAt() != null
                        ? task.getCreatedAt().toString() : null)
                .build();
    }

    // ------------------------------------------------------------------
    // CleanerIssue -> CleanerIssueResponse
    // ------------------------------------------------------------------
    public CleanerIssueResponse toIssueResponse(CleanerIssue issue) {
        if (issue == null) return null;

        return CleanerIssueResponse.builder()
                .id(issue.getId())
                .taskId(issue.getCleanerTask() != null
                        ? issue.getCleanerTask().getId() : null)
                .roomId(issue.getRoom() != null
                        ? issue.getRoom().getId().toString() : null)
                .roomName(issue.getRoom() != null
                        ? issue.getRoom().getRoomName() : null)
                .cleanerAccountId(issue.getCleanerAccount() != null
                        ? issue.getCleanerAccount().getId().toString() : null)
                .cleanerName(issue.getCleanerAccount() != null
                        ? issue.getCleanerAccount().getFullName() : null)
                .reason(issue.getReason())
                .note(issue.getNote())
                .status(issue.getStatus() != null
                        ? issue.getStatus().name().toLowerCase() : null)
                .reportedAt(issue.getReportedAt() != null
                        ? issue.getReportedAt().toString() : null)
                .resolvedAt(issue.getResolvedAt() != null
                        ? issue.getResolvedAt().toString() : null)
                .build();
    }
}
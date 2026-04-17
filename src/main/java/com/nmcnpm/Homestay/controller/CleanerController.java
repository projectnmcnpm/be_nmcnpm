package com.nmcnpm.Homestay.controller;

import com.nmcnpm.Homestay.dto.request.CreateCleanerIssueRequest;
import com.nmcnpm.Homestay.dto.request.UpdateCleanerTaskRequest;
import com.nmcnpm.Homestay.dto.response.ApiResponse;
import com.nmcnpm.Homestay.dto.response.CleanerIssueResponse;
import com.nmcnpm.Homestay.dto.response.CleanerTaskResponse;
import com.nmcnpm.Homestay.service.CleanerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cleaner workflow endpoints:
 *
 *   GET    /api/cleaner/tasks          CLEANER / MANAGER
 *   PATCH  /api/cleaner/tasks/{id}     CLEANER / MANAGER
 *   POST   /api/cleaner/issues         CLEANER / MANAGER
 *
 * State machine cho task:
 *   pending -> in_progress
 *   in_progress -> completed | issue
 *   issue -> in_progress | completed
 *
 * Khi task -> completed:
 *   room.status cleaning -> available (trong cùng transaction)
 */
@RestController
@RequestMapping("/api/cleaner")
@RequiredArgsConstructor
public class CleanerController {

    private final CleanerService cleanerService;

    // ------------------------------------------------------------------
    // GET /api/cleaner/tasks
    //   - Manager: thấy tất cả task
    //   - Cleaner: chỉ thấy task được gán cho mình
    // ------------------------------------------------------------------
    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('CLEANER', 'MANAGER')")
    public ApiResponse<List<CleanerTaskResponse>> getAllTasks() {
        return ApiResponse.success(cleanerService.getAllTasks());
    }

    // ------------------------------------------------------------------
    // PATCH /api/cleaner/tasks/{id}
    // Body: { "state": "in_progress" }
    // ------------------------------------------------------------------
    @PatchMapping("/tasks/{id}")
    @PreAuthorize("hasAnyRole('CLEANER', 'MANAGER')")
    public ApiResponse<CleanerTaskResponse> updateTaskState(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCleanerTaskRequest request
    ) {
        return ApiResponse.success(cleanerService.updateTaskState(id, request));
    }

    // ------------------------------------------------------------------
    // POST /api/cleaner/issues
    // Body: { "roomId": "...", "reason": "...", "note": "..." }
    // ------------------------------------------------------------------
    @PostMapping("/issues")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CLEANER', 'MANAGER')")
    public ApiResponse<CleanerIssueResponse> createIssue(
            @Valid @RequestBody CreateCleanerIssueRequest request
    ) {
        return ApiResponse.success(
                cleanerService.createIssue(request),
                "Issue reported successfully"
        );
    }
}
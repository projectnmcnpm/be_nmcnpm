package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.request.CreateCleanerIssueRequest;
import com.nmcnpm.Homestay.dto.request.UpdateCleanerTaskRequest;
import com.nmcnpm.Homestay.dto.response.CleanerIssueResponse;
import com.nmcnpm.Homestay.dto.response.CleanerTaskResponse;
import com.nmcnpm.Homestay.entity.Account;
import com.nmcnpm.Homestay.entity.CleanerIssue;
import com.nmcnpm.Homestay.entity.CleanerTask;
import com.nmcnpm.Homestay.entity.Room;
import com.nmcnpm.Homestay.enums.CleanerIssueStatus;
import com.nmcnpm.Homestay.enums.CleanerTaskState;
import com.nmcnpm.Homestay.enums.RoomStatus;
import com.nmcnpm.Homestay.exception.AppException;
import com.nmcnpm.Homestay.exception.ErrorCode;
import com.nmcnpm.Homestay.mapper.CleanerMapper;
import com.nmcnpm.Homestay.repository.AccountRepository;
import com.nmcnpm.Homestay.repository.CleanerIssueRepository;
import com.nmcnpm.Homestay.repository.CleanerTaskRepository;
import com.nmcnpm.Homestay.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CleanerService {

    private final CleanerTaskRepository  cleanerTaskRepository;
    private final CleanerIssueRepository cleanerIssueRepository;
    private final RoomRepository         roomRepository;
    private final AccountRepository      accountRepository;
    private final CleanerMapper          cleanerMapper;

    // -------------------------------------------------------------------------
    // GET /api/cleaner/tasks
    // Manager: xem tất cả task.
    // Cleaner: chỉ xem task được gán cho mình.
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    public List<CleanerTaskResponse> getAllTasks() {
        String role = currentRole();
        List<CleanerTask> tasks;

        if ("CLEANER".equalsIgnoreCase(role)) {
            Account account = currentAccount();
            tasks = cleanerTaskRepository.findByCleanerAccountId(account.getId());
        } else {
            // MANAGER (và RECEPTIONIST nếu mở quyền sau)
            tasks = cleanerTaskRepository.findAllActive();
        }

        return tasks.stream()
                .map(cleanerMapper::toTaskResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // PATCH /api/cleaner/tasks/{id}
    //
    // Nghiệp vụ quan trọng:
    //   - in_progress  -> set startedAt
    //   - completed    -> set completedAt + đổi room status cleaning -> available
    //   - issue        -> chuyển state (cleaner sẽ POST /issues riêng để ghi chi tiết)
    // -------------------------------------------------------------------------
    @Transactional
    public CleanerTaskResponse updateTaskState(Long id, UpdateCleanerTaskRequest request) {
        CleanerTask task = cleanerTaskRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR,
                        "Cleaner task not found: " + id));

        // Kiểm tra cleaner chỉ sửa task của mình (bỏ qua cho manager)
        String role = currentRole();
        if ("CLEANER".equalsIgnoreCase(role)) {
            Account account = currentAccount();
            if (task.getCleanerAccount() == null
                    || !task.getCleanerAccount().getId().equals(account.getId())) {
                throw new AppException(ErrorCode.FORBIDDEN,
                        "You can only update your own tasks");
            }
        }

        CleanerTaskState newState = parseStateOrThrow(request.getState());
        validateStateTransition(task.getState(), newState);

        OffsetDateTime now = OffsetDateTime.now();

        switch (newState) {
            case IN_PROGRESS -> task.setStartedAt(now);
            case COMPLETED   -> {
                task.setCompletedAt(now);
                // Đồng bộ: cleaning -> available trong cùng transaction
                Room room = task.getRoom();
                if (room != null && room.getStatus() == RoomStatus.CLEANING) {
                    room.setStatus(RoomStatus.AVAILABLE);
                    roomRepository.save(room);
                }
            }
            case ISSUE -> {
                // Giữ startedAt nếu đã có; cleaner sẽ POST issue chi tiết
            }
            default -> { /* PENDING — reset nếu cần */ }
        }

        task.setState(newState);
        return cleanerMapper.toTaskResponse(cleanerTaskRepository.save(task));
    }

    // -------------------------------------------------------------------------
    // POST /api/cleaner/issues
    // -------------------------------------------------------------------------
    @Transactional
    public CleanerIssueResponse createIssue(CreateCleanerIssueRequest request) {
        // Resolve room
        UUID roomUUID = parseUuidOrThrow(request.getRoomId(), "roomId");
        Room room = roomRepository.findById(roomUUID)
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        // Resolve cleaner account
        Account cleaner = currentAccount();

        // Resolve task (optional)
        CleanerTask task = null;
        if (request.getTaskId() != null) {
            task = cleanerTaskRepository.findById(request.getTaskId()).orElse(null);
        }

        CleanerIssue issue = CleanerIssue.builder()
                .room(room)
                .cleanerAccount(cleaner)
                .cleanerTask(task)
                .reason(request.getReason())
                .note(request.getNote())
                .status(CleanerIssueStatus.REPORTED)
                .reportedAt(OffsetDateTime.now())
                .build();

        // Nếu gắn với task, tự động chuyển task state -> ISSUE
        if (task != null && task.getState() != CleanerTaskState.COMPLETED) {
            task.setState(CleanerTaskState.ISSUE);
            cleanerTaskRepository.save(task);
        }

        return cleanerMapper.toIssueResponse(cleanerIssueRepository.save(issue));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** State machine cho cleaner task. */
    private void validateStateTransition(CleanerTaskState current, CleanerTaskState next) {
        boolean valid = switch (current) {
            case PENDING     -> next == CleanerTaskState.IN_PROGRESS;
            case IN_PROGRESS -> next == CleanerTaskState.COMPLETED
                    || next == CleanerTaskState.ISSUE;
            case ISSUE       -> next == CleanerTaskState.IN_PROGRESS
                    || next == CleanerTaskState.COMPLETED;
            case COMPLETED   -> false; // task đã hoàn thành, không đổi
        };
        if (!valid) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot transition cleaner task from "
                            + current.name().toLowerCase()
                            + " to " + next.name().toLowerCase());
        }
    }

    private CleanerTaskState parseStateOrThrow(String s) {
        try {
            return CleanerTaskState.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Invalid task state: " + s
                            + ". Valid: pending, in_progress, completed, issue");
        }
    }

    private UUID parseUuidOrThrow(String s, String field) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND,
                    "Invalid " + field + ": " + s);
        }
    }

    private String currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "";
        return auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }

    private Account currentAccount() {
        String email = currentEmail();
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
    }
}
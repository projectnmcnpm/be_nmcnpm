package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.request.CreateAccountRequest;
import com.nmcnpm.Homestay.dto.request.UpdateAccountRequest;
import com.nmcnpm.Homestay.dto.response.AccountResponse;
import com.nmcnpm.Homestay.entity.Account;
import com.nmcnpm.Homestay.enums.AccountRole;
import com.nmcnpm.Homestay.enums.AccountStatus;
import com.nmcnpm.Homestay.exception.AppException;
import com.nmcnpm.Homestay.exception.ErrorCode;
import com.nmcnpm.Homestay.mapper.AccountMapper;
import com.nmcnpm.Homestay.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper     accountMapper;
    private final PasswordEncoder   passwordEncoder;

    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // -------------------------------------------------------------------------
    // GET /api/accounts
    // -------------------------------------------------------------------------
    public List<AccountResponse> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // POST /api/accounts
    // -------------------------------------------------------------------------
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest req) {
        // Kiểm tra email trùng
        if (accountRepository.existsByEmail(req.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        AccountRole role = parseRoleOrThrow(req.getRole());
        AccountStatus status = req.getStatus() != null
                ? parseStatusOrThrow(req.getStatus())
                : AccountStatus.ACTIVE;

        Account account = Account.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getName())
                .role(role)
                .status(status)
                .build();

        account = accountRepository.save(account);

        // Gán created_label sau khi có created_at
        if (account.getCreatedAt() != null) {
            account.setCreatedLabel(account.getCreatedAt().format(LABEL_FMT));
            account = accountRepository.save(account);
        }

        return accountMapper.toResponse(account);
    }

    // -------------------------------------------------------------------------
    // PATCH /api/accounts/{id}  — partial update
    // -------------------------------------------------------------------------
    @Transactional
    public AccountResponse updateAccount(String id, UpdateAccountRequest req) {
        Account account = findById(id);

        if (req.getName() != null && !req.getName().isBlank()) {
            account.setFullName(req.getName());
        }

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            // Kiểm tra email mới không trùng với account khác
            if (!req.getEmail().equals(account.getEmail())
                    && accountRepository.existsByEmail(req.getEmail())) {
                throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
            account.setEmail(req.getEmail());
        }

        if (req.getRole() != null && !req.getRole().isBlank()) {
            account.setRole(parseRoleOrThrow(req.getRole()));
        }

        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            account.setStatus(parseStatusOrThrow(req.getStatus()));
        }

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            account.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        }

        return accountMapper.toResponse(accountRepository.save(account));
    }

    // -------------------------------------------------------------------------
    // DELETE /api/accounts/{id}  — soft delete
    // -------------------------------------------------------------------------
    @Transactional
    public void deleteAccount(String id) {
        Account account = findById(id);

        // Không cho xóa manager cuối cùng
        if (account.getRole() == AccountRole.MANAGER) {
            long activeManagers = accountRepository.findAll().stream()
                    .filter(a -> a.getRole() == AccountRole.MANAGER
                            && a.getDeletedAt() == null
                            && !a.getId().equals(account.getId()))
                    .count();
            if (activeManagers == 0) {
                throw new AppException(ErrorCode.FORBIDDEN,
                        "Cannot delete the last manager account");
            }
        }

        account.setDeletedAt(OffsetDateTime.now());
        accountRepository.save(account);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Account findById(String id) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        return accountRepository.findById(uuid)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));
    }

    private AccountRole parseRoleOrThrow(String s) {
        try {
            return AccountRole.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.FORBIDDEN,
                    "Invalid role: " + s + ". Valid: manager, receptionist, cleaner, customer");
        }
    }

    private AccountStatus parseStatusOrThrow(String s) {
        try {
            return AccountStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Invalid status: " + s + ". Valid: active, disabled");
        }
    }
}
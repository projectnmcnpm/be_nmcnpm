package com.nmcnpm.Homestay.controller;

import com.nmcnpm.Homestay.dto.request.CreateAccountRequest;
import com.nmcnpm.Homestay.dto.request.UpdateAccountRequest;
import com.nmcnpm.Homestay.dto.response.AccountResponse;
import com.nmcnpm.Homestay.dto.response.ApiResponse;
import com.nmcnpm.Homestay.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Account endpoints — tất cả yêu cầu role MANAGER:
 *
 *   GET    /api/accounts          manager
 *   POST   /api/accounts          manager
 *   PATCH  /api/accounts/{id}     manager
 *   DELETE /api/accounts/{id}     manager
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class AccountController {

    private final AccountService accountService;

    // ------------------------------------------------------------------
    // GET /api/accounts
    // ------------------------------------------------------------------
    @GetMapping
    public ApiResponse<List<AccountResponse>> getAllAccounts() {
        return ApiResponse.success(accountService.getAllAccounts());
    }

    // ------------------------------------------------------------------
    // POST /api/accounts
    // ------------------------------------------------------------------
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {
        return ApiResponse.success(
                accountService.createAccount(request),
                "Account created successfully"
        );
    }

    // ------------------------------------------------------------------
    // PATCH /api/accounts/{id}
    // ------------------------------------------------------------------
    @PatchMapping("/{id}")
    public ApiResponse<AccountResponse> updateAccount(
            @PathVariable String id,
            @Valid @RequestBody UpdateAccountRequest request
    ) {
        return ApiResponse.success(accountService.updateAccount(id, request));
    }

    // ------------------------------------------------------------------
    // DELETE /api/accounts/{id}  -> 204 No Content
    // ------------------------------------------------------------------
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@PathVariable String id) {
        accountService.deleteAccount(id);
    }
}
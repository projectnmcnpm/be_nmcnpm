package com.nmcnpm.Homestay.controller;

import com.nmcnpm.Homestay.dto.request.LoginRequest;
import com.nmcnpm.Homestay.dto.request.RefreshTokenRequest;
import com.nmcnpm.Homestay.dto.request.RegisterRequest;
import com.nmcnpm.Homestay.dto.response.ApiResponse;
import com.nmcnpm.Homestay.dto.response.AuthResponse;
import com.nmcnpm.Homestay.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Auth endpoints:
 *
 *   POST /api/auth/login     — public
 *   POST /api/auth/register  — public
 *   POST /api/auth/refresh   — public (dùng refreshToken)
 *   POST /api/auth/logout    — authenticated (thu hồi refreshToken)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ------------------------------------------------------------------
    // POST /api/auth/login
    // ------------------------------------------------------------------
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Login successful");
    }

    // ------------------------------------------------------------------
    // POST /api/auth/register
    // ------------------------------------------------------------------
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "Registration successful");
    }

    // ------------------------------------------------------------------
    // POST /api/auth/refresh
    // Body: { "refreshToken": "uuid-string" }
    // Trả về access token + refresh token mới (token rotation).
    // ------------------------------------------------------------------
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request), "Token refreshed successfully");
    }

    // ------------------------------------------------------------------
    // POST /api/auth/logout
    // Thu hồi tất cả refresh token của user hiện tại.
    // ------------------------------------------------------------------
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success(null, "Logged out successfully");
    }
}
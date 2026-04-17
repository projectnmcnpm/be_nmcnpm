package com.nmcnpm.Homestay.service;

import com.nmcnpm.Homestay.dto.request.LoginRequest;
import com.nmcnpm.Homestay.dto.request.RefreshTokenRequest;
import com.nmcnpm.Homestay.dto.request.RegisterRequest;
import com.nmcnpm.Homestay.dto.response.AuthResponse;
import com.nmcnpm.Homestay.entity.Account;
import com.nmcnpm.Homestay.entity.RefreshToken;
import com.nmcnpm.Homestay.enums.AccountRole;
import com.nmcnpm.Homestay.enums.AccountStatus;
import com.nmcnpm.Homestay.exception.AppException;
import com.nmcnpm.Homestay.exception.ErrorCode;
import com.nmcnpm.Homestay.repository.AccountRepository;
import com.nmcnpm.Homestay.repository.RefreshTokenRepository;
import com.nmcnpm.Homestay.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AccountRepository      accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil                jwtUtil;
    private final PasswordEncoder        passwordEncoder;

    /** Thời gian sống của refresh token (ngày). Mặc định 7 ngày. */
    @Value("${jwt.refresh-expiration-days:7}")
    private long refreshExpirationDays;

    /** Thời gian sống của access token (ms) — lấy từ jwt.expiration-ms. */
    @Value("${jwt.expiration-ms:3600000}")
    private long accessExpirationMs;

    private static final DateTimeFormatter LABEL_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // -------------------------------------------------------------------------
    // POST /api/auth/login
    // -------------------------------------------------------------------------
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (account.getStatus() == AccountStatus.DISABLED) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        return buildFullAuthResponse(account);
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/register
    // -------------------------------------------------------------------------
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Account account = Account.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getName())
                .role(AccountRole.CUSTOMER)
                .status(AccountStatus.ACTIVE)
                .build();

        account = accountRepository.save(account);

        if (account.getCreatedAt() != null) {
            account.setCreatedLabel(account.getCreatedAt().format(LABEL_FMT));
            accountRepository.save(account);
        }

        return buildFullAuthResponse(account);
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/refresh
    // Nhận refreshToken, kiểm tra hợp lệ, cấp lại accessToken + refreshToken mới.
    // -------------------------------------------------------------------------
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS,
                        "Refresh token not found"));

        if (!stored.isValid()) {
            // Thu hồi tất cả token của account nếu phát hiện token đã hết hạn/revoked được dùng lại
            // (có thể là dấu hiệu token bị đánh cắp)
            refreshTokenRepository.revokeAllByAccountId(stored.getAccount().getId());
            throw new AppException(ErrorCode.INVALID_CREDENTIALS,
                    "Refresh token is expired or revoked. Please login again.");
        }

        // Thu hồi token cũ (rotation: mỗi lần refresh sinh token mới)
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildFullAuthResponse(stored.getAccount());
    }

    // -------------------------------------------------------------------------
    // POST /api/auth/logout
    // Thu hồi tất cả refresh token của user hiện tại.
    // -------------------------------------------------------------------------
    @Transactional
    public void logout() {
        String email = currentEmail();
        if (email == null || email.isBlank()) return;

        accountRepository.findByEmail(email).ifPresent(account ->
                refreshTokenRepository.revokeAllByAccountId(account.getId())
        );
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Tạo đầy đủ AuthResponse: access token + refresh token mới + user info.
     */
    private AuthResponse buildFullAuthResponse(Account account) {
        // Access token (JWT)
        String accessToken = jwtUtil.generateToken(
                account.getEmail(), account.getRole().name());

        // Refresh token mới (UUID string, lưu DB)
        String rawRefreshToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(rawRefreshToken)
                .account(account)
                .expiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        long expiresInSeconds = accessExpirationMs / 1000;

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(expiresInSeconds)
                .user(AuthResponse.UserInfo.builder()
                        .id(account.getId().toString())
                        .email(account.getEmail())
                        .name(account.getFullName())
                        .role(account.getRole().name().toLowerCase())
                        .build())
                .build();
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }
}
package com.nmcnpm.Homestay.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * Body cho POST /api/auth/refresh
 * { "refreshToken": "uuid-string" }
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken is required")
    String refreshToken;
}
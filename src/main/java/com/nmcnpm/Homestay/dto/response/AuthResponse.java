package com.nmcnpm.Homestay.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Response cho login / register / refresh.
 *
 * {
 *   "accessToken":  "eyJ...",
 *   "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
 *   "expiresIn":    3600,
 *   "user": { "id": "...", "email": "...", "name": "...", "role": "manager" }
 * }
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResponse {

    /** JWT access token — ngắn hạn (mặc định 1h). */
    String accessToken;

    /**
     * Refresh token dạng UUID string — dài hạn (7 ngày).
     * Lưu vào DB, dùng để cấp lại access token mà không cần đăng nhập lại.
     */
    String refreshToken;

    /** Số giây access token còn hiệu lực (để frontend biết lúc nào cần refresh). */
    Long expiresIn;

    UserInfo user;

    // -------------------------------------------------------------------------

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserInfo {
        String id;
        String name;
        String email;
        String role;
    }
}
package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Được trả về khi user gọi API Sign In.
 * token: chứa JWT (JSON Web Token) hoặc access token mà client dùng để xác thực các API sau này (truyền trong Authorization: Bearer <token>).
 * twoStepVerification: thông tin về việc có yêu cầu xác thực 2 bước không.
 * Ví dụ "required" hoặc "enabled".
 */
@Getter
@Setter
public class TokenDto {
    private String token;   // JWT token (access token) sau khi user đăng nhập
    private String twoStepVerification; // Trạng thái xác thực 2 bước (2FA/OTP)
}

package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Được dùng khi client gọi API PUT /api/user/profile (hoặc tương tự) để cập nhật thông tin tài khoản.
 *
 * Cho phép user:
 * Đổi nickname hiển thị.
 * Bật/tắt hoặc thay đổi phương thức two-step verification (ví dụ: từ email → Google Authenticator
 */
@Getter
@Setter
public class UpdateProfileRequest {
    private String nickName;
    private String twoStepVerificationType;
}


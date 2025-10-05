package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Là DTO trả về cho client khi cần hiển thị thông tin người dùng (ví dụ GET /api/user/profile).
 * Chứa các thông tin cơ bản để hiển thị trên giao diện người dùng: email, tên, avatar, trạng thái bảo mật.
 * isBand (có thể bạn muốn là isBanned) cho biết user có bị khóa tài khoản không.
 */
@Getter
@Setter
public class UserDto {
    private String id;
    private String email;
    private String name;
    private String profilePhoto;
    private boolean isBand;
    private String createdAt;
    private String twoStepVerificationType;
}

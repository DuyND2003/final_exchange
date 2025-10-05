package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

// Nếu hệ thống bật 2FA (Two-Factor Authentication) hoặc login bằng OTP (qua email/SMS), thì code sẽ chứa giá trị đó
@Getter
@Setter
public class SignInRequest {
    private String email;
    private String password;
    private String code;
}

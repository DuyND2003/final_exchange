package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class SignUpRequest {
    @Email
    @NotBlank(message = "Email cannot be empty")
    private String email;

    @NotBlank
    private String password;

    private String code; // OTP code (xác thực email trong quá trình đăng ký).
}


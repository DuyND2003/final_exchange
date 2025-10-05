package org.example.final_usth.marketdata.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class User {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String email;
    private String passwordHash;
    private String passwordSalt; // Salt kèm hash để tăng bảo mật
    private String twoStepVerificationType; // Loại xác thực 2FA (email, TOTP, SMS...)
    private String nickName;
}

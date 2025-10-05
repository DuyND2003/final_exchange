package org.example.final_usth.api.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Nhưng trong code hiện tại, nó không thực hiện logic gì mà chỉ ném ra một RuntimeException("123456").
// endpoint này dự kiến sẽ được dùng để gửi mã xác thực (OTP / verification code) cho user (ví dụ khi đăng ký, rút tiền…).
@RestController
@RequestMapping("/api")
public class CodeController {

    @PostMapping("/codes")
    public void getCode() {
        throw new RuntimeException("123456");
    }

}

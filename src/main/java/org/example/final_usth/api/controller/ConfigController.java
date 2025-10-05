package org.example.final_usth.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

//Trong các sàn giao dịch, configs API thường dùng để:
// Cho frontend (web/mobile) lấy các cấu hình động khi khởi động app.
// Ví dụ: danh sách cặp giao dịch được bật (BTC/USDT, ETH/USDT...),
@RestController
public class ConfigController {

    @GetMapping("/configs")
    public Map<String, Object> getConfigs() {
        return null;
    }

}

package org.example.final_usth.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// Đây là một Spring MVC Controller (khác với @RestController), trả về view (String) thay vì JSON
//Có một endpoint mapping: GET /trade/ và /account/**.
// Khi user truy cập vào các URL đó, controller forward request về index.html.
// Trong một hệ thống exchange (và nhiều app web SPA như React/Vue/Angular):
//Frontend được build thành một Single Page Application (SPA).
//index.html chứa toàn bộ app React/Vue, điều hướng client-side bằng JavaScript Router.
//Backend chỉ cần forward tất cả request frontend route về index.html, để JS router render đúng trang.
@Controller
public class HomeController {
    @GetMapping(value = {"trade/*", "account/*"}) // Áp dụng cho tất cả URL dạng /trade/... và /account/....
    public String test() {
        return "forward:/index.html";
    }
}


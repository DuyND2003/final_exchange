package org.example.final_usth.api;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * File WebConfig này là cầu nối giúp Spring kích hoạt AuthInterceptor. Nếu không có cấu hình này, interceptor sẽ không bao giờ được gọi.
 * Khi request đến server, Spring MVC sẽ gọi lần lượt các interceptor đã đăng ký
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor; //// Interceptor custom dùng để xử lý auth

    @Override
    //  // Đăng ký AuthInterceptor để Spring MVC gọi nó cho mọi request trước khi đến Controller
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor);
    }
}


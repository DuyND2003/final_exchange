package org.example.final_usth.api;

import lombok.RequiredArgsConstructor;
import org.example.final_usth.marketdata.entity.User;
import org.example.final_usth.marketdata.manager.UserManager;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * . File AuthInterceptor này là một Spring Interceptor dùng để
 *  xác thực người dùng qua access token trước khi vào controller.
 *  Đây là middleware chạy trước khi request đến Controller (nhờ preHandle).
 *  Lấy access token từ query param hoặc cookie.
 * Nếu có token → nhờ UserManager tìm user tương ứng.
 * Gắn currentUser và accessToken vào request attribute.
 * Controller có thể lấy @RequestAttribute User currentUser để biết user nào đang gọi API.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final UserManager userManager;

    // preHandle chạy trước mỗi request.
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // getAccessToken(request) sẽ cố gắng tìm token.
        String accessToken = getAccessToken(request);
        if (accessToken != null) {
            // Gọi userManager.getUserByAccessToken(token) để load user từ DB/cache.
            User user = userManager.getUserByAccessToken(accessToken);
            // Gắn currentUser và accessToken vào request.
            request.setAttribute("currentUser", user);
            request.setAttribute("accessToken", accessToken);
        }
        // Luôn return true → cho phép request tiếp tục vào Controller.
        return true;
    }

    private String getAccessToken(HttpServletRequest request) {
        String tokenKey = "accessToken";
        // Thử lấy token từ query parameter (?accessToken=xxx)
        String token = request.getParameter(tokenKey);
        //   // 2. Nếu không có trong query param, thì thử tìm trong cookie
        if (token == null && request.getCookies() != null) {
            // Duyệt qua tất cả cookie, tìm cookie có tên "accessToken"
            for (Cookie cookie : request.getCookies()) {
                if (cookie.getName().equals(tokenKey)) {
                    token = cookie.getValue();
                }
            }
        }
        return token;
    }
}

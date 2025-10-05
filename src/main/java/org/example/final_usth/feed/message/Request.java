package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Là message đầu vào từ client (frontend) gửi lên server, chứa loại yêu cầu (type)
 * Khi người dùng thao tác gửi lệnh, đăng ký feed,…
 */
public class Request {
    private String type;
}

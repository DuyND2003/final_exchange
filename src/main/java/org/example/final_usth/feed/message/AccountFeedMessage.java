package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
//  push thông tin tài khoản (balance) của người dùng qua WebSocket hoặc Kafka topic cho frontend.
public class AccountFeedMessage {
    private String type = "funds";
    private String productId;
    private String userId;
    private String currencyCode; // "BTC", mã tiền tệ đc cập nhập
    private String available;
    private String hold;
}

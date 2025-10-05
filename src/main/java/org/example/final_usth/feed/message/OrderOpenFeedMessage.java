package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Thông báo rằng lệnh đã được ghi vào sổ lệnh (order book) và sẵn sàng khớp
 * Sau khi kiểm tra đủ điều kiện và đặt vào sổ
 */
public class OrderOpenFeedMessage {
    private String type = "open";
    private String productId;
    private long sequence;
    private String time;
    private String orderId;
    private String remainingSize;
    private String price;
    private String side;
}


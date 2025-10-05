package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Thông báo rằng sàn đã nhận được lệnh mới từ user (nhưng chưa mở trong sổ lệnh)
 * Ngay sau khi người dùng gửi yêu cầu đặt lệnh
 */
public class OrderReceivedFeedMessage {
    private String type = "received";
    private String time;
    private String productId;
    private long sequence;
    private String orderId;
    private String size;
    private String price;
    private String funds;
    private String side;
    private String orderType;
}

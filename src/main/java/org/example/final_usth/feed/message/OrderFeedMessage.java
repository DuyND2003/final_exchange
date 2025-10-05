package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Thông báo chi tiết về một lệnh cụ thể — thường là lệnh vừa được người dùng đặt lên sàn.
 */
public class OrderFeedMessage {
    private String type = "order";
    private String productId;
    private String userId;
    private String sequence;
    private String id;
    private String price;
    private String size;
    private String funds;
    private String side;
    private String orderType;
    private String createdAt;
    private String filledSize;
    private String executedValue;
    private String status;
}


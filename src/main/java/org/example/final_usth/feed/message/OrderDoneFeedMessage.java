package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Gửi khi lệnh đã hoàn tất (filled hết) hoặc bị hủy.
 */
public class OrderDoneFeedMessage {
    private String type = "done";
    private String productId;
    private long sequence;
    private String orderId;
    private String remainingSize;
    private String price;
    private String side;
    private String reason;
    private String time;
}

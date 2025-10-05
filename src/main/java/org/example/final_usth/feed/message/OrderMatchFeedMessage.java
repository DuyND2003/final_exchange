package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Thông báo khi một giao dịch (trade) xảy ra — tức là khi lệnh khớp giữa bên mua và bên bán.
 */
public class OrderMatchFeedMessage {
    private String type = "match";
    private String productId;
    private long tradeId;
    private long sequence;
    private String takerOrderId;
    private String makerOrderId;
    private String time;
    private String size;
    private String price;
    private String side;
}
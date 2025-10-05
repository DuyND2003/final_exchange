package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Nó tổng hợp mọi thông tin quan trọng của một lệnh để hiển thị trên UI trading (order history, open orders, order detail).
 * Được build từ OrderEntity (dữ liệu DB) hoặc từ Order (RAM trong Matching Engine).
 */
@Getter
@Setter
public class OrderDto {
    private String id;
    // Giá đặt lệnh (chỉ áp dụng cho LIMIT order).
    // Với MARKET order, price có thể = "0".
    private String price;
    // Khối lượng đặt lệnh (base currency), ví dụ: 1.5 BTC
    private String size;
    // Tổng giá trị (quote currency), ví dụ: 1.5 BTC * 20000 USDT = 30000 USDT
    // Với MARKET BUY, user nhập funds thay vì size.
    private String funds;
    private String productId;
    private String side;
    private String type;
    private String createdAt;
    private String filledSize;
    private String executedValue;
    // Trạng thái order: "open", "filled", "cancelled", "rejected"
    private String status;
}


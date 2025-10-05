package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Là DTO cho trade history API (lịch sử giao dịch khớp lệnh).
 *
 * Được dùng khi user hoặc market data API cần xem danh sách các lệnh đã khớp.
 */
@Getter
@Setter
public class TradeDto {
    private long sequence;  // // Số thứ tự (sequence) của trade, đảm bảo order theo thời gian
    private String time;
    private String price;
    private String size;
    private String side;
}

package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Đây là Data Transfer Object cho một cặp giao dịch (trading pair) trên sàn.
 * Được dùng trong API /api/products để trả thông tin cấu hình các cặp giao dịch.
 */
@Getter
@Setter
public class ProductDto {
    private String id;  // ID duy nhất của cặp (ví dụ: "BTC-USDT")
    private String baseCurrency;     // Đồng cơ sở (ví dụ: "BTC")
    private String quoteCurrency; /// Đồng định giá (ví dụ: "USDT")
    private String quoteIncrement;      // Bước nhảy của giá định giá (ví dụ: "0.01 USDT")
}


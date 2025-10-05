package org.example.final_usth.marketdata.entity;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;
import org.example.final_usth.enums.OrderStatus;
import org.example.final_usth.enums.OrderType;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class OrderEntity {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private long sequence; // Thứ tự để đảm bảo sắp xếp (matching engine log)
    private String productId; // Cặp giao dịch: BTC-USDT, ETH-VND
    private String userId; // User nào đặt lệnh
    private String clientOid; // Client Order ID (idempotency key, do client sinh ra)
    private Date time;  // Thời điểm order gửi vào hệ thống
    private BigDecimal size;  // Khối lượng đặt (BTC)
    private BigDecimal funds; // Tổng giá trị order (size * price)
    private BigDecimal filledSize; // Đã khớp bao nhiêu khối lượng
    private BigDecimal executedValue; // Giá trị đã khớp (size * price thực tế)
    private BigDecimal price;  // Giá đặt
    private OrderType type;      // LIMIT / MARKET
    private OrderSide side;  // BUY / SELL
    private OrderStatus status; // OPEN / FILLED / CANCELED / EXPIRED
}


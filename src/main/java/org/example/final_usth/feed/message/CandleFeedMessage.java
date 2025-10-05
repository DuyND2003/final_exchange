package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * đại diện cho thông điệp “nến giá” (candlestick) mà hệ thống gửi ra real-time feed (Kafka hoặc WebSocket).
 * Mục đích là để truyền dữ liệu nến (OHLCV) đến frontend hiển thị biểu đồ giá như TradingView.
 * Trong hệ thống sàn giao dịch, dữ liệu “nến” (candle) thể hiện biến động giá trong một khoảng thời gian nhất định
 * — ví dụ: 1 phút, 5 phút, 1 giờ, 1 ngày, v.v.
 */
public class CandleFeedMessage {
    private String type = "candle";
    private String productId;
    private long sequence; // Số thứ tự (sequence) tăng dần giúp xác định thứ tự thông điệp — đảm bảo dữ liệu đến đúng thứ tự.
    private int granularity; // Độ dài nến (theo phút). Ví dụ: 1 = 1 phút, 5 = 5 phút, 60 = 1 giờ.
    private long time; // Thời điểm mở nến (UNIX timestamp).
    private String open;
    private String close;
    private String high;
    private String low;
    private String volume; // Khối lượng giao dịch trong nến.
}


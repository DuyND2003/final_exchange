package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
/**
 * Là yêu cầu từ client gửi lên WebSocket server để đăng ký (subscribe)
 * nhận dữ liệu real-time của một hoặc nhiều cặp giao dịch.
 */
public class SubscribeRequest extends Request {
    private List<String> productIds; // Danh sách các cặp giao dịch cần theo dõi, ví dụ: ["BTC-USDT", "ETH-USDT"]
    private List<String> currencyIds; //Nếu có) dùng khi cần subscribe theo loại tiền, ví dụ ["BTC", "USDT"]
    private List<String> channels; // 	Các kênh dữ liệu muốn nhận, ví dụ ["ticker", "level2", "matches", "user"]
}

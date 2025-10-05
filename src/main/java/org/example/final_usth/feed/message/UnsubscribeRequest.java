package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
/**
 * Là yêu cầu client gửi để hủy đăng ký (unsubscribe) nhận dữ liệu real-time.
 * Dạng tương tự SubscribeRequest, nhưng để ngắt kết nối khỏi một kênh.
 */
public class UnsubscribeRequest extends Request {
    private List<String> productIds;
    private List<String> currencyIds;
    private List<String> channels;
}
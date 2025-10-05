package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.marketdata.orderbook.L2OrderBookChange;

import java.util.Date;
import java.util.List;

@Getter
@Setter
/**
 * truyền cập nhật thay đổi (incremental updates) trong sổ lệnh cấp độ 2 (Level 2 Order Book) của sàn giao dịch.
 * Ban đầu frontend nhận L2SnapshotFeedMessage → ảnh chụp toàn bộ sổ lệnh.
 * Sau đó, hệ thống chỉ gửi L2UpdateFeedMessage để thông báo những thay đổi nhỏ như lệnh mới, lệnh bị hủy, hoặc khối lượng cập nhật.
 */

public class L2UpdateFeedMessage {
    private String type = "l2update";
    private String productId;
    private String time;    // Xác định thời điểm update được tạo — giúp frontend hoặc hệ thống sắp xếp thứ tự chính xác.
    private List<L2OrderBookChange> changes; // Danh sách các thay đổi cụ thể trong sổ lệnh.

    public L2UpdateFeedMessage() {
    }

    public L2UpdateFeedMessage(String productId, List<L2OrderBookChange> l2OrderBookChanges) {
        this.productId = productId;
        this.time = new Date().toInstant().toString();
        this.changes = l2OrderBookChanges;
    }
}

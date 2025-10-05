package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

@Getter
@Setter
// đại diện cho ảnh chụp sổ lệnh cấp độ 2 (Level 2 Order Book Snapshot)
// L2: Gộp các lệnh theo từng mức giá (ví dụ: tổng số BTC chờ bán ở 26300 USDT).
// L3 Toàn bộ danh sách lệnh chi tiết từng order.
// l1: Chỉ hiển thị best bid và best ask (giá mua/bán tốt nhất).
public class L2SnapshotFeedMessage extends L2OrderBook {
    // Đánh dấu đây là snapshot (toàn bộ sổ lệnh), khác với "l2update" là thông điệp cập nhật incremental.
    private String type = "snapshot";
    // Dùng để khởi tạo đối tượng L2SnapshotFeedMessage từ một L2OrderBook hiện có.
    public L2SnapshotFeedMessage(L2OrderBook snapshot) {
        BeanUtils.copyProperties(snapshot, this);
    }
}

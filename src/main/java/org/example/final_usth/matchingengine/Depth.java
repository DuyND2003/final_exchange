package org.example.final_usth.matchingengine;

import org.example.final_usth.matchingengine.entity.Order;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.TreeMap;

// // Depth đại diện cho order book depth (1 bên: BUY hoặc SELL).
// Kế thừa TreeMap<BigDecimal, PriceGroupedOrderCollection> để lưu trữ các mức giá (BigDecimal = price).
// Key   = Price (giá đặt lệnh)
// Value = Tập hợp các order có cùng mức giá
public class Depth extends TreeMap<BigDecimal, PriceGroupedOrderCollection> {

    // Constructor: cho phép truyền comparator để quyết định sắp xếp:
    // - SELL side (asks)  → Comparator.naturalOrder() (giá thấp lên cao)
    // - BUY side (bids)   → Comparator.reverseOrder() (giá cao xuống thấp)
    public Depth(Comparator<BigDecimal> comparator) {
        super(comparator);
    }

    // Thêm order vào Depth
    public void addOrder(Order order) {
        // Tìm group theo giá (nếu chưa có thì tạo mới PriceGroupedOrderCollection)
        // Sau đó put order vào group này với key = orderId
        this.computeIfAbsent(order.getPrice(), k -> new PriceGroupedOrderCollection()).put(order.getId(), order);
    }

    // Xoá order ra khỏi Depth
    public void removeOrder(Order order) {
        // Lấy group theo giá
        var orders = get(order.getPrice());
        if (orders == null) {
            return; // không tồn tại group thì bỏ qua
        }

        // Xoá order khỏi group
        orders.remove(order.getId());

        // Nếu group đó trống → xoá luôn mức giá khỏi TreeMap
        if (orders.isEmpty()) {
            remove(order.getPrice());
        }
    }
}


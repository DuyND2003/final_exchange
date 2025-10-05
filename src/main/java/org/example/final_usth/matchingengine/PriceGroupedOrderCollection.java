package org.example.final_usth.matchingengine;

import lombok.Getter;
import org.example.final_usth.matchingengine.entity.Order;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

//  PriceGroupedOrderCollection = tập hợp các order cùng mức giá, Mỗi group giá có thể chứa nhiều order (FIFO – ai vào trước khớp trước).
//  Kế thừa LinkedHashMap<String, Order>
//  Key   = orderId   Value = Order object
@Getter
public class PriceGroupedOrderCollection extends LinkedHashMap<String, Order> {

    public void addOrder(Order order) {
        // Thêm order vào Map với key = orderId
        put(order.getId(), order);
    }

    // ------------------- CALCULATE TOTAL REMAINING -------------------
    public BigDecimal getRemainingSize() {
        // Tính tổng remainingSize của tất cả order trong group
        // values() = tập tất cả Order trong Map
        // map(Order::getRemainingSize) → lấy remainingSize của từng order
        // reduce(BigDecimal::add)      → cộng dồn
        return values().stream()
                .map(Order::getRemainingSize)
                .reduce(BigDecimal::add)
                .get();
    }
}

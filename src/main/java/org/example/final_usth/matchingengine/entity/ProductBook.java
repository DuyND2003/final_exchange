package org.example.final_usth.matchingengine.entity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.matchingengine.message.ProductMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RequiredArgsConstructor
public class ProductBook {
    private final Map<String, Product> products = new HashMap<>();
    private final MessageSender messageSender;
    private final AtomicLong messageSequence;

    public Collection<Product> getAllProducts() {
        return products.values();
    }

    public Product getProduct(String productId) {
        return products.get(productId);
    }

    // Thêm hoặc cập nhật sản phẩm và gửi ProductMessage đi.
    // Dùng khi muốn thông báo cho hệ thống (Kafka/Redis/WebSocket).
    public void putProduct(Product product) {
        this.products.put(product.getId(), product);
        messageSender.send(productMessage(product.clone()));
        // product.clone(): tránh trường hợp object trong map bị thay đổi sau khi đã gửi đi (giữ message "đóng băng" tại thời điểm đó).
    }
    // Thêm sản phẩm vào danh sách nhưng KHÔNG gửi message đi (chỉ lưu cục bộ)
    // Dùng khi load dữ liệu ban đầu.
    public void addProduct(Product product) {
        this.products.put(product.getId(), product);
    }

    // Tạo ProductMessage từ một product
    private ProductMessage productMessage(Product product) {
        ProductMessage message = new ProductMessage();
        // Tăng sequence và gán vào message → đảm bảo thứ tự xử lý message
        message.setSequence(messageSequence.incrementAndGet());
        message.setProduct(product);
        return message;
    }
}



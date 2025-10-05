package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

/**
 * Nó chứa toàn bộ dữ liệu do người dùng nhập để tạo lệnh.
 */
@Getter
@Setter
public class PlaceOrderRequest {
    // Client Order ID – optional.
    // Cho phép client gán ID riêng để đối chiếu (ví dụ khi gọi API qua bot trading).
    // Nếu null thì server sẽ tự sinh UUID làm orderId
    private String clientOid;

    // Cặp giao dịch, ví dụ: "BTC-USDT"
    // @NotBlank → bắt buộc phải có
    @NotBlank
    private String productId;

    // Số lượng tài sản đặt (base currency), ví dụ: "1.5"
    // Với SELL hoặc LIMIT BUY thì phải nhập size.
    // Với MARKET BUY thì size có thể bỏ trống (sẽ dùng funds thay thế).
    @NotBlank
    private String size;

    // Số tiền sử dụng (quote currency), ví dụ: "30000"
    // Dùng chủ yếu cho MARKET BUY (mua BTC với 30000 USDT)
    private String funds;

    // Giá đặt lệnh (quote currency), ví dụ: "20000"
    // Bắt buộc cho LIMIT order, không dùng cho MARKET order.
    private String price;

    @NotBlank
    private String side;

    @NotBlank
    private String type;
}


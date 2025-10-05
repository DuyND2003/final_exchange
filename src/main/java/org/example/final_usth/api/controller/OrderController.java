package org.example.final_usth.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.example.final_usth.api.model.OrderDto;
import org.example.final_usth.api.model.PagedList;
import org.example.final_usth.api.model.PlaceOrderRequest;
import org.example.final_usth.enums.OrderSide;
import org.example.final_usth.enums.OrderStatus;
import org.example.final_usth.enums.OrderType;
import org.example.final_usth.marketdata.entity.OrderEntity;
import org.example.final_usth.marketdata.entity.ProductEntity;
import org.example.final_usth.marketdata.entity.User;
import org.example.final_usth.marketdata.repository.OrderRepository;
import org.example.final_usth.marketdata.repository.ProductRepository;
import org.example.final_usth.matchingengine.command.CancelOrderCommand;
import org.example.final_usth.matchingengine.command.MatchingEngineCommandProducer;
import org.example.final_usth.matchingengine.command.PlaceOrderCommand;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Đây là Trading Feature – Order Management:
 * Đặt lệnh (place order) → User gửi yêu cầu đặt lệnh mua/bán (BUY/SELL).
 * Huỷ lệnh (cancel order) → User huỷ 1 lệnh hoặc toàn bộ lệnh mở của mình.
 * Xem danh sách lệnh (list orders) → User xem các lệnh đang mở hoặc đã khớp/hủy.
 * Tất cả đều là API REST để frontend (UI) hoặc bot trading có thể tương tác với hệ thống.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository orderRepository;
    private final MatchingEngineCommandProducer matchingEngineCommandProducer;
    private final ProductRepository productRepository;

    // API đặt lệnh: POST /api/orders
    @PostMapping(value = "/orders")
    public OrderDto placeOrder(@RequestBody @Valid PlaceOrderRequest request,
                               @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 2. Kiểm tra product có tồn tại không (ví dụ: BTC-USDT)
        ProductEntity product = productRepository.findById(request.getProductId());
        if (product == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "product not found: " + request.getProductId());
        }

        // 3. Parse dữ liệu từ request sang enum & BigDecimal
        //    - type: LIMIT / MARKET
        //    - side: BUY / SELL
        OrderType type = OrderType.valueOf(request.getType().toUpperCase());
        OrderSide side = OrderSide.valueOf(request.getSide().toUpperCase());
        BigDecimal size = new BigDecimal(request.getSize());
        BigDecimal price = request.getPrice() != null ? new BigDecimal(request.getPrice()) : null;
        BigDecimal funds = request.getFunds() != null ? new BigDecimal(request.getFunds()) : null;

        // 4. Tạo PlaceOrderCommand gửi vào Matching Engine
        PlaceOrderCommand command = new PlaceOrderCommand();
        command.setProductId(request.getProductId());
        command.setOrderId(UUID.randomUUID().toString()); // sinh orderId ngẫu nhiên
        command.setUserId(currentUser.getId());            /// gán user hiện tại
        command.setOrderType(type);
        command.setOrderSide(side);
        command.setSize(size);
        command.setPrice(price);
        command.setFunds(funds);
        command.setTime(new Date());

        // 5. Chuẩn hoá lệnh (format size, price theo quy định của product)
        formatPlaceOrderCommand(command, product);
        // 6. Validate lệnh (ví dụ SELL size phải > 0, BUY funds phải > 0)
        validatePlaceOrderCommand(command);
        // 7. Gửi lệnh vào Kafka để Matching Engine xử lý
        matchingEngineCommandProducer.send(command, null);

        // 8. Trả về OrderDto cho frontend (chỉ cần orderId để tracking)
        OrderDto orderDto = new OrderDto();
        orderDto.setId(command.getOrderId());
        return orderDto;
    }

    @DeleteMapping("/orders/{orderId}")
    @SneakyThrows
    public void cancelOrder(@PathVariable String orderId, @RequestAttribute(required = false) User currentUser) {
        // 1. Kiểm tra user đã đăng nhập chưa
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 2. Tìm lệnh trong DB theo orderId
        OrderEntity order = orderRepository.findByOrderId(orderId);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found: " + orderId);
        }

        // 3. Đảm bảo user hiện tại là chủ sở hữu của order đó
        if (!order.getUserId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        // 4. Tạo CancelOrderCommand gửi sang Matching Engine qua Kafka
        CancelOrderCommand command = new CancelOrderCommand();
        command.setProductId(order.getProductId());
        command.setOrderId(order.getId());
        matchingEngineCommandProducer.send(command, null);
    }

    @DeleteMapping("/orders")
    @SneakyThrows
    // API huỷ nhiều lệnh theo điều kiện (productId + side)
    public void cancelOrders(String productId, String side, @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 2. Parse tham số side (BUY/SELL) nếu có
        OrderSide orderSide = side != null ? OrderSide.valueOf(side.toUpperCase()) : null;

        // 3. Lấy danh sách tất cả lệnh OPEN của user theo filter (productId, side)
        //    Giới hạn 20000 lệnh để tránh quá tải
        PagedList<OrderEntity> orderPage = orderRepository.findAll(currentUser.getId(), productId, OrderStatus.OPEN,
                orderSide, 1, 20000);

        // 4. Với mỗi order, tạo CancelOrderCommand và gửi sang Kafka
        for (OrderEntity order : orderPage.getItems()) {
            CancelOrderCommand command = new CancelOrderCommand();
            command.setProductId(order.getProductId());
            command.setOrderId(order.getId());
            matchingEngineCommandProducer.send(command, null);
        }
    }

    // API: GET /api/orders
    // Dùng để lấy danh sách các lệnh của user (có thể filter theo productId, status, phân trang)
    @GetMapping("/orders")
    public PagedList<OrderDto> listOrders(@RequestParam(required = false) String productId,
                                          @RequestParam(required = false) String status,   // trạng thái lệnh (OPEN, FILLED…)
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "50") int pageSize,
                                          @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        // 2. Nếu có truyền status thì parse về enum OrderStatus
        OrderStatus orderStatus = status != null ? OrderStatus.valueOf(status.toUpperCase()) : null;

        // 3. Gọi repository để query danh sách order trong DB
        //    - chỉ lấy lệnh của user hiện tại
        //    - có thể filter theo productId và status
        //    - phân trang theo page và pageSize
        PagedList<OrderEntity> orderPage = orderRepository.findAll(currentUser.getId(), productId, orderStatus, null,
                page, pageSize);

        // 4. Convert từ OrderEntity (DB) → OrderDto (trả về cho frontend)
        return new PagedList<>(
                orderPage.getItems().stream().map(this::orderDto).collect(Collectors.toList()),
                orderPage.getCount());
    }

    // Chuyển đổi từ OrderEntity (dữ liệu trong DB) sang OrderDto (trả về cho frontend)
    // Mục đích: format các field thành string, xử lý null → "0"
    private OrderDto orderDto(OrderEntity order) {
        OrderDto orderDto = new OrderDto();
        orderDto.setId(order.getId());
        orderDto.setPrice(order.getPrice().toPlainString());
        orderDto.setSize(order.getSize().toPlainString());
        orderDto.setFilledSize(order.getFilledSize() != null ? order.getFilledSize().toPlainString() : "0");
        // Số tiền dùng cho lệnh (chỉ dùng khi BUY MARKET, nếu null thì gán "0")
        orderDto.setFunds(order.getFunds() != null ? order.getFunds().toPlainString() : "0");
        orderDto.setExecutedValue(order.getExecutedValue() != null ? order.getExecutedValue().toPlainString() : "0");
        orderDto.setSide(order.getSide().name().toLowerCase());
        orderDto.setProductId(order.getProductId());
        orderDto.setType(order.getType().name().toLowerCase());
        // // Thời gian tạo lệnh (ISO string) nếu có
        if (order.getCreatedAt() != null) {
            orderDto.setCreatedAt(order.getCreatedAt().toInstant().toString());
        }
        //  // Trạng thái lệnh: open/filled/cancelled
        if (order.getStatus() != null) {
            orderDto.setStatus(order.getStatus().name().toLowerCase());
        }
        return orderDto;
    }

    // Chuẩn hoá dữ liệu của PlaceOrderCommand trước khi gửi vào Matching Engine
    // Mục tiêu: đảm bảo size, price, funds tuân theo quy định (scale, làm tròn)
    private void formatPlaceOrderCommand(PlaceOrderCommand command, ProductEntity product) {
        BigDecimal size = command.getSize();
        BigDecimal price = command.getPrice();
        BigDecimal funds = command.getFunds();
        OrderSide side = command.getOrderSide();

        switch (command.getOrderType()) {
            case LIMIT -> {
                // Với lệnh LIMIT, user phải nhập size + price
                // Làm tròn size theo baseScale (số chữ số thập phân cho khối lượng)
                size = size.setScale(product.getBaseScale(), RoundingMode.DOWN);
                // Làm tròn price theo quoteScale (số chữ số thập phân cho giá)
                price = price.setScale(product.getQuoteScale(), RoundingMode.DOWN);
                // Nếu là BUY thì funds = size * price, nếu SELL thì funds = 0
                funds = side == OrderSide.BUY ? size.multiply(price) : BigDecimal.ZERO;
            }
            case MARKET -> {
                // Với lệnh MARKET, user chỉ nhập funds (BUY) hoặc size (SELL)
                price = BigDecimal.ZERO; // không cần price
                if (side == OrderSide.BUY) {
                    // MARKET BUY: user nhập số tiền → hệ thống tính ra size sau khi khớp
                    size = BigDecimal.ZERO;
                    funds = funds.setScale(product.getQuoteScale(), RoundingMode.DOWN); // làm tròn số tiền
                } else {
                    // MARKET SELL: user nhập size → hệ thống tính ra funds sau khi khớp
                    size = size.setScale(product.getBaseScale(), RoundingMode.DOWN); // làm tròn khối lượng
                    funds = BigDecimal.ZERO;
                }
            }
            default -> throw new RuntimeException("unknown order type: " + command.getType());
        }

        // Cập nhật lại vào command
        command.setSize(size);
        command.setPrice(price);
        command.setFunds(funds);
    }

    // Hàm này dùng để kiểm tra tính hợp lệ cơ bản của lệnh trước khi gửi sang Matching Engine
    // Mục tiêu: loại bỏ các lệnh BUY/SELL sai dữ liệu (size hoặc funds <= 0)
    private void validatePlaceOrderCommand(PlaceOrderCommand command) {
        BigDecimal size = command.getSize();
        BigDecimal funds = command.getFunds();
        OrderSide side = command.getOrderSide();

        if (side == OrderSide.SELL) {
            // Với lệnh SELL:
            //   - Người dùng bán một lượng tài sản (vd: 1 BTC)
            //   - size (khối lượng) phải > 0
            if (size.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("bad SELL order: size must be positive");
            }
        } else {
            // Với lệnh BUY:
            //   - Người dùng mua bằng một lượng tiền (funds)
            //   - funds (tổng số tiền) phải > 0
            if (funds.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("bad BUY order: funds must be positive");
            }
        }
    }

}


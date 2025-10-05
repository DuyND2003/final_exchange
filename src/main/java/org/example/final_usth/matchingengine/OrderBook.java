package org.example.final_usth.matchingengine;

import com.alibaba.fastjson.JSON;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.enums.OrderSide;
import org.example.final_usth.enums.OrderStatus;
import org.example.final_usth.enums.OrderType;
import org.example.final_usth.matchingengine.entity.*;
import org.example.final_usth.matchingengine.message.OrderMessage;
import org.example.final_usth.matchingengine.message.TradeMessage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Slf4j
public class OrderBook {
    private final String productId; // Mã sản phẩm (VD: BTC-USDT) -> mỗi OrderBook quản lý 1 cặp
    private final ProductBook productBook; // Danh sách các sản phẩm trong hệ thống
    private final AccountBook accountBook;  // Quản lý tài khoản (balance, hold, release, exchange)
    private final Depth asks = new Depth(Comparator.naturalOrder()); // Order SELL, sắp xếp từ thấp -> cao
    private final Depth bids = new Depth(Comparator.reverseOrder()); // Order BUY, sắp xếp từ cao -> thấp
    private final Map<String, Order> orderById = new HashMap<>(); // Map id → order để tra cứu nhanh
    private final MessageSender messageSender; // Kafka producer, gửi event ra ngoài (order, trade,…)
    private final AtomicLong messageSequence;   // Đánh số tăng dần cho từng message gửi ra
    private long orderSequence; // Sequence tăng dần cho order
    private long tradeSequence;  // Sequence tăng dần cho trade
    private long orderBookSequence; // Sequence cho trạng thái order book (sổ lệnh)

    // Khởi tạo OrderBook cho 1 product
    public OrderBook(String productId,
                     long orderSequence, long tradeSequence, long orderBookSequence,
                     AccountBook accountBook, ProductBook productBook, MessageSender messageSender, AtomicLong messageSequence) {
        this.productId = productId;
        this.productBook = productBook;
        this.accountBook = accountBook;
        this.orderSequence = orderSequence;
        this.tradeSequence = tradeSequence;
        this.orderBookSequence = orderBookSequence;
        this.messageSender = messageSender;
        this.messageSequence = messageSequence;
    }


    public void placeOrder(Order takerOrder) {
        var product = productBook.getProduct(productId);
        if (product == null) {
            log.warn("order rejected, reason: PRODUCT_NOT_FOUND");
            return;
        }

        takerOrder.setSequence(++orderSequence); // gán sequence mới cho order

        boolean ok;
        // Bước 1: kiểm tra balance của user
        if (takerOrder.getSide() == OrderSide.BUY) {
            // Mua: hold tiền quote (VD: USDT)
            ok = accountBook.hold(takerOrder.getUserId(), product.getQuoteCurrency(), takerOrder.getRemainingFunds());
        } else {
            // Bán: hold coin base (VD: BTC)
            ok = accountBook.hold(takerOrder.getUserId(), product.getBaseCurrency(), takerOrder.getRemainingSize());
        }
        if (!ok) {
            // Nếu không đủ tiền → reject order
            log.warn("order rejected, reason: INSUFFICIENT_FUNDS: {}", JSON.toJSONString(takerOrder));
            takerOrder.setStatus(OrderStatus.REJECTED);
            messageSender.send(orderMessage(takerOrder.clone())); // phát sự kiện order bị từ chối
            return;
        }

        // Bước 2: order đã nhận
        takerOrder.setStatus(OrderStatus.RECEIVED);
        messageSender.send(orderMessage(takerOrder.clone()));

        // Bước 3: Matching (khớp lệnh)
        // Nếu taker là BUY → tìm bên SELL (asks), ngược lại nếu taker là SELL → tìm bên BUY (bids)
        var makerDepth = takerOrder.getSide() == OrderSide.BUY ? asks : bids;

        // Lấy iterator duyệt qua từng mức giá trong depth (map price → list orders)
        var depthEntryItr = makerDepth.entrySet().iterator();

        MATCHING:
        while (depthEntryItr.hasNext()) { // duyệt qua từng mức giá của order đối ứng (maker side)
            var entry = depthEntryItr.next();
            var price = entry.getKey(); // giá hiện tại của maker side
            var orders = entry.getValue();  // danh sách order tại mức giá này (map orderId → order)

            // Kiểm tra có "price crossing" hay không
            //  - BUY price >= SELL price → khớp được
            //  - SELL price <= BUY price → khớp được
            if (!isPriceCrossed(takerOrder, price)) {
                break; // không có crossing → dừng matching (không còn khớp được nữa)
            }

            // Duyệt qua từng order của maker tại mức giá này
            var orderItr = orders.entrySet().iterator();
            while (orderItr.hasNext()) {
                var orderEntry = orderItr.next();
                var makerOrder = orderEntry.getValue(); // lấy từng maker order

                // Thực hiện trade giữa taker và maker
                Trade trade = trade(takerOrder, makerOrder);
                if (trade == null) {
                    break MATCHING; // nếu không còn khớp được → dừng toàn bộ vòng lặp MATCHING
                }

                // Update balance cho 2 user:
                //  - Buyer: giảm USDT, tăng BTC
                //  - Seller: giảm BTC, tăng USDT
                accountBook.exchange(takerOrder.getUserId(), makerOrder.getUserId(), product.getBaseCurrency(),
                        product.getQuoteCurrency(), takerOrder.getSide(), trade.getSize(), trade.getFunds());

                // Nếu maker order đã hoàn thành (FILLED) hoặc bị hủy (CANCELLED)
                if (makerOrder.getStatus() == OrderStatus.FILLED || makerOrder.getStatus() == OrderStatus.CANCELLED) {
                    orderItr.remove();                      // xóa khỏi depth (ở mức giá)
                    orderById.remove(makerOrder.getId());   // xóa khỏi map orderById
                    unholdOrderFunds(makerOrder, product);   // giải phóng số dư hold còn lại của maker
                }

                orderBookSequence++;      // tăng sequence của sổ lệnh (để client đồng bộ trạng thái)
                messageSender.send(orderMessage(makerOrder.clone()));   // gửi message cập nhật order maker
                messageSender.send(tradeMessage(trade));                 // gửi message trade vừa khớp
            }

            // Nếu mức giá này đã hết order (orders trống) → xóa hẳn price line khỏi depth
            if (orders.isEmpty()) {
                depthEntryItr.remove();
            }
        }

        // Bước 4: Sau khi matching xong → xử lý trạng thái còn lại của taker
        if (takerOrder.getType() == OrderType.LIMIT && takerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
            // Nếu là LIMIT và còn dư chưa khớp → giữ lại trong orderbook
            addOrder(takerOrder);
            takerOrder.setStatus(OrderStatus.OPEN);
            orderBookSequence++;
        } else {
            // Nếu là MARKET order hoặc LIMIT nhưng đã không còn size hợp lệ
            if (takerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
                takerOrder.setStatus(OrderStatus.CANCELLED);    // Market mà còn dư → cancel
            } else {
                takerOrder.setStatus(OrderStatus.FILLED);       // Khớp hết → filled
            }
            unholdOrderFunds(takerOrder, product);              // giải phóng số dư hold còn lại (nếu có)
        }
        // Gửi message cuối cùng để thông báo trạng thái của taker order
        messageSender.send(orderMessage(takerOrder.clone()));
    }

    // ---------------------- CANCEL ORDER ----------------------
    public void cancelOrder(String orderId) {
        var order = orderById.remove(orderId);
        if (order == null) {
            return;
        }

        // Xoá order khỏi depth
        var depth = order.getSide() == OrderSide.BUY ? bids : asks;
        depth.removeOrder(order);

        order.setStatus(OrderStatus.CANCELLED);
        messageSender.send(orderMessage(order.clone())); // gửi thông báo cancel


        var product = productBook.getProduct(productId);
        unholdOrderFunds(order, product); // giải phóng số dư hold
    }

    // ---------------------- MATCH TRADE ----------------------
    private Trade trade(Order takerOrder, Order makerOrder) {
        // Giá khớp lấy theo maker order (maker là người chào giá trước, taker chỉ khớp theo)
        BigDecimal price = makerOrder.getPrice();

        // Tính size của taker
        BigDecimal takerSize;
        if (takerOrder.getSide() == OrderSide.BUY && takerOrder.getType() == OrderType.MARKET) {
            // Nếu TAKER là BUY MARKET → không có price, chỉ biết số tiền (funds)
            // => size = funds / price (ví dụ: 1000 USDT / 20000 USDT/BTC = 0.05 BTC)
            takerSize = takerOrder.getRemainingFunds().divide(price, 4, RoundingMode.DOWN);
        } else {
            // Các trường hợp khác (LIMIT BUY/SELL, MARKET SELL) → dùng remaining size
            takerSize = takerOrder.getRemainingSize();
        }

        // Nếu taker không còn gì để khớp → return null (dừng)
        if (takerSize.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        // ---------------------- TÍNH TRADE SIZE ----------------------
        BigDecimal tradeSize = takerSize.min(makerOrder.getRemainingSize()); // tradeSize = min(số lượng taker muốn khớp, số lượng maker còn lại)
        BigDecimal tradeFunds = tradeSize.multiply(price);  // tradeFunds = số tiền tương ứng (tradeSize * price)

        // ---------------------- UPDATE ORDER SAU KHỚP ----------------------
        // Giảm remainingSize cho cả taker và maker
        takerOrder.setRemainingSize(takerOrder.getRemainingSize().subtract(tradeSize));
        makerOrder.setRemainingSize(makerOrder.getRemainingSize().subtract(tradeSize));


        if (takerOrder.getSide() == OrderSide.BUY) {
            // Nếu taker là BUY → còn lại bao nhiêu tiền chưa dùng hết thì trừ tiếp
            takerOrder.setRemainingFunds(takerOrder.getRemainingFunds().subtract(tradeFunds));
        } else {
            // Nếu taker là SELL → maker phải trả tiền, nên giảm remainingFunds của maker
            makerOrder.setRemainingFunds(makerOrder.getRemainingFunds().subtract(tradeFunds));
        }

        // Nếu maker đã hết size → FILLED
        if (makerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) == 0) {
            makerOrder.setStatus(OrderStatus.FILLED);
        }

        // ---------------------- TẠO TRADE OBJECT ----------------------
        Trade trade = new Trade();
        trade.setSequence(++tradeSequence); // tăng trade sequence
        trade.setProductId(productId);       // gắn product (ví dụ BTC-USDT)
        trade.setSize(tradeSize);           // khối lượng khớp
        trade.setFunds(tradeFunds);         // tổng tiền khớp
        trade.setPrice(price);              // giá khớp
        trade.setSide(makerOrder.getSide());  // side theo maker (SELL hay BUY)
        trade.setTime(takerOrder.getTime());  // thời gian khớp (dùng time của taker)
        trade.setTakerOrderId(takerOrder.getId());
        trade.setMakerOrderId(makerOrder.getId());
        return trade;
    }


    // Thêm order mới vào orderbook (khi không khớp hết, hoặc LIMIT còn dư)
    public void addOrder(Order order) {
        var depth = order.getSide() == OrderSide.BUY ? bids : asks;  // chọn depth phù hợp
        depth.addOrder(order);  // thêm vào depth (giá → order list)
        orderById.put(order.getId(), order); // map id → order (tìm kiếm nhanh)
    }

    // Kiểm tra có "crossing" không (giá có khớp được không)
    private boolean isPriceCrossed(Order takerOrder, BigDecimal makerOrderPrice) {
        if (takerOrder.getType() == OrderType.MARKET) { // Market order thì luôn khớp
            return true;
        }
        if (takerOrder.getSide() == OrderSide.BUY) {  // Nếu taker là BUY → giá đặt mua >= giá bán maker → khớp
            return takerOrder.getPrice().compareTo(makerOrderPrice) >= 0;
        } else { // Nếu taker là SELL → giá đặt bán <= giá mua maker → khớp
            return takerOrder.getPrice().compareTo(makerOrderPrice) <= 0;
        }
    }

    // Giải phóng số dư "hold" còn lại sau khi order bị cancel/filled
    private void unholdOrderFunds(Order makerOrder, Product product) {
        if (makerOrder.getSide() == OrderSide.BUY) {
            // Nếu BUY order bị hủy hoặc còn dư tiền chưa dùng
            if (makerOrder.getRemainingFunds().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), product.getQuoteCurrency(), makerOrder.getRemainingFunds());
            }
        } else {
            // Nếu SELL order bị hủy hoặc còn dư coin chưa bán
            if (makerOrder.getRemainingSize().compareTo(BigDecimal.ZERO) > 0) {
                accountBook.unhold(makerOrder.getUserId(), product.getBaseCurrency(), makerOrder.getRemainingSize());
            }
        }
    }


    private OrderMessage orderMessage(Order order) {
        OrderMessage message = new OrderMessage();
        message.setSequence(messageSequence.incrementAndGet());  // Sequence toàn cục cho message (dùng AtomicLong để tăng dần, đảm bảo thứ tự)
        message.setOrderBookSequence(orderBookSequence);  // Gắn sequence riêng của orderBook (theo từng sản phẩm)
        message.setOrder(order);  // Đính kèm order hiện tại vào message
        return message;
    }

    private TradeMessage tradeMessage(Trade trade) {
        TradeMessage message = new TradeMessage();
        message.setSequence(messageSequence.incrementAndGet()); // Sequence toàn cục cho message (mỗi message gửi đi đều có id tăng dần)
        message.setTrade(trade);
        return message;
    }
}


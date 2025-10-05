package org.example.final_usth.matchingengine;

import com.alibaba.fastjson.JSON;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.matchingengine.command.*;
import org.example.final_usth.matchingengine.entity.MessageSender;
import org.example.final_usth.matchingengine.entity.Order;
import org.example.final_usth.matchingengine.entity.Product;
import org.example.final_usth.matchingengine.entity.ProductBook;
import org.example.final_usth.matchingengine.message.CommandEndMessage;
import org.example.final_usth.matchingengine.message.CommandStartMessage;
import org.example.final_usth.matchingengine.snapshot.EngineSnapshotManager;
import org.example.final_usth.matchingengine.snapshot.EngineState;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class MatchingEngine {
    private final Map<String, OrderBook> orderBooks = new HashMap<>(); // lưu tất cả OrderBook theo productId (ví dụ BTC-USDT).
    private final EngineSnapshotManager stateStore;
    private final Counter commandProcessedCounter; // metric để theo dõi số command đã xử lý (dùng Micrometer/Prometheus).
    private final AtomicLong messageSequence = new AtomicLong();
    private final MessageSender messageSender;
    private final ProductBook productBook;
    private final AccountBook accountBook;
    @Getter
    private Long startupCommandOffset; // offset Kafka khi engine start lại (để không xử lý trùng).

    public MatchingEngine(EngineSnapshotManager stateStore, MessageSender messageSender) {
        this.stateStore = stateStore; // quản lý snapshot (MongoDB).
        this.messageSender = messageSender;
        this.commandProcessedCounter = Counter.builder("gbe.matching-engine.command.processed")
                .register(Metrics.globalRegistry);
        this.productBook = new ProductBook(messageSender, this.messageSequence);
        this.accountBook = new AccountBook(messageSender, this.messageSequence);

        restoreSnapshot(stateStore, messageSender); // load lại toàn bộ state (accounts, products, orders) từ MongoDB vào RAM.
    }

    public void executeCommand(Command command, long offset) {
        commandProcessedCounter.increment();

        sendCommandStartMessage(command, offset); // báo cho hệ thống rằng command này bắt đầu.
        if (command instanceof PlaceOrderCommand placeOrderCommand) {
            executeCommand(placeOrderCommand);
        } else if (command instanceof CancelOrderCommand cancelOrderCommand) {
            executeCommand(cancelOrderCommand);
        } else if (command instanceof DepositCommand depositCommand) {
            executeCommand(depositCommand);
        } else if (command instanceof PutProductCommand putProductCommand) {
            executeCommand(putProductCommand);
        } else {
            log.warn("Unhandled command: {} {}", command.getClass().getName(), JSON.toJSONString(command));
        }
        sendCommandEndMessage(command, offset);  // luôn có Start + End message để đảm bảo idempotent + recovery khi hệ thống bị crash giữa chừng.
    }

    private void executeCommand(DepositCommand command) {
        // tăng số dư available và gửi AccountMessage.
        accountBook.deposit(command.getUserId(), command.getCurrency(), command.getAmount(),
                command.getTransactionId());
    }

    private void executeCommand(PutProductCommand command) {
        // đăng ký cặp giao dịch mới và tạo OrderBook rỗng cho nó.
        productBook.putProduct(new Product(command));
        createOrderBook(command.getProductId());
    }

    private void executeCommand(PlaceOrderCommand command) {
        OrderBook orderBook = orderBooks.get(command.getProductId());
        if (orderBook == null) {
            log.warn("no such order book: {}", command.getProductId());
            return;
        }
        // hold tiền trong AccountBook và đưa order vào OrderBook để matching.
        orderBook.placeOrder(new Order(command));
    }

    private void executeCommand(CancelOrderCommand command) {
        OrderBook orderBook = orderBooks.get(command.getProductId());
        if (orderBook == null) {
            log.warn("no such order book: {}", command.getProductId());
            return;
        }
        // unhold tiền và xóa order.
        orderBook.cancelOrder(command.getOrderId());
    }
    // Hai hàm này dùng để gửi Start và End message cho mỗi command.
    private void sendCommandStartMessage(Command command, long offset) {
        CommandStartMessage message = new CommandStartMessage();
        message.setSequence(messageSequence.incrementAndGet()); // tạo số sequence duy nhất
        message.setCommandOffset(offset); // gắn offset Kafka hiện tại
        messageSender.send(message);      // gửi ra Kafka
    }

    private void sendCommandEndMessage(Command command, long offset) {
        CommandEndMessage message = new CommandEndMessage();
        message.setSequence(messageSequence.incrementAndGet());
        message.setCommandOffset(offset);
        messageSender.send(message);
    }
    //Chức năng: Khi engine restart, nó không bắt đầu từ rỗng mà nạp lại state từ MongoDB:
    //offset Kafka → để consumer seek lại đúng vị trí.
    //sequence → đảm bảo message ID tiếp tục đúng thứ tự.
    //accounts, products, order books, orders → đưa hết vào RAM.
    private void restoreSnapshot(EngineSnapshotManager stateStore, MessageSender messageSender) {
        log.info("restoring snapshot");
        stateStore.runInSession(session -> {
            EngineState engineState = stateStore.getEngineState(session);
            if (engineState == null) {
                log.info("no snapshot found");
                return;
            }

            log.info("snapshot found, state: {}", JSON.toJSONString(engineState));

            // khôi phục offset và sequence
            if (engineState.getCommandOffset() != null) {
                this.startupCommandOffset = engineState.getCommandOffset();
            }
            if (engineState.getMessageSequence() != null) {
                this.messageSequence.set(engineState.getMessageSequence());
            }

            // khôi phục product book
            stateStore.getProducts(session).forEach(productBook::addProduct);

            // khôi phục account book
            stateStore.getAccounts(session).forEach(accountBook::add);

            // khôi phục order book + orders
            for (Product product : this.productBook.getAllProducts()) {
                OrderBook orderBook = new OrderBook(product.getId(),
                        engineState.getOrderSequences().getOrDefault(product.getId(), 0L),
                        engineState.getTradeSequences().getOrDefault(product.getId(), 0L),
                        engineState.getOrderBookSequences().getOrDefault(product.getId(), 0L),
                        accountBook, productBook, messageSender, this.messageSequence);
                orderBooks.put(orderBook.getProductId(), orderBook);


                for (Order order : stateStore.getOrders(session, product.getId())) {
                    orderBook.addOrder(order);
                }
            }
        });
        log.info("snapshot restored");
    }
    //Khi có cặp giao dịch mới, tạo sổ lệnh trống trong RAM.
    private void createOrderBook(String productId) {
        if (orderBooks.containsKey(productId)) {
            return;
        }
        OrderBook orderBook = new OrderBook(productId, 0, 0, 0, accountBook, productBook, messageSender, messageSequence);
        orderBooks.put(productId, orderBook);
    }

}


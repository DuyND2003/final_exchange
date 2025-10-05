package org.example.final_usth.matchingengine.snapshot;

import com.alibaba.fastjson.JSON;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;
import org.example.final_usth.enums.OrderStatus;
import org.example.final_usth.matchingengine.entity.Account;
import org.example.final_usth.matchingengine.entity.Order;
import org.example.final_usth.matchingengine.entity.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

// Chức năng chính: lưu và khôi phục trạng thái engine (EngineState, Accounts, Orders, Products)
// Mục đích: khi restart engine, có thể tiếp tục xử lý mà không mất dữ liệu hoặc xử lý trùng lặp

@Slf4j
@Component
public class EngineSnapshotManager {
    private final MongoCollection<EngineState> engineStateCollection;
    private final MongoCollection<Account> accountCollection;
    private final MongoCollection<Order> orderCollection;
    private final MongoCollection<Product> productCollection; // (BTC/USDT,…)
    private final MongoClient mongoClient;

    public EngineSnapshotManager(MongoClient mongoClient, MongoDatabase database) {
        this.mongoClient = mongoClient;
        this.engineStateCollection = database.getCollection("snapshot_engine", EngineState.class);
        this.accountCollection = database.getCollection("snapshot_account", Account.class);
        this.orderCollection = database.getCollection("snapshot_order", Order.class);
        this.orderCollection.createIndex(Indexes.descending("product_id", "sequence"), new IndexOptions().unique(true));
        this.productCollection = database.getCollection("snapshot_product", Product.class);
    }

    // Chạy một đoạn code trong MongoDB session (hỗ trợ transaction / snapshot isolation)
    public void runInSession(Consumer<ClientSession> consumer) {
        try (ClientSession session = mongoClient.startSession(ClientSessionOptions.builder().snapshot(true).build())) {
            consumer.accept(session);
        }
    }

    // Lấy danh sách tất cả Products từ snapshot
    public List<Product> getProducts(ClientSession session) {
        return this.productCollection
                .find(session)
                .into(new ArrayList<>());
    }

    public List<Account> getAccounts(ClientSession session) {
        return this.accountCollection
                .find(session)
                .into(new ArrayList<>());
    }

    public List<Order> getOrders(ClientSession session, String productId) {
        return this.orderCollection
                .find(session, Filters.eq("productId", productId))
                .sort(Sorts.ascending("sequence"))
                .into(new ArrayList<>());
    }
    // Lấy EngineState hiện tại (id luôn = "default")
    public EngineState getEngineState(ClientSession session) {
        return engineStateCollection
                .find(session, Filters.eq("_id", "default"))
                .first();
    }
    // Lưu snapshot của EngineState + Accounts + Orders + Products vào MongoDB
    public void save(EngineState engineState,
                     Collection<Account> accounts,
                     Collection<Order> orders,
                     Collection<Product> products) {
        log.info("saving snapshot: state={}, {} account(s), {} order(s), {} products",
                JSON.toJSONString(engineState), accounts.size(), orders.size(), products.size());

        // Tạo danh sách các lệnh ghi (WriteModel) cho từng loại dữ liệu
        List<WriteModel<Account>> accountWriteModels = buildAccountWriteModels(accounts);
        List<WriteModel<Product>> productWriteModels = buildProductWriteModels(products);
        List<WriteModel<Order>> orderWriteModels = buildOrderWriteModels(orders);
        try (ClientSession session = mongoClient.startSession()) {
            session.startTransaction();
            try {
                // Lưu EngineState: upsert (_id = "default")
                engineStateCollection.replaceOne(session, Filters.eq("_id", engineState.getId()), engineState,
                        new ReplaceOptions().upsert(true));
                // Lưu danh sách Accounts (update hoặc insert nếu chưa có)
                if (!accountWriteModels.isEmpty()) {
                    accountCollection.bulkWrite(session, accountWriteModels, new BulkWriteOptions().ordered(false));
                }

                if (!productWriteModels.isEmpty()) {
                    productCollection.bulkWrite(session, productWriteModels, new BulkWriteOptions().ordered(false));
                }
                // Lưu danh sách Orders
                // - OPEN order → replace/upsert
                // - FILLED/CANCELLED order → delete
                if (!orderWriteModels.isEmpty()) {
                    orderCollection.bulkWrite(session, orderWriteModels, new BulkWriteOptions().ordered(false));
                }

                session.commitTransaction();
            } catch (Exception e) {
                session.abortTransaction();
                throw new RuntimeException(e);
            }
        }
    }
    // Các hàm này đều tạo ra List<WriteModel<T>>, WriteModel là một “lệnh” ghi vào MongoDB (insert, update, replace, delete).
    // code gọi collection.bulkWrite(writeModels) để chạy nhiều thao tác cùng lúc → nhanh hơn gọi từng insert/update riêng.
    private List<WriteModel<Product>> buildProductWriteModels(Collection<Product> products) {
        List<WriteModel<Product>> writeModels = new ArrayList<>();
        if (products.isEmpty()) {
            return writeModels;
        }
        for (Product item : products) {
            Bson filter = Filters.eq("_id", item.getId()); // Với mỗi Product, tìm document có _id = item.getId().
            // 🔹 1. ReplaceOneModel<> Tìm document phù hợp với filter, rồi thay thế toàn bộ document đó bằng item.",
            // Đây là một WriteModel trong MongoDB Java Driver.
            // Tham số item, Đây là object Java (POJO) được map sang MongoDB document. MongoDB sẽ lưu document mới bằng toàn bộ object này.
            WriteModel<Product> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        return writeModels;
    }

    private List<WriteModel<Order>> buildOrderWriteModels(Collection<Order> orders) {
        List<WriteModel<Order>> writeModels = new ArrayList<>();
        if (orders.isEmpty()) {
            return writeModels;
        }
        for (Order item : orders) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Order> writeModel;
            if (item.getStatus() == OrderStatus.OPEN) {
                // nếu lệnh đang mở -> lưu lại
                writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            } else {
                // nếu lệnh đã DONE/CANCEL -> xóa đi khỏi snapshot
                writeModel = new DeleteOneModel<>(filter);
            }
            writeModels.add(writeModel);
        }
        return writeModels;
    }

    private List<WriteModel<Account>> buildAccountWriteModels(Collection<Account> accounts) {
        List<WriteModel<Account>> writeModels = new ArrayList<>();
        if (accounts.isEmpty()) {
            return writeModels;
        }
        for (Account item : accounts) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<Account> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        return writeModels;
    }

}


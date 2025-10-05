package org.example.final_usth.marketdata.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.example.final_usth.marketdata.entity.ProductEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
//Load danh sách sản phẩm vào ProductBook khi khởi động MatchingEngine.
//Lưu lại khi admin thêm cặp giao dịch mới (qua lệnh PutProductCommand).
public class ProductRepository {
    private final MongoCollection<ProductEntity> mongoCollection;

    // Tạo repository cho bảng productentity trong MongoDB
    public ProductRepository(MongoDatabase database) {
        // MongoCollection<ProductEntity> đại diện cho collection trong DB.
        this.mongoCollection = database.getCollection(ProductEntity.class.getSimpleName().toLowerCase(), ProductEntity.class);
    }
    // Tìm document có _id = id.
    // Dùng khi muốn lấy thông tin của một cặp giao dịch cụ thể (ví dụ "BTC-USDT").
    public ProductEntity findById(String id) {
        return this.mongoCollection.find(Filters.eq("_id", id)).first();
    }

    // Truy vấn toàn bộ collection.
    // Trả về danh sách tất cả ProductEntity.
    // Dùng khi khởi động hệ thống để load toàn bộ cặp giao dịch.
    public List<ProductEntity> findAll() {
        return this.mongoCollection.find().into(new ArrayList<>());
    }

    // Tạo filter: _id = product.getId().
    // ReplaceOneModel với upsert(true):
    // bulkWrite với ordered(false) → có thể thực hiện nhiều write operation song song (ở đây chỉ có 1).
    public void save(ProductEntity product) {
        List<WriteModel<ProductEntity>> writeModels = new ArrayList<>();
        Bson filter = Filters.eq("_id", product.getId());
        WriteModel<ProductEntity> writeModel = new ReplaceOneModel<>(filter, product, new ReplaceOptions().upsert(true));
        writeModels.add(writeModel);
        this.mongoCollection.bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }
}


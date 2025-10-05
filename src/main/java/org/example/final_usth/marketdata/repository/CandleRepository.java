package org.example.final_usth.marketdata.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.example.final_usth.api.model.PagedList;
import org.example.final_usth.marketdata.entity.Candle;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  Là lớp DAO truy cập MongoDB collection candle (mỗi document là 1 nến OHLCV).
 *  findById(String id): lấy 1 cây nến theo _id.
 *  findAll(productId, granularity, pageIndex, pageSize): truy vấn danh sách nến có filter + phân trang + sắp xếp theo time giảm dần.
 *  saveAll(Collection<Candle>): ghi hàng loạt (upsert) nhiều nến vào DB.
 * Tính năng biểu đồ giá (chart/candlestick chart) trong sàn giao dịch.
 * Khi hệ thống khớp lệnh (trade) xong, các TradeMessage được đẩy qua Kafka → CandleMakerThread gom chúng lại thành các nến (open, high, low, close, volume) cho từng khoảng thời gian (1m, 5m, 1h…).
 * Những nến này sau đó được lưu vào MongoDB qua CandleRepository.saveAll.
 * API (trong ProductController) sẽ gọi CandleRepository.findAll(...) để lấy dữ liệu nến trả về cho frontend.
 */



@Component
public class CandleRepository {
    private final MongoCollection<Candle> mongoCollection;

    // Lấy collection "candle" (tên = class name viết thường)
    // Kiểu tài liệu được map trực tiếp sang lớp Candle
    public CandleRepository(MongoDatabase database) {
        this.mongoCollection = database.getCollection(Candle.class.getSimpleName().toLowerCase(), Candle.class);
    }

    public Candle findById(String id) {
        // Tìm document theo _id = id, lấy phần tử đầu tiên (hoặc null nếu không có)
        return this.mongoCollection
                .find(Filters.eq("_id", id))
                .first();
    }

    public PagedList<Candle> findAll(String productId, Integer granularity, int pageIndex, int pageSize) {
        // Xây filter động: nếu có productId thì lọc theo productId;
        // nếu có granularity thì lọc thêm; ngược lại để trống.
        Bson filter = Filters.empty();
        if (productId != null) {
            filter = Filters.and(Filters.eq("productId", productId), filter);
        }
        if (granularity != null) {
            filter = Filters.and(Filters.eq("granularity", granularity), filter);
        }

        // Đếm tổng bản ghi để trả về tổng count cho client (phục vụ phân trang UI)
        long count = this.mongoCollection.countDocuments(filter);

        // Truy vấn danh sách:
        // - sort theo time giảm dần (mới nhất trước)
        // - skip & limit để phân trang
        List<Candle> candles = this.mongoCollection.find(filter)
                .sort(Sorts.descending("time"))
                .skip(pageIndex - 1)
                .limit(pageSize)
                .into(new ArrayList<>());
        return new PagedList<>(candles, count);
    }

    public void saveAll(Collection<Candle> candles) {
        List<WriteModel<Candle>> writeModels = new ArrayList<>();
        for (Candle item : candles) {
            // B1. Xác định filter theo _id của nến
            Bson filter = Filters.eq("_id", item.getId());

            // B2. Tạo ReplaceOneModel:
            //  - Nếu có document cùng _id -> replace toàn bộ bằng item mới
            //  - Nếu chưa có -> insert mới (do upsert=true)
            WriteModel<Candle> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            // B3. Thêm vào batch
            writeModels.add(writeModel);
        }
        // B4. Thực hiện bulkWrite toàn bộ batch một lần
        this.mongoCollection.bulkWrite(writeModels);
    }
}



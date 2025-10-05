package org.example.final_usth.marketdata.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.example.final_usth.marketdata.entity.TradeEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Đây là repository thao tác với collection trade trong MongoDB.
 * Mỗi bản ghi trong collection này là một giao dịch thực sự đã khớp (trade) giữa taker và maker.
 * Hiển thị lịch sử khớp lệnh (Recent Trades)
 * Lưu trade sau khi engine khớp lệnh
 * Dữ liệu cho biểu đồ nến (candlestick)
 *
 */
@Component
public class TradeRepository {
    private final MongoCollection<TradeEntity> collection;

    public TradeRepository(MongoDatabase database) {
        this.collection = database.getCollection(TradeEntity.class.getSimpleName().toLowerCase(), TradeEntity.class);
        this.collection.createIndex(Indexes.descending("productId", "sequence"));
    }

    public List<TradeEntity> findByProductId(String productId, int limit) {
        return this.collection.find(Filters.eq("productId", productId))
                .sort(Sorts.descending("sequence"))
                .limit(limit)
                .into(new ArrayList<>());
    }

    public void saveAll(Collection<TradeEntity> trades) {
        List<WriteModel<TradeEntity>> writeModels = new ArrayList<>();
        for (TradeEntity item : trades) {
            Bson filter = Filters.eq("_id", item.getId());
            WriteModel<TradeEntity> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        collection.bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }

}


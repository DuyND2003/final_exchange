package org.example.final_usth.marketdata.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.conversions.Bson;
import org.example.final_usth.marketdata.entity.AccountEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * AccountBook (trong Matching Engine) giữ balances trong RAM để xử lý nhanh (deposit, hold, unhold, exchange).
 * Nhưng dữ liệu không thể chỉ để trong RAM, vì khi service crash thì mất hết.
 * Vì vậy ta cần một lớp Repository để lưu trạng thái account của user xuống MongoDB → đó chính là AccountRepository.
 */
@Component
public class AccountRepository {
    private final MongoCollection<AccountEntity> collection;

    public AccountRepository(MongoDatabase database) {
        this.collection = database.getCollection(AccountEntity.class.getSimpleName().toLowerCase(), AccountEntity.class);
        this.collection.createIndex(Indexes.descending("userId", "currency"), new IndexOptions().unique(true));
    }

    public List<AccountEntity> findAccountsByUserId(String userId) {
        return collection
                .find(Filters.eq("userId", userId))
                .into(new ArrayList<>());
    }

    public void saveAll(Collection<AccountEntity> accounts) {
        List<WriteModel<AccountEntity>> writeModels = new ArrayList<>();
        for (AccountEntity item : accounts) {
            Bson filter = Filters.eq("userId", item.getUserId());
            filter = Filters.and(filter, Filters.eq("currency", item.getCurrency()));
            WriteModel<AccountEntity> writeModel = new ReplaceOneModel<>(filter, item, new ReplaceOptions().upsert(true));
            writeModels.add(writeModel);
        }
        collection.bulkWrite(writeModels, new BulkWriteOptions().ordered(false));
    }
}


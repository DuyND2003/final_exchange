package org.example.final_usth.marketdata.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.example.final_usth.marketdata.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mỗi document trong collection user là thông tin của một người dùng đã đăng ký trên sàn.
 * Khi user nhập email, backend gọi findByEmail(email) để kiểm tra xem có user tồn tại không và đối chiếu password.
 *
 */
@Component
public class UserRepository {
    private final MongoCollection<User> collection;

    public UserRepository(MongoDatabase database) {
        this.collection = database.getCollection(User.class.getSimpleName().toLowerCase(), User.class);
        this.collection.createIndex(Indexes.descending("email"), new IndexOptions().unique(true));
    }

    public User findByEmail(String email) {
        return this.collection.find(Filters.eq("email", email)).first();
    }

    public User findByUserId(String userId) {
        return this.collection.find(Filters.eq("_id", userId)).first();
    }

    public void save(User user) {
        this.collection.insertOne(user);
    }

}


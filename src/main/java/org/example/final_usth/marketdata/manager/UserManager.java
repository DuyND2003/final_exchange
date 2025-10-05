package org.example.final_usth.marketdata.manager;

import lombok.RequiredArgsConstructor;
import org.example.final_usth.marketdata.entity.User;
import org.example.final_usth.marketdata.repository.UserRepository;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class UserManager {
    private final UserRepository userRepository;
    private final RedissonClient redissonClient;
    private final AccountManager accountManager;

    public User createUser(String email, String password) {
        // kiểm tra email đã tồn tại chưa
        User user = userRepository.findByEmail(email);
        if (user != null) {
            throw new RuntimeException("duplicate email address");
        }

        // tạo user mới, sinh UUID và hash password với salt riêng
        user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setEmail(email);
        user.setPasswordSalt(UUID.randomUUID().toString());
        user.setPasswordHash(encryptPassword(password, user.getPasswordSalt()));
        userRepository.save(user);
        return user;
    }

    public String generateAccessToken(User user, String sessionId) {
        // accessToken = userId : sessionId : secret
        String accessToken = user.getId() + ":" + sessionId + ":" + generateAccessTokenSecret(user);

        // lưu token vào Redis với TTL = 14 ngày (value chỉ là ngày hiện tại)
        redissonClient.getBucket(redisKeyForAccessToken(accessToken))
                .set(new Date().toString(), 14, TimeUnit.DAYS);

        return accessToken;
    }

    public void deleteAccessToken(String accessToken) {
        // xóa accessToken trong Redis
        redissonClient.getBucket(redisKeyForAccessToken(accessToken)).delete();
    }

    public User getUserByAccessToken(String accessToken) {
        if (accessToken == null) {
            return null;
        }

        // check token còn tồn tại trong Redis không
        Object val = redissonClient.getBucket(redisKeyForAccessToken(accessToken)).get();
        if (val == null) {
            return null;
        }

        // accessToken phải có đúng 3 phần
        String[] parts = accessToken.split(":");
        if (parts.length != 3) {
            return null;
        }

        String userId = parts[0];
        User user = userRepository.findByUserId(userId);
        if (user == null) {
            return null;
        }

        // xác minh secret trong token có khớp với user không
        if (!parts[2].equals(generateAccessTokenSecret(user))) {
            return null;
        }
        return user;
    }

    public User getUser(String email, String password) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return null;
        }

        // so sánh password đã hash với hash lưu trong DB
        if (user.getPasswordHash().equals(encryptPassword(password, user.getPasswordSalt()))) {
            return user;
        }
        return null;
    }

    // Hash password + salt bằng MD5
    private String encryptPassword(String password, String saltKey) {
        return DigestUtils.md5DigestAsHex((password + saltKey).getBytes(StandardCharsets.UTF_8));
    }

    // Sinh secret cho token dựa vào userId + email + passwordHash
    private String generateAccessTokenSecret(User user) {
        String key = user.getId() + user.getEmail() + user.getPasswordHash();
        return DigestUtils.md5DigestAsHex(key.getBytes(StandardCharsets.UTF_8));
    }

    // Tạo key Redis (prefix "token.")
    private String redisKeyForAccessToken(String accessToken) {
        return "token." + accessToken;
    }
}
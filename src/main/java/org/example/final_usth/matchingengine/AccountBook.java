package org.example.final_usth.matchingengine;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.enums.OrderSide;
import org.example.final_usth.matchingengine.entity.Account;
import org.example.final_usth.matchingengine.entity.MessageSender;
import org.example.final_usth.matchingengine.message.AccountMessage;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

//AccountBook = sổ cái RAM của hệ thống, quản lý available/hold balance.
//Mỗi thay đổi đều phát ra AccountMessage để các dịch vụ khác cập nhật DB, cache, realtime UI.
@Slf4j
@RequiredArgsConstructor
public class AccountBook {
    // accounts lưu theo cấu trúc 2 tầng:
    //  Map<userId, Map<currency, Account>>
    //  Ví dụ: accounts["user1"]["BTC"] -> Account của user1 với BTC
    private final Map<String, Map<String, Account>> accounts = new HashMap<>();
    private final MessageSender messageSender;
    private final AtomicLong messageSequence;

    //Khi restore snapshot từ DB → nạp vào RAM.
    //Khi tạo account mới → lưu vào map.
    public void add(Account account) {
        // computeIfAbsent: nếu userId chưa tồn tại thì tạo mới HashMap cho user đó
        // rồi put Account vào map theo currency
        this.accounts.computeIfAbsent(account.getUserId(), x -> new HashMap<>())
                .put(account.getCurrency(), account);
    }

    // Lấy nhanh Account theo (userId, currency).
    @Nullable
    public Account getAccount(String userId, String currency) {
        // lấy map account của user theo userId
        Map<String, Account> accountMap = accounts.get(userId);
        if (accountMap != null) {
            // nếu tồn tại, trả về account theo currency
            return accountMap.get(currency);
        }
        return null;
    }

    // -------------------- DEPOSIT --------------------
    public void deposit(String userId, String currency, BigDecimal amount, String transactionId) {
        // tìm account theo userId + currency
        Account account = getAccount(userId, currency);
        if (account == null) {
            // nếu chưa có thì tạo account mới cho user + currency
            account = createAccount(userId, currency);
        }
        // tăng số dư khả dụng (available balance)
        account.setAvailable(account.getAvailable().add(amount));


        // gửi message thông báo update account ra Kafka
        // dùng clone() để tránh object trong memory bị thay đổi sau này
        messageSender.send(accountMessage(account.clone()));
    }

    public boolean hold(String userId, String currency, BigDecimal amount) {
        // Kiểm tra số tiền có hợp lệ không (phải > 0)
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            log.error("amount should greater than 0: {}", amount);
            return false;
        }
        Account account = getAccount(userId, currency);
        // Nếu account chưa tồn tại HOẶC available < amount cần hold -> từ chối
        if (account == null || account.getAvailable().compareTo(amount) < 0) {
            return false;
        }
        // Giảm số dư khả dụng (vì tiền này bị "đặt cọc" cho order) và tăng số dư hold (tiền đang bị khóa cho order)
        account.setAvailable(account.getAvailable().subtract(amount));
        account.setHold(account.getHold().add(amount));

        messageSender.send(accountMessage(account.clone()));
        return true;  // Trả về true = hold thành công
    }

    // -------------------- UNHOLD --------------------
    public void unhold(String userId, String currency, BigDecimal amount) {
        // Kiểm tra amount có hợp lệ không
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NullPointerException("amount should greater than 0");
        }
        Account account = getAccount(userId, currency);
        // Nếu account chưa có HOẶC số dư hold < amount cần trả lại -> throw lỗi
        if (account == null || account.getHold().compareTo(amount) < 0) {
            throw new NullPointerException("insufficient funds");
        }
        account.setAvailable(account.getAvailable().add(amount));
        account.setHold(account.getHold().subtract(amount));

        messageSender.send(accountMessage(account.clone()));
    }

    // exchange() chính là bước thanh toán cuối cùng khi lệnh được khớp.
    // Nó xử lý 4 tài khoản liên quan (Taker Base, Taker Quote, Maker Base, Maker Quote).
    public void exchange(String takerUserId, String makerUserId,
                         String baseCurrency, String quoteCurrency,
                         OrderSide takerSide, BigDecimal size, BigDecimal funds) {
        // Lấy account của Taker và Maker cho cả 2 loại tiền:
        // baseCurrency = tài sản gốc (BTC trong cặp BTC/USDT)
        // quoteCurrency = tài sản định giá (USDT trong cặp BTC/USDT)
        Account takerBaseAccount = getAccount(takerUserId, baseCurrency);
        Account takerQuoteAccount = getAccount(takerUserId, quoteCurrency);
        Account makerBaseAccount = getAccount(makerUserId, baseCurrency);
        Account makerQuoteAccount = getAccount(makerUserId, quoteCurrency);

        // Nếu một trong các account chưa tồn tại (user chưa từng giao dịch loại coin này)
        // thì tạo mới account trong RAM
        if (takerBaseAccount == null) {
            takerBaseAccount = createAccount(takerUserId, baseCurrency);
        }
        if (takerQuoteAccount == null) {
            takerQuoteAccount = createAccount(takerUserId, quoteCurrency);
        }
        if (makerBaseAccount == null) {
            makerBaseAccount = createAccount(makerUserId, baseCurrency);
        }
        if (makerQuoteAccount == null) {
            makerQuoteAccount = createAccount(makerUserId, quoteCurrency);
        }
        // ----------------- Cập nhật số dư khi khớp lệnh -----------------
        if (takerSide == OrderSide.BUY) {
            // Nếu Taker là BUY:
            //  - Taker nhận base (vd: BTC)
            takerBaseAccount.setAvailable(takerBaseAccount.getAvailable().add(size));
            //  - Taker mất quote (USDT), trừ vào hold vì đã "đặt cọc" khi tạo lệnh
            takerQuoteAccount.setHold(takerQuoteAccount.getHold().subtract(funds));
            //  - Maker mất base (BTC), trừ vào hold vì lệnh SELL đã "khóa" sẵn BTC
            makerBaseAccount.setHold(makerBaseAccount.getHold().subtract(size));
            //  - Maker nhận quote (USDT), cộng vào available
            makerQuoteAccount.setAvailable(makerQuoteAccount.getAvailable().add(funds));
        } else {
            // Nếu Taker là SELL:
            takerBaseAccount.setHold(takerBaseAccount.getHold().subtract(size));
            takerQuoteAccount.setAvailable(takerQuoteAccount.getAvailable().add(funds));
            makerBaseAccount.setAvailable(makerBaseAccount.getAvailable().add(size));
            makerQuoteAccount.setHold(makerQuoteAccount.getHold().subtract(funds));
        }

        // ----------------- Validate -----------------
        // Sau khi cập nhật, kiểm tra để đảm bảo không account nào bị âm balance
        validateAccount(takerBaseAccount);
        validateAccount(takerQuoteAccount);
        validateAccount(makerBaseAccount);
        validateAccount(makerQuoteAccount);

        // ----------------- Gửi AccountMessage -----------------
        // Thông báo cập nhật balance của cả 4 account qua Kafka
        // để persistence thread ghi xuống Mongo/Redis và WebSocket push về client
        messageSender.send(accountMessage(takerBaseAccount.clone()));
        messageSender.send(accountMessage(takerQuoteAccount.clone()));
        messageSender.send(accountMessage(makerBaseAccount.clone()));
        messageSender.send(accountMessage(makerQuoteAccount.clone()));
    }

    private void validateAccount(Account account) {
        // Nếu số dư khả dụng < 0 HOẶC số dư hold < 0 thì ném lỗi
        if (account.getAvailable().compareTo(BigDecimal.ZERO) < 0 || account.getHold().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("bad account: " + JSON.toJSONString(account));
        }
    }

    //Một user chỉ có account cho loại tiền khi lần đầu họ deposit/trade.
    //Tạo account mới trong RAM nhanh chóng, không cần query DB.
    //MongoDB/Redis sẽ được cập nhật sau thông qua AccountMessage.
    public Account createAccount(String userId, String currency) {
        // Khởi tạo Account mới với userId + currency
        Account account = new Account();
        account.setId(userId + "-" + currency);
        account.setUserId(userId);
        account.setCurrency(currency);
        account.setAvailable(BigDecimal.ZERO);
        account.setHold(BigDecimal.ZERO);
        // Đưa account mới vào map quản lý: accounts[userId][currency] = account
        this.accounts.computeIfAbsent(account.getUserId(), x -> new HashMap<>()).put(account.getCurrency(), account);
        return account;
    }

    private AccountMessage accountMessage(Account account) {
        AccountMessage message = new AccountMessage();
        // Sequence tăng dần, đảm bảo mỗi AccountMessage có ID duy nhất theo thời gian
        message.setSequence(messageSequence.incrementAndGet());
        message.setAccount(account);     // Gắn account snapshot vào message
        return message;
    }
}

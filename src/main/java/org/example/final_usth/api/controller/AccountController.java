package org.example.final_usth.api.controller;

import lombok.RequiredArgsConstructor;
import org.example.final_usth.api.model.AccountDto;
import org.example.final_usth.marketdata.entity.AccountEntity;
import org.example.final_usth.marketdata.entity.User;
import org.example.final_usth.marketdata.manager.AccountManager;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// API này cho phép frontend hiển thị số dư khả dụng (available) và số dư bị giữ (hold) của user.
// Đây là thông tin cực quan trọng để UI hiển thị "Balance" trong màn hình trading.
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AccountController {
    private final AccountManager accountManager;

    @GetMapping("/accounts")
    public List<AccountDto> getAccounts(@RequestParam(name = "currency") List<String> currencies,
                                        @RequestAttribute(required = false) User currentUser) {
        if (currentUser == null) { // Nếu không có user (chưa đăng nhập hoặc token invalid)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        // Lấy tất cả account của user từ DB, Convert thành Map<currency, AccountEntity> để tra cứu nhanh.
        List<AccountEntity> accounts = accountManager.getAccounts(currentUser.getId());
        Map<String, AccountEntity> accountByCurrency = accounts.stream()
                .collect(Collectors.toMap(AccountEntity::getCurrency, x -> x));

        List<AccountDto> accountDtoList = new ArrayList<>();
        for (String currency : currencies) { // Duyệt qua từng currency user yêu cầu.
            AccountEntity account = accountByCurrency.get(currency);
            if (account != null) {
                accountDtoList.add(accountDto(account)); // Nếu có trong DB → convert sang AccountDto.
            } else {
                // Nếu không có → tạo AccountDto mới với số dư mặc định bằng "0".
                AccountDto accountDto = new AccountDto();
                accountDto.setCurrency(currency);
                accountDto.setAvailable("0");
                accountDto.setHold("0");
                accountDtoList.add(accountDto);
            }
        }
        return accountDtoList;
    }
    // Helper method để convert từ AccountEntity (DB entity) sang AccountDto (object trả về API).
    private AccountDto accountDto(AccountEntity account) {
        AccountDto accountDto = new AccountDto();
        accountDto.setId(account.getId());
        accountDto.setCurrency(account.getCurrency());
        // Dùng toPlainString() để convert BigDecimal thành chuỗi JSON dễ đọc
        accountDto.setAvailable(account.getAvailable() != null ? account.getAvailable().toPlainString() : "0");
        accountDto.setHold(account.getHold() != null ? account.getHold().toPlainString() : "0");
        return accountDto;
    }

}


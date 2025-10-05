package org.example.final_usth.marketdata.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.marketdata.entity.AccountEntity;
import org.example.final_usth.marketdata.repository.AccountRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountManager {
    private final AccountRepository accountRepository;

    // Lấy toàn bộ tài khoản (account) theo userId
    public List<AccountEntity> getAccounts(String userId) {
        return accountRepository.findAccountsByUserId(userId);
    }

    // Lưu nhiều account cùng lúc
    public void saveAll(Collection<AccountEntity> accounts) {
        if (accounts.isEmpty()) {
            return;
        }

        long t1 = System.currentTimeMillis();
        accountRepository.saveAll(accounts);
        log.info("saved {} account(s) ({}ms)", accounts.size(), System.currentTimeMillis() - t1);
    }
}


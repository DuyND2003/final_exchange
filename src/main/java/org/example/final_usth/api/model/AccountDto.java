package org.example.final_usth.api.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Đây là object trả về trong API Account (ví dụ /api/accounts).
 * Được xây dựng từ AccountEntity (trong DB) hoặc từ Account (trong RAM) → chuyển sang dạng String để client dễ đọc.
 * Dùng trong feature ví tài khoản (Wallet/Balance), cho user xem mình có bao nhiêu tiền khả dụng và bao nhiêu đang bị khóa trong lệnh.
 */
@Getter
@Setter
public class AccountDto {
    private String id;
    private String currency;
    private String currencyIcon;
    private String available;
    private String hold;
}


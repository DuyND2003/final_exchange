package org.example.final_usth.marketdata.entity;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Ticker {
    private String productId;
    private long tradeId; // ID của trade cuối cùng
    private long sequence;
    private Date time;
    private BigDecimal price;
    private OrderSide side;
    private BigDecimal lastSize; // Giá trade cuối cùng
    // Thống kê 1 giờ
    private Long time24h;
    private BigDecimal open24h;
    private BigDecimal close24h;
    private BigDecimal high24h;
    private BigDecimal low24h;
    private BigDecimal volume24h;
    // Thống kê 30 ngày
    private Long time30d;
    private BigDecimal open30d;
    private BigDecimal close30d;
    private BigDecimal high30d;
    private BigDecimal low30d;
    private BigDecimal volume30d;

}

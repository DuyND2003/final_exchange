package org.example.final_usth.marketdata.entity;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class TradeEntity {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private long sequence;
    private String productId;
    private String takerOrderId; // Lệnh của taker (người khớp ngay)
    private String makerOrderId;  // Lệnh của maker (người đặt chờ sẵn trên order book)
    private BigDecimal price;
    private BigDecimal size;
    private OrderSide side;
    private Date time;
}


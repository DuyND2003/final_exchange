package org.example.final_usth.matchingengine.entity;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Trade {
    private String productId;
    private long sequence;
    private BigDecimal size;
    private BigDecimal funds;
    private BigDecimal price;
    private Date time;
    private OrderSide side;
    private String takerOrderId;
    private String makerOrderId;
}

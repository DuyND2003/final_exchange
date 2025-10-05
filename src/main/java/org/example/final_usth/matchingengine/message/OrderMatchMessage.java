package org.example.final_usth.matchingengine.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderMatchMessage extends OrderBookMessage {
    private String productId;
    private long sequence;
    private long tradeId;
    private String takerOrderId;
    private String makerOrderId;
    private String takerUserId;
    private String makerUserId;
    private OrderSide side;
    private BigDecimal price;
    private BigDecimal size;
    private BigDecimal funds;

    public OrderMatchMessage() {
        this.setType(MessageType.ORDER_MATCH);
    }

}

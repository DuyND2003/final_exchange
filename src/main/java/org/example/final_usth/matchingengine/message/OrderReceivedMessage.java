package org.example.final_usth.matchingengine.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;
import org.example.final_usth.enums.OrderType;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class OrderReceivedMessage extends OrderBookMessage {
    private String orderId;
    private String userId;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private OrderSide side;
    private OrderType orderType;
    private String clientOid;
    private Date time;

    public OrderReceivedMessage() {
        this.setType(MessageType.ORDER_RECEIVED);
    }
}

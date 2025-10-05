package org.example.final_usth.matchingengine.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderOpenMessage extends OrderBookMessage {
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal price;
    private OrderSide side;
    private String userId;

    public OrderOpenMessage() {
        this.setType(MessageType.ORDER_OPEN);
    }

}

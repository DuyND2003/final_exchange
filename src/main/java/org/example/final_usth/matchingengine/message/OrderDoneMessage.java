package org.example.final_usth.matchingengine.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;
import org.example.final_usth.enums.OrderType;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderDoneMessage extends OrderBookMessage {
    private String orderId;
    private BigDecimal remainingSize;
    private BigDecimal remainingFunds;
    private BigDecimal price;
    private OrderSide side;
    private OrderType orderType;
    private String doneReason;
    private String userId;

    public OrderDoneMessage() {
        this.setType(MessageType.ORDER_DONE);
    }
}

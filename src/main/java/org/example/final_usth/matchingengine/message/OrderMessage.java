package org.example.final_usth.matchingengine.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.matchingengine.entity.Order;

@Getter
@Setter
public class OrderMessage extends Message{
    private long orderBookSequence;
    private Order order;

    public OrderMessage(){this.setMessageType(MessageType.ORDER);}
}

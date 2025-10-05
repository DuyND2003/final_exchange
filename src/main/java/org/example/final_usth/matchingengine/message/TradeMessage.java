package org.example.final_usth.matchingengine.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.matchingengine.entity.Trade;

@Getter
@Setter
public class TradeMessage extends Message {
    private Trade trade;

    public TradeMessage() {
        this.setMessageType(MessageType.TRADE);
    }
}


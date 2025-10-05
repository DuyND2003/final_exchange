package org.example.final_usth.matchingengine.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderCommand extends Command{
    private String productId;
    private String orderId;

    public CancelOrderCommand(){ this.setType(CommandType.CANCEL_ORDER);}
}

package org.example.final_usth.matchingengine.command;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;
import org.example.final_usth.enums.OrderType;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class PlaceOrderCommand extends Command{
    private String productId;
    private String orderId;
    private String userId;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private OrderType orderType;
    private OrderSide orderSide;
    private Date time;

    public PlaceOrderCommand(){ this.setType(CommandType.PLACE_ORDER);}
}

package org.example.final_usth.matchingengine.entity;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.enums.OrderSide;
import org.example.final_usth.enums.OrderStatus;
import org.example.final_usth.enums.OrderType;
import org.example.final_usth.matchingengine.command.Command;
import org.example.final_usth.matchingengine.command.PlaceOrderCommand;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class Order implements Cloneable{
    private String id;
    private long sequence;
    private String userId;
    private OrderType type;
    private OrderSide side;
    private BigDecimal price;
    private BigDecimal remainingSize;
    private BigDecimal remainingFunds;
    private BigDecimal size;
    private BigDecimal funds;
    private Date time;
    private String productId;
    private String clientOid;
    private OrderStatus status;

    public Order(PlaceOrderCommand command){
        this.productId = command.getProductId();
        this.userId = command.getUserId();
        this.type = command.getOrderType();
        this.side = command.getOrderSide();
        this.price = command.getPrice();
        this.size = command.getSize();
        this.id = command.getUserId();
        if(command.getOrderType() == OrderType.LIMIT){
            this.funds = command.getSize().multiply(command.getPrice());
        } else {
            this.funds = command.getFunds();
        }
        this.remainingSize= this.size;
        this.remainingFunds = this.funds;
        this.time = command.getTime();
    }
    @Override
    public Order clone() {
        try {
            return (Order) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

}

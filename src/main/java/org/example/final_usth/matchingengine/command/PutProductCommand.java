package org.example.final_usth.matchingengine.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PutProductCommand extends Command{
    private String productId;
    private String baseCurrency;
    private String quoteCurrency;

    public PutProductCommand(){this.setType(CommandType.PUT_PRODUCT);}
}

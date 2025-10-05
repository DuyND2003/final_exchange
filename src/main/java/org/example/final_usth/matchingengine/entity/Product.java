package org.example.final_usth.matchingengine.entity;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.matchingengine.command.PutProductCommand;

@Getter
@Setter
public class Product implements Cloneable {
    private String id;
    private String baseCurrency;
    private String quoteCurrency;

    public Product() {
    }

    public Product(PutProductCommand command) {
        this.id = command.getProductId();
        this.baseCurrency = command.getBaseCurrency();
        this.quoteCurrency = command.getQuoteCurrency();
    }

    @Override
    public Product clone() {
        try {
            return (Product) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}



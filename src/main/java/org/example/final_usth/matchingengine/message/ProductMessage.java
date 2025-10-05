package org.example.final_usth.matchingengine.message;

import lombok.Getter;
import lombok.Setter;
import org.example.final_usth.matchingengine.entity.Product;

@Getter
@Setter
public class ProductMessage extends Message {
    private Product product;

    public ProductMessage() {
        this.setMessageType(MessageType.PRODUCT);
    }
}


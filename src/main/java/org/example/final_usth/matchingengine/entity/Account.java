package org.example.final_usth.matchingengine.entity;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
public class Account implements Cloneable{
    private String id;
    private BigDecimal hold;
    private BigDecimal available;
    private String userId;
    private String currency;

    public Account clone(){
        try{
            return (Account) super.clone();
        }catch(CloneNotSupportedException e){
            throw new AssertionError();
        }
    }
}

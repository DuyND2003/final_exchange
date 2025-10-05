package org.example.final_usth.marketdata.entity;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class ProductEntity {
    private String id;
    private Date createdAt;
    private Date updatedAt;
    private String baseCurrency;  // Tiền cơ sở (BTC)
    private String quoteCurrency;  // Tiền định giá (USDT)
    private BigDecimal baseMinSize;
    private BigDecimal baseMaxSize;
    private BigDecimal quoteMinSize;
    private BigDecimal quoteMaxSize;
    private int baseScale; // Số chữ số thập phân cho base
    private int quoteScale;
    private float quoteIncrement;
}
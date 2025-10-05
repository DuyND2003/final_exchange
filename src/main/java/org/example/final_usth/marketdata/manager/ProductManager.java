package org.example.final_usth.marketdata.manager;

import lombok.RequiredArgsConstructor;
import org.example.final_usth.marketdata.repository.ProductRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductManager {
    private final ProductRepository productRepository;

}

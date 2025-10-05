package org.example.final_usth.api.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.example.final_usth.marketdata.entity.ProductEntity;
import org.example.final_usth.marketdata.entity.User;
import org.example.final_usth.marketdata.manager.AccountManager;
import org.example.final_usth.marketdata.manager.UserManager;
import org.example.final_usth.marketdata.repository.ProductRepository;
import org.example.final_usth.matchingengine.command.CancelOrderCommand;
import org.example.final_usth.matchingengine.command.DepositCommand;
import org.example.final_usth.matchingengine.command.MatchingEngineCommandProducer;
import org.example.final_usth.matchingengine.command.PutProductCommand;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AdminController {
    private final MatchingEngineCommandProducer producer;
    private final AccountManager accountManager;
    private final ProductRepository productRepository;
    private final UserManager userManager;

    @GetMapping("/api/admin/createUser")
    public User createUser(String email, String password) {
        User user = userManager.getUser(email, password);
        if (user != null) {
            return user;
        }
        return userManager.createUser(email, password);
    }

    @GetMapping("/api/admin/deposit")
    public String deposit(@RequestParam String userId, @RequestParam String currency, @RequestParam String amount) {
        DepositCommand command = new DepositCommand();
        command.setUserId(userId);
        command.setCurrency(currency);
        command.setAmount(new BigDecimal(amount));
        command.setTransactionId(UUID.randomUUID().toString());
        producer.send(command, null);
        return "ok";
    }

    @PutMapping("/api/admin/products")
    public ProductEntity saveProduct(@RequestBody @Valid PutProductRequest request) {
        String productId = request.getBaseCurrency() + "-" + request.getQuoteCurrency();
        ProductEntity product = new ProductEntity();
        product.setId(productId);
        product.setBaseCurrency(request.baseCurrency);
        product.setQuoteCurrency(request.quoteCurrency);
        product.setBaseScale(6);
        product.setQuoteScale(2);
        product.setBaseMinSize(BigDecimal.ZERO);
        product.setBaseMaxSize(new BigDecimal("100000000"));
        product.setQuoteMinSize(BigDecimal.ZERO);
        product.setQuoteMaxSize(new BigDecimal("10000000000"));
        productRepository.save(product);

        PutProductCommand putProductCommand = new PutProductCommand();
        putProductCommand.setProductId(product.getId());
        putProductCommand.setBaseCurrency(product.getBaseCurrency());
        putProductCommand.setQuoteCurrency(product.getQuoteCurrency());
        producer.send(putProductCommand, null);

        return product;
    }

    public void cancelOrder(String orderId, String productId) {
        CancelOrderCommand command = new CancelOrderCommand();
        command.setProductId(productId);
        command.setOrderId(orderId);
        producer.send(command, null);
    }

    @Getter
    @Setter
    public static class PutProductRequest {
        @NotBlank
        private String baseCurrency;
        @NotBlank
        private String quoteCurrency;

    }

}


package org.example.final_usth.marketdata.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.marketdata.entity.OrderEntity;
import org.example.final_usth.marketdata.repository.OrderRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderManager {
    private final OrderRepository orderRepository;

    public void saveAll(Collection<OrderEntity> orders) {
        if (orders.isEmpty()) {
            return;
        }
        long t1 = System.currentTimeMillis();
        orderRepository.saveAll(orders);
        log.info("saved {} order(s) ({}ms)", orders.size(), System.currentTimeMillis() - t1);
    }
}

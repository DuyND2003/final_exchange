package org.example.final_usth.marketdata.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.marketdata.entity.TradeEntity;
import org.example.final_usth.marketdata.repository.TradeRepository;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
@RequiredArgsConstructor
@Slf4j
public class TradeManager {
    private final TradeRepository tradeRepository;

    public void saveAll(Collection<TradeEntity> trades) {
        if (trades.isEmpty()) {
            return;
        }

        long t1 = System.currentTimeMillis();
        tradeRepository.saveAll(trades);
        log.info("saved {} trade(s) ({}ms)", trades.size(), System.currentTimeMillis() - t1);
    }
}

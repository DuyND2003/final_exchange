package org.example.final_usth.matchingengine;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.final_usth.matchingengine.entity.MessageSender;
import org.example.final_usth.matchingengine.snapshot.EngineSnapshotManager;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MatchingEngineLoader {
    private final EngineSnapshotManager engineSnapshotManager; //dùng để load snapshot (account, order book, trade state) từ MongoDB.
    private final MessageSender messageSender;
    @Getter
    @Nullable
    private volatile MatchingEngine preperedMatchingEngine; // giữ một instance của MatchingEngine mới nhất được load từ snapshot, luôn ready trong RAM để có thể thay thế nhanh.

    // Khi Spring khởi tạo bean này → constructor chạy → gọi ngay startRefreshPreparingMatchingEnginePeriodically().
    // Như vậy, hệ thống luôn có background job load lại snapshot định kỳ.
    public MatchingEngineLoader(EngineSnapshotManager engineSnapshotManager, MessageSender messageSender) {
        this.engineSnapshotManager = engineSnapshotManager;
        this.messageSender = messageSender;
        startRefreshPreparingMatchingEnginePeriodically();
    }

    private void startRefreshPreparingMatchingEnginePeriodically() {
        // Tạo một thread pool chỉ có 1 thread chạy background job.
        // chạy job liên tục, lặp lại sau mỗi lần hoàn tất (ở đây là mỗi 1 phút).
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(() -> {
            try {
                log.info("reloading latest snapshot");
                //Tạo mới một MatchingEngine từ snapshot (new MatchingEngine(engineSnapshotManager, messageSender)).
                //Gán nó cho preperedMatchingEngine
                preperedMatchingEngine = new MatchingEngine(engineSnapshotManager, messageSender);
                log.info("done");
            } catch (Exception e) {
                log.error("matching engine create error: {}", e.getMessage(), e);
            }
        }, 0, 1, TimeUnit.MINUTES);
    }
}

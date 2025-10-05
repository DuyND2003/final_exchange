package org.example.final_usth.middleware.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
public abstract class KafkaConsumerThread<K, V> extends Thread {
    protected final KafkaConsumer<K, V> consumer;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Logger logger;

    @Override
    public void run() {
        logger.info("starting...");
        try {
            doSubscribe();
            while (!closed.get()) {
                doPoll();
            }
        } catch (WakeupException e) {
            if (!closed.get()) {
                throw e;
            }
        } catch (Exception e) {
            logger.error("consumer error: {}", e.getMessage(), e);
        } finally {
            consumer.close();
        }
        logger.info("exiting...");
    }

    public void shutdown() {
        closed.set(true);
        consumer.wakeup();
    }

    @Override
    public void interrupt() {
        this.shutdown();
        super.interrupt();
    }

    protected abstract void doSubscribe();

    protected abstract void doPoll();
}

package org.example.final_usth.matchingengine.entity;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.example.final_usth.AppProperties;
import org.example.final_usth.matchingengine.message.Message;
import org.example.final_usth.matchingengine.message.MessageSerializer;
import org.example.final_usth.middleware.kafka.KafkaProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Slf4j
@Component
// Đây là một Kafka Producer wrapper dùng để gửi các message (Message) từ Matching Engine sang Kafka topi
// hệ thống tạo ra event (Order, Trade, Account update, …) → MessageSender → đẩy ra Kafka → các service khác (snapshot, market data, API, …) sẽ đọc và xử lý.
public class MessageSender {
    private final AppProperties appProperties;
    private final KafkaProperties kafkaProperties;
    private final KafkaProducer<String, Message> kafkaProducer;

    public MessageSender(AppProperties appProperties, KafkaProperties kafkaProperties) {
        this.appProperties = appProperties;
        this.kafkaProperties = kafkaProperties;
        this.kafkaProducer = kafkaProducer();
    }

    // Gửi 1 message tới Kafka topic
    public void send(Message message) {
        ProducerRecord<String, Message> record = new ProducerRecord<>(appProperties.getMatchingEngineMessageTopic(), message);
        kafkaProducer.send(record);
    }

    private KafkaProducer<String, Message> kafkaProducer() {
        Properties properties = new Properties();
        // Địa chỉ Kafka broker
        properties.put("bootstrap.servers", kafkaProperties.getBootstrapServers());
        // Key serializer: String
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        // Value serializer: custom MessageSerializer (serialize Message -> byte[])
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MessageSerializer.class.getName());
        // Nén dữ liệu bằng Zstd để giảm kích thước
        properties.put("compression.type", "zstd");
        // Retry vô hạn khi có lỗi (tối đa Integer.MAX_VALUE)
        properties.put("retries", 2147483647);
        // Gửi batch: chờ 100ms để gom message trước khi gửi
        properties.put("linger.ms", 100);
        // Kích thước batch gửi (2 * 16384 = 32KB), giảm overhead, tăng throughput, giúp Kafka vừa nhanh, vừa tiết kiệm tài nguyên mạng.
        properties.put("batch.size", 16384 * 2);
        // Bật idempotent (quan trọng để tránh gửi trùng message khi retry), Kafka đảm bảo dù retry bao nhiêu lần, broker cũng chỉ ghi message đó đúng một lần.
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        properties.put("max.in.flight.requests.per.connection", 5);
        // ACKS=all: broker chỉ báo OK khi message đã replicate đầy đủ
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        // Tạo producer với config trên
        return new KafkaProducer<>(properties);
    }
}


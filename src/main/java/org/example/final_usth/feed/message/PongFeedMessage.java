package org.example.final_usth.feed.message;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Trả lời cho tín hiệu ping/pong của WebSocket để giữ kết nối sống
 * Khi client gửi “ping”
 */
public class PongFeedMessage {
    private String type;
}

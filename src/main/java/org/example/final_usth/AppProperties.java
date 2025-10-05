package org.example.final_usth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "me")
@Getter
@Setter
@Validated
public class AppProperties {
    private String matchingEngineCommandTopic;
    private String matchingEngineMessageTopic;
}

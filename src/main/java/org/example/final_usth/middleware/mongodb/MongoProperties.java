package org.example.final_usth.middleware.mongodb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@ConfigurationProperties(prefix = "mongodb")
@Validated
public class MongoProperties {
    private String uri;
}

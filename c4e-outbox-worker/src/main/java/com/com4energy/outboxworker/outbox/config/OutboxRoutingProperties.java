package com.com4energy.outboxworker.outbox.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "c4e.outbox.routing")
public class OutboxRoutingProperties {

    private Map<String, PublishTarget> types = new HashMap<>();

    public Optional<PublishTarget> forEventType(String eventType) {
        return Optional.ofNullable(types.get(eventType));
    }

    public record PublishTarget(String exchange, String routingKey) {}

}

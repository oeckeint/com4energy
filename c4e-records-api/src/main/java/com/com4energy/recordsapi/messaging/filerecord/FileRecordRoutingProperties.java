package com.com4energy.recordsapi.messaging.filerecord;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;

/**
 * Propiedades de routing para los eventos de file records publicados
 * por el ingestion-service via outbox.
 *
 * <p>Cada entrada en {@code types} representa un tipo de evento con su cola,
 * exchange, routing key y dead letter queue asociados.</p>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "c4e.file-records")
public class FileRecordRoutingProperties {

    private Map<String, QueueConfig> types = new HashMap<>();

    @Getter
    @Setter
    public static class QueueConfig {
        private String eventType;
        private String queue;
        private String exchange;
        private String routingKey;
        private String deadLetterExchange;
        private String deadLetterQueue;
    }

}

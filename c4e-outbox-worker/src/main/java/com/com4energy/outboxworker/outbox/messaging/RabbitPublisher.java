package com.com4energy.outboxworker.outbox.messaging;

import java.util.Map;

import com.com4energy.outboxworker.outbox.config.OutboxRoutingProperties;
import com.com4energy.outboxworker.outbox.config.OutboxRoutingProperties.PublishTarget;
import com.com4energy.outboxworker.outbox.repository.OutboxEventRepository.OutboxEventRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@Slf4j
public class RabbitPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final OutboxRoutingProperties routingProperties;

    public RabbitPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                           OutboxRoutingProperties routingProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.routingProperties = routingProperties;
    }

    @PostConstruct
    void logRoutingTable() {
        log.info("=== OutboxWorker routing table ===");
        routingProperties.getTypes().forEach((eventType, target) ->
                log.info("  eventType={} -> exchange={} routingKey={}",
                        eventType, target.exchange(), target.routingKey())
        );
        log.info("==================================");
    }

    public void publish(OutboxEventRecord event) {
        sendToTarget(event, readFileRecordPayload(event.payload()));
    }

    private void sendToTarget(OutboxEventRecord event, Object payload) {
        PublishTarget target = routingProperties.forEventType(event.eventType())
                .orElseThrow(() -> new IllegalStateException(
                        "No routing configured for eventType=" + event.eventType()
                ));

        log.info("Publishing outbox event id={} type={} to exchange={} routingKey={}",
                event.id(), event.eventType(), target.exchange(), target.routingKey());

        rabbitTemplate.convertAndSend(target.exchange(), target.routingKey(), payload);
    }

    private Map<String, Object> readFileRecordPayload(String payload) {
        try {
            return objectMapper.readValue(payload, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid outbox payload: " + ex.getMessage(), ex);
        }
    }

}


package com.com4energy.outboxworker.outbox.service;

import com.com4energy.outboxworker.outbox.messaging.RabbitPublisher;
import com.com4energy.outboxworker.outbox.repository.OutboxEventRepository;
import com.com4energy.outboxworker.outbox.repository.OutboxEventRepository.OutboxEventRecord;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OutboxProcessor {

    private final OutboxEventRepository repository;
    private final RabbitPublisher rabbitPublisher;

    public OutboxProcessor(OutboxEventRepository repository, RabbitPublisher rabbitPublisher) {
        this.repository = repository;
        this.rabbitPublisher = rabbitPublisher;
    }

    public void process(OutboxEventRecord event) {
        try {
            rabbitPublisher.publish(event);
            repository.markProcessed(event.id());
            log.debug("Outbox event published. id={}", event.id());
        } catch (Exception ex) {
            repository.markFailed(event.id(), ex.getMessage());
            log.error("Outbox event failed. id={}", event.id(), ex);
        }
    }

}

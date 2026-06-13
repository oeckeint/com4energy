package com.com4energy.outboxworker.outbox.service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.UUID;

import com.com4energy.outboxworker.outbox.repository.OutboxEventRepository;
import com.com4energy.outboxworker.outbox.repository.OutboxEventRepository.OutboxEventRecord;
import org.springframework.transaction.support.TransactionTemplate;

public class OutboxPollingService {

    private final OutboxEventRepository repository;
    private final OutboxProcessor outboxProcessor;
    private final TransactionTemplate transactionTemplate;
    private final int batchSize;
    private final String workerId;

    public OutboxPollingService(
            OutboxEventRepository repository,
            OutboxProcessor outboxProcessor,
            TransactionTemplate transactionTemplate,
            int batchSize,
            String workerId
    ) {
        this.repository = repository;
        this.outboxProcessor = outboxProcessor;
        this.transactionTemplate = transactionTemplate;
        this.batchSize = batchSize;
        this.workerId = workerId == null || workerId.isBlank() ? buildWorkerId() : workerId;
    }

    public void pollAndDispatch() {
        List<OutboxEventRecord> claimedEvents = claimPendingBatch();
        if (claimedEvents.isEmpty()) {
            return;
        }
        for (OutboxEventRecord event : claimedEvents) {
            outboxProcessor.process(event);
        }
    }

    private List<OutboxEventRecord> claimPendingBatch() {
        List<OutboxEventRecord> events = transactionTemplate.execute(status -> {
            List<Long> ids = repository.findPendingIdsForUpdate(batchSize);
            if (ids.isEmpty()) {
                return List.of();
            }
            repository.markAsProcessing(ids, workerId);
            return repository.findByIds(ids);
        });

        return events == null ? List.of() : events;
    }

    private String buildWorkerId() {
        try {
            return InetAddress.getLocalHost().getHostName() + "-" + UUID.randomUUID();
        } catch (UnknownHostException ex) {
            return "outbox-worker-" + UUID.randomUUID();
        }
    }
}


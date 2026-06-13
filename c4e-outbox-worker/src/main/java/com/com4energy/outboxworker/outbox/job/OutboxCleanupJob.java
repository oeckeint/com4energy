package com.com4energy.outboxworker.outbox.job;

import com.com4energy.outboxworker.outbox.repository.OutboxEventRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class OutboxCleanupJob {

    private final OutboxEventRepository repository;
    private final int retentionDays;
    private final int batchSize;

    public OutboxCleanupJob(OutboxEventRepository repository, int retentionDays, int batchSize) {
        this.repository = repository;
        this.retentionDays = retentionDays;
        this.batchSize = batchSize;
    }

    @Scheduled(cron = "${c4e.outbox.cleanup.cron:0 0 * * * *}")
    public void cleanup() {
        if (retentionDays <= 0 || batchSize <= 0) {
            log.warn("Skipping outbox cleanup due to invalid config: retentionDays={}, batchSize={}. Expected both > 0.",
                    retentionDays, batchSize);
            return;
        }

        log.info("Outbox cleanup started. retentionDays={}, batchSize={}", retentionDays, batchSize);

        int totalDeleted = 0;
        int deleted;

        do {
            deleted = repository.deleteProcessedOlderThan(retentionDays, batchSize);
            totalDeleted += deleted;
        } while (deleted > 0);

        if (totalDeleted > 0) {
            log.info("Outbox cleanup finished. Total deleted: {}", totalDeleted);
        } else {
            log.debug("Outbox cleanup finished. Nothing to delete.");
        }
    }

}

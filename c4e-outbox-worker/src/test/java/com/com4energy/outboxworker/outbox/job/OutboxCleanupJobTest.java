package com.com4energy.outboxworker.outbox.job;

import com.com4energy.outboxworker.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxCleanupJobTest {

    @Test
    void cleanupShouldDeleteInBatchesUntilNoRecordsLeft() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        when(repository.deleteProcessedOlderThan(anyInt(), anyInt())).thenReturn(500, 120, 0);

        OutboxCleanupJob job = new OutboxCleanupJob(repository, 7, 500);
        job.cleanup();

        verify(repository, times(3)).deleteProcessedOlderThan(7, 500);
    }

    @Test
    void cleanupShouldExitWhenConfigurationIsInvalid() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);

        OutboxCleanupJob job = new OutboxCleanupJob(repository, 7, 0);
        job.cleanup();

        verify(repository, times(0)).deleteProcessedOlderThan(anyInt(), anyInt());
    }
}


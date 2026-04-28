package com.com4energy.outboxworker.outbox.worker;

import com.com4energy.outboxworker.outbox.service.OutboxPollingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class OutboxWorker {

	private final OutboxPollingService pollingService;

	public OutboxWorker(OutboxPollingService pollingService) {
		this.pollingService = pollingService;
	}

	@Scheduled(fixedDelayString = "${c4e.outbox.worker.poll-interval-ms:5000}")
	public void run() {
		pollingService.pollAndDispatch();
	}

}

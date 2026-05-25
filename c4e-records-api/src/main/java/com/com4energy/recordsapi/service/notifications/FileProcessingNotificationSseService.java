package com.com4energy.recordsapi.service.notifications;

import com.com4energy.recordsapi.controller.notifications.dto.FileProcessingNotificationResponse;
import com.com4energy.recordsapi.domain.entity.messaging.FileRecordEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class FileProcessingNotificationSseService {

    private static final long SSE_TIMEOUT_MS = 0L;
    private static final int HEARTBEAT_SECONDS = 20;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    public FileProcessingNotificationSseService() {
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, HEARTBEAT_SECONDS, HEARTBEAT_SECONDS, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(ex -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ex) {
            emitters.remove(emitter);
            emitter.complete();
        }

        return emitter;
    }

    public void publish(FileRecordEvent event) {
        FileProcessingNotificationResponse payload = FileProcessingNotificationResponse.builder()
                .id(event.getId())
                .eventType(event.getEventType())
                .status(event.getStatus())
                .filename(event.getFilename())
                .fileType(event.getFileType())
                .origin(event.getOrigin())
                .failureReason(event.getFailureReason())
                .comment(event.getComment())
                .occurredAt(event.getOccurredAt())
                .receivedAt(event.getReceivedAt())
                .build();

        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("file-record-event").data(payload));
            } catch (Exception ex) {
                emitters.remove(emitter);
                emitter.complete();
            }
        });
    }

    private void sendHeartbeat() {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
            } catch (Exception ex) {
                emitters.remove(emitter);
                emitter.complete();
            }
        });
    }

    @PreDestroy
    void shutdown() {
        heartbeatExecutor.shutdownNow();
        emitters.forEach(SseEmitter::complete);
        emitters.clear();
    }
}


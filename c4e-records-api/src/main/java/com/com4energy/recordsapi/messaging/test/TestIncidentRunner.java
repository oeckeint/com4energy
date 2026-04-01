package com.com4energy.recordsapi.messaging.test;

import com.com4energy.event.publisher.common.Environment;
import com.com4energy.event.publisher.incident.contract.IncidentCategory;
import com.com4energy.event.publisher.incident.contract.IncidentEvent;
import com.com4energy.event.publisher.incident.contract.IncidentSeverity;
import com.com4energy.event.publisher.incident.contract.IncidentStatus;
import com.com4energy.event.publisher.incident.contract.IncidentType;
import com.com4energy.event.publisher.core.Publisher;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TestIncidentRunner implements CommandLineRunner {

    @Qualifier("incidentPublisher")
    private final Publisher incidentPublisher;

    @Override
    public void run(String... args) {
        IncidentEvent testEvent = new IncidentEvent(
                UUID.randomUUID().toString(),   // id
                "c4e-records-api",             // serviceName
                Environment.DEV,               // environment
                "/api/v1/incidents",            // endpoint
                "createIncident",              // methodName
                "POST",                        // httpMethod
                "tr-123",                     // traceId
                "sp-456",                     // spanId
                "usr-99",                     // userId
                "NullPointerException",        // exceptionType
                "Incidente de prueba",         // message
                "java.lang.NullPointerException...", // stackTrace
                IncidentCategory.SYSTEM,        // category
                IncidentSeverity.ERROR,         // severity
                IncidentStatus.NEW,             // status
                "E-500",                      // errorCode
                "input.csv",                  // filename
                "CSV",                        // fileType
                "incoming",                   // folderName
                "{\"seed\":true,\"fileSize\":\"2MB\"}", // metadata complementaria
                "runner",                     // createdBy
                LocalDateTime.now(),           // updatedOn
                "runner",                     // updatedBy
                null                           // timestamp (se genera automaticamente)
        );

        try {
            incidentPublisher.send(IncidentType.SYSTEM, testEvent);
            log.info("Evento de prueba enviado");
        } catch (RuntimeException ex) {
            // No bloquea el startup si RabbitMQ aun no esta disponible.
            log.warn("No se pudo enviar el evento de prueba al iniciar: {}", ex.getMessage());
        }
    }
}

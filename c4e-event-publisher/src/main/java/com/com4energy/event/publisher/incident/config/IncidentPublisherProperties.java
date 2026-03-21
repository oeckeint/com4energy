package com.com4energy.event.publisher.incident.config;

import com.com4energy.event.publisher.incident.contract.IncidentType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Propiedades del publisher de incidentes para microservicios emisores. */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "c4e.incidents")
public class IncidentPublisherProperties {

    private boolean enabled = true;
    private Map<String, PublishTarget> types = new HashMap<>();

    public Optional<PublishTarget> forType(@NonNull IncidentType type) {
        return Optional.ofNullable(types.get(type.key()));
    }

    public record PublishTarget(
            @NotBlank String exchange,
            @NotBlank String routingKey,
            String queue,
            String deadLetterExchange,
            String deadLetterQueue
    ) {}
}


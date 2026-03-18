package com.com4energy.recordsapi.messaging.incident;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import jakarta.validation.constraints.NotBlank;
import com.com4energy.incidents.shared.contract.IncidentType;

import java.util.Map;
import java.util.Optional;

@Getter
@Setter
@ConfigurationProperties(prefix = "rabbitmq.incidents")
public class IncidentRabbitProperties {

    private Map<String, IncidentConfig> types;

    public Optional<IncidentConfig> getConfig(@NonNull IncidentType type) {
        return Optional.ofNullable(types.get(type.key()));
    }

    @Getter @Setter
    public static class IncidentConfig {
        @NotBlank private String queue;
        @NotBlank private String exchange;
        @NotBlank private String routingKey;
        @NotBlank private String deadLetterExchange;
        @NotBlank private String deadLetterQueue;
    }

}

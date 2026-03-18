package com.com4energy.recordsapi.messaging.incident;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import com.com4energy.incidents.shared.contract.IncidentType;
import com.com4energy.incidents.shared.contract.IncidentEvent;
import com.com4energy.recordsapi.exception.BusinessException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IncidentProducer {

    private final RabbitTemplate rabbitTemplate;
    private final IncidentRabbitProperties props;

    public void send(@NonNull IncidentType type, @NonNull IncidentEvent event) {
        IncidentRabbitProperties.IncidentConfig config = this.props.getConfig(type)
                .orElseThrow(() -> new BusinessException(Messages.format(MessageKey.INCIDENT_TYPE_NOT_CONFIGURED, type.key())));

        this.rabbitTemplate.convertAndSend(config.getExchange(), config.getRoutingKey(), event);
    }

}

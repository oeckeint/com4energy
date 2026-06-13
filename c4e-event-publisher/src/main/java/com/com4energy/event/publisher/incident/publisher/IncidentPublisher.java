package com.com4energy.event.publisher.incident.publisher;

import com.com4energy.event.publisher.core.Publisher;
import com.com4energy.event.publisher.exception.PublisherException;
import com.com4energy.event.publisher.incident.config.IncidentPublisherProperties;
import com.com4energy.event.publisher.incident.contract.IncidentEvent;
import com.com4energy.event.publisher.incident.contract.IncidentType;
import com.com4energy.i18n.core.Messages;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public class IncidentPublisher implements Publisher {

    private final RabbitTemplate rabbitTemplate;
    private final IncidentPublisherProperties properties;

    public IncidentPublisher(RabbitTemplate rabbitTemplate, IncidentPublisherProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void send(IncidentType type, IncidentEvent event) {
        IncidentPublisherProperties.PublishTarget target = properties.forType(type)
                .orElseThrow(() -> new PublisherException(
                        Messages.format(IncidentPublisherMessageKey.INCIDENT_TYPE_NOT_CONFIGURED, type.key())
                ));

        rabbitTemplate.convertAndSend(target.exchange(), target.routingKey(), event);
    }
}

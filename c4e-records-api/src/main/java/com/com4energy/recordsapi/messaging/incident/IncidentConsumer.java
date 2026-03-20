package com.com4energy.recordsapi.messaging.incident;

import com.com4energy.recordsapi.common.MessageKey;
import com.com4energy.recordsapi.common.Messages;
import com.com4energy.recordsapi.domain.entity.messaging.Incident;
import com.com4energy.recordsapi.mapper.IncidentEventMapper;
import com.com4energy.recordsapi.repository.IncidentRepository;
import com.com4energy.event.publisher.incident.config.IncidentPublisherProperties;
import com.com4energy.event.publisher.incident.contract.IncidentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListenerConfigurer;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistrar;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

/**
 * Registra dinamicamente un listener por cada tipo de incidente definido en
 * {@code rabbitmq.incidents.types} del {@code application.yml}.
 *
 * <p>Al agregar un nuevo tipo en configuracion, este consumer lo recoge
 * automaticamente sin necesidad de modificar codigo.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncidentConsumer implements RabbitListenerConfigurer {

    private static final String LISTENER_ID_PREFIX = "incident-listener-";

    private final IncidentPublisherProperties props;
    private final IncidentRepository incidentRepository;
    private final MessageConverter messageConverter;
    private final IncidentEventMapper mapper;

    @Override
    public void configureRabbitListeners(RabbitListenerEndpointRegistrar register) {
        props.getTypes().forEach((typeKey, config) -> {
            SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
            endpoint.setId(LISTENER_ID_PREFIX + typeKey);
            endpoint.setQueueNames(config.queue());
            endpoint.setMessageListener(message -> {
                IncidentEvent event = (IncidentEvent) messageConverter.fromMessage(message);
                Incident incident = mapper.toIncident(event);
                incidentRepository.save(incident);
                log.info(Messages.format(MessageKey.INCIDENT_SAVED, typeKey, incident.getId()));
            });

            register.registerEndpoint(endpoint);
        });
    }
}

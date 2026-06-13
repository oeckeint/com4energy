package com.com4energy.event.publisher.incident.config;

import com.com4energy.event.publisher.core.Publisher;
import com.com4energy.event.publisher.incident.publisher.IncidentPublisher;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** Auto-configuracion para exponer IncidentPublisher en microservicios Spring Boot. */
@AutoConfiguration
@EnableConfigurationProperties(IncidentPublisherProperties.class)
@ConditionalOnProperty(prefix = "c4e.incidents", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IncidentPublisherAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public Publisher incidentPublisher(RabbitTemplate rabbitTemplate,
                                       IncidentPublisherProperties properties) {
        return new IncidentPublisher(rabbitTemplate, properties);
    }
}

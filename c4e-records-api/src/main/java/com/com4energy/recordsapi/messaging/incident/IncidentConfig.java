package com.com4energy.recordsapi.messaging.incident;

import static com.com4energy.recordsapi.common.Constants.RABBITMQ_DEAD_LETTER_EXCHANGE;
import static com.com4energy.recordsapi.common.Constants.RABBITMQ_DEAD_LETTER_ROUTING_KEY;

import com.com4energy.event.publisher.incident.config.IncidentPublisherProperties;
import com.com4energy.event.publisher.incident.contract.IncidentEvent;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(IncidentPublisherProperties.class)
public class IncidentConfig {

    private final IncidentPublisherProperties props;

    public IncidentConfig(IncidentPublisherProperties props) {
        this.props = props;
    }

    /**
     * Declara dinámicamente todas las colas, exchanges y bindings
     * basados en los tipos de incidentes declarados en application.yml.
     * Spring AMQP registra automáticamente el contenido en Declarables.
     */
    @Bean
    public Declarables incidentDeclarables() {
        List<Declarable> declarables = new ArrayList<>();

        props.getTypes().forEach((type, config) -> {
            // Args para enrutar mensajes fallidos al DLX
            Map<String, Object> args = new HashMap<>();
            args.put(RABBITMQ_DEAD_LETTER_EXCHANGE, config.deadLetterExchange());
            args.put(RABBITMQ_DEAD_LETTER_ROUTING_KEY, config.deadLetterQueue());

            // Cola principal y binding
            Queue mainQueue = new Queue(config.queue(), true, false, false, args);
            TopicExchange mainExchange = new TopicExchange(config.exchange());
            Binding mainBinding = BindingBuilder.bind(mainQueue).to(mainExchange).with(config.routingKey());

            // Dead Letter Queue y binding
            Queue dlq = new Queue(config.deadLetterQueue(), true);
            TopicExchange dlx = new TopicExchange(config.deadLetterExchange());
            Binding dlqBinding = BindingBuilder.bind(dlq).to(dlx).with(config.routingKey());

            declarables.add(mainQueue);
            declarables.add(mainExchange);
            declarables.add(mainBinding);
            declarables.add(dlq);
            declarables.add(dlx);
            declarables.add(dlqBinding);
        });

        return new Declarables(declarables);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        Map<String, Class<?>> idMapping = new HashMap<>();
        idMapping.put("com.com4energy.event.publisher.incident.contract.IncidentEvent", IncidentEvent.class);
        idMapping.put("common.publisher.incident.contract.IncidentEvent", IncidentEvent.class);
        typeMapper.setIdClassMapping(idMapping);
        typeMapper.setTrustedPackages(
                "com.com4energy.event.publisher.incident.contract",
                "common.publisher.incident.contract",
                "java.util"
        );

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

}

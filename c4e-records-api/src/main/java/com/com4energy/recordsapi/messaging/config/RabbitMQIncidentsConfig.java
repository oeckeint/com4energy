package com.com4energy.recordsapi.messaging.config;

import com.com4energy.recordsapi.messaging.incident.IncidentRabbitProperties;
import org.springframework.amqp.core.*;
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
@EnableConfigurationProperties(IncidentRabbitProperties.class)
public class RabbitMQIncidentsConfig {

    private static final String DEAD_LETTER_EXCHANGE_ARGUMENT = "x-dead-letter-exchange";
    private static final String DEAD_LETTER_QUEUE_ARGUMENT = "x-dead-letter-routing-key";

    private final IncidentRabbitProperties props;

    public RabbitMQIncidentsConfig(IncidentRabbitProperties props) {
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
            args.put(DEAD_LETTER_EXCHANGE_ARGUMENT, config.getDeadLetterExchange());
            args.put(DEAD_LETTER_QUEUE_ARGUMENT, config.getDeadLetterQueue());

            // Cola principal y binding
            Queue mainQueue = new Queue(config.getQueue(), true, false, false, args);
            TopicExchange mainExchange = new TopicExchange(config.getExchange());
            Binding mainBinding = BindingBuilder.bind(mainQueue).to(mainExchange).with(config.getRoutingKey());

            // Dead Letter Queue y binding
            Queue dlq = new Queue(config.getDeadLetterQueue(), true);
            TopicExchange dlx = new TopicExchange(config.getDeadLetterExchange());
            Binding dlqBinding = BindingBuilder.bind(dlq).to(dlx).with(config.getRoutingKey());

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
        return new Jackson2JsonMessageConverter();
    }

}
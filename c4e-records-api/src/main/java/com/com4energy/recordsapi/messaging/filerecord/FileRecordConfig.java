package com.com4energy.recordsapi.messaging.filerecord;

import static com.com4energy.recordsapi.common.Constants.RABBITMQ_DEAD_LETTER_EXCHANGE;
import static com.com4energy.recordsapi.common.Constants.RABBITMQ_DEAD_LETTER_ROUTING_KEY;

import org.springframework.amqp.core.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Declara dinámicamente todas las colas, exchanges y bindings para eventos
 * de file records publicados por el ingestion-service via outbox worker.
 *
 * <p>Cada tipo configurado en {@code c4e.file-records.types} genera:
 * <ul>
 *   <li>Cola principal con DLX configurado</li>
 *   <li>Exchange principal (TopicExchange)</li>
 *   <li>Binding principal</li>
 *   <li>Dead Letter Queue + Exchange + Binding</li>
 * </ul>
 * </p>
 */
@Configuration
@EnableConfigurationProperties(FileRecordRoutingProperties.class)
public class FileRecordConfig {

    private final FileRecordRoutingProperties props;

    public FileRecordConfig(FileRecordRoutingProperties props) {
        this.props = props;
    }

    @Bean
    public Declarables fileRecordDeclarables() {
        List<Declarable> declarables = new ArrayList<>();

        props.getTypes().forEach((typeKey, config) -> {
            // Args para enrutar mensajes fallidos al DLX
            Map<String, Object> args = new HashMap<>();
            args.put(RABBITMQ_DEAD_LETTER_EXCHANGE, config.getDeadLetterExchange());
            args.put(RABBITMQ_DEAD_LETTER_ROUTING_KEY, config.getDeadLetterQueue());

            // Cola principal y binding
            Queue mainQueue    = new Queue(config.getQueue(), true, false, false, args);
            TopicExchange mainExchange = new TopicExchange(config.getExchange());
            Binding mainBinding = BindingBuilder.bind(mainQueue).to(mainExchange).with(config.getRoutingKey());

            // Dead Letter Queue y binding
            // La routing key del DLQ debe coincidir con x-dead-letter-routing-key del main queue (= nombre del DLQ)
            Queue dlq          = new Queue(config.getDeadLetterQueue(), true);
            DirectExchange dlx = new DirectExchange(config.getDeadLetterExchange());
            Binding dlqBinding = BindingBuilder.bind(dlq).to(dlx).with(config.getDeadLetterQueue());

            declarables.add(mainQueue);
            declarables.add(mainExchange);
            declarables.add(mainBinding);
            declarables.add(dlq);
            declarables.add(dlx);
            declarables.add(dlqBinding);
        });

        return new Declarables(declarables);
    }

}

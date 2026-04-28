package com.com4energy.outboxworker.outbox.config;

import com.com4energy.outboxworker.outbox.job.OutboxCleanupJob;
import com.com4energy.outboxworker.outbox.messaging.RabbitPublisher;
import com.com4energy.outboxworker.outbox.repository.OutboxEventRepository;
import com.com4energy.outboxworker.outbox.service.OutboxPollingService;
import com.com4energy.outboxworker.outbox.service.OutboxProcessor;
import com.com4energy.outboxworker.outbox.worker.OutboxWorker;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(OutboxRoutingProperties.class)
@ConditionalOnClass({NamedParameterJdbcTemplate.class, RabbitTemplate.class})
@ConditionalOnProperty(prefix = "c4e.outbox.worker", name = "enabled", havingValue = "true")
public class OutboxWorkerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxEventRepository outboxEventRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        return new OutboxEventRepository(jdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public RabbitPublisher rabbitPublisher(
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper,
            OutboxRoutingProperties routingProperties
    ) {
        return new RabbitPublisher(rabbitTemplate, objectMapper, routingProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxProcessor outboxProcessor(OutboxEventRepository repository, RabbitPublisher rabbitPublisher) {
        return new OutboxProcessor(repository, rabbitPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxPollingService outboxPollingService(
            OutboxEventRepository repository,
            OutboxProcessor outboxProcessor,
            TransactionTemplate transactionTemplate,
            Environment environment
    ) {
        int batchSize = environment.getProperty("c4e.outbox.worker.batch-size", Integer.class, 50);
        String workerId = environment.getProperty("c4e.outbox.worker.worker-id", "");
        return new OutboxPollingService(repository, outboxProcessor, transactionTemplate, batchSize, workerId);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxWorker outboxWorker(OutboxPollingService outboxPollingService) {
        return new OutboxWorker(outboxPollingService);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "c4e.outbox.cleanup", name = "enabled", havingValue = "true")
    public OutboxCleanupJob outboxCleanupJob(OutboxEventRepository repository, Environment environment) {
        int retentionDays = environment.getProperty("c4e.outbox.cleanup.retention-days", Integer.class, 7);
        int batchSize = environment.getProperty("c4e.outbox.cleanup.batch-size", Integer.class, 500);
        return new OutboxCleanupJob(repository, retentionDays, batchSize);
    }
}






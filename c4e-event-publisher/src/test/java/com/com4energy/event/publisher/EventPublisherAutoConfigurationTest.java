package com.com4energy.event.publisher;

import com.com4energy.event.publisher.incident.config.IncidentPublisherAutoConfiguration;
import com.com4energy.event.publisher.core.Publisher;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IncidentPublisherAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(IncidentPublisherAutoConfiguration.class))
            .withBean(RabbitTemplate.class, () -> mock(RabbitTemplate.class))
            .withPropertyValues(
                    "c4e.incidents.types.validation.exchange=incident.validation.exchange",
                    "c4e.incidents.types.validation.routing-key=incident.validation.key"
            );

    @Test
    void shouldCreatePublisherBean() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(Publisher.class));
    }
}


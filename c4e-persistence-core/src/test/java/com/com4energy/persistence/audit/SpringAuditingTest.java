package com.com4energy.persistence.audit;

import com.com4energy.consumerprobe.AuditProbeEntity;
import com.com4energy.consumerprobe.AuditProbeRepository;
import com.com4energy.consumerprobe.ConsumerProbeApplication;
import com.com4energy.persistence.autoconfigure.PersistenceCoreAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica que la auditoría automática de Spring rellena created_at/created_by
 * (y updated_*) al persistir una entidad que extiende {@link Auditable},
 * siempre que el consumidor active @EnableJpaAuditing + un AuditorAware.
 */
class SpringAuditingTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PersistenceCoreAutoConfiguration.class,
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    JpaRepositoriesAutoConfiguration.class
            ))
            .withUserConfiguration(ConsumerProbeApplication.class, AuditingTestConfig.class)
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:auditdb;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
            );

    @Test
    void populatesAuditFieldsOnPersist() {
        contextRunner.run(ctx -> {
            AuditProbeRepository repository = ctx.getBean(AuditProbeRepository.class);

            AuditProbeEntity saved = repository.saveAndFlush(new AuditProbeEntity());

            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getCreatedBy()).isEqualTo("TEST");
            assertThat(saved.getUpdatedAt()).isNotNull();
            assertThat(saved.getUpdatedBy()).isEqualTo("TEST");
        });
    }

    @Configuration
    @EnableJpaAuditing
    static class AuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("TEST");
        }
    }
}

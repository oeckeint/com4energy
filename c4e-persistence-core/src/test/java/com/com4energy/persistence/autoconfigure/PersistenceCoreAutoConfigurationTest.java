package com.com4energy.persistence.autoconfigure;

import com.com4energy.consumerprobe.ConsumerProbeApplication;
import com.com4energy.consumerprobe.ConsumerProbeRepository;
import com.com4energy.persistence.filerecord.FileRecord;
import com.com4energy.persistence.medidas.medidacch.BaseMedidaCCHRepository;
import com.com4energy.persistence.medidas.medidacch.MedidaCCH;
import com.com4energy.persistence.medidas.medidah.BaseMedidaHRepository;
import com.com4energy.persistence.medidas.medidah.MedidaH;
import com.com4energy.persistence.medidas.medidaqh.BaseMedidaQHRepository;
import com.com4energy.persistence.medidas.medidaqh.MedidaQH;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceCoreAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    PersistenceCoreAutoConfiguration.class,
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    JpaRepositoriesAutoConfiguration.class
            ))
            .withPropertyValues(
                    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
                    "spring.datasource.driver-class-name=org.h2.Driver",
                    "spring.jpa.hibernate.ddl-auto=create-drop",
                    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
            );

    @Test
    void contextStartsWithoutErrors() {
        contextRunner.run(ctx -> assertThat(ctx).hasNotFailed());
    }

    /**
     * El kernel registra automáticamente sus entidades JPA bajo
     * com.com4energy.persistence en cualquier consumidor que solo añada la dependencia.
     */
    @Test
    void kernelEntitiesAreAutoRegistered() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(managedEntityTypes(ctx)).contains(
                    FileRecord.class, MedidaH.class, MedidaQH.class, MedidaCCH.class);
        });
    }

    /**
     * Los repositorios base del kernel son @NoRepositoryBean: existen solo para
     * ser extendidos por el repo concreto de cada servicio. NO deben instanciarse
     * como beans (de lo contrario aparecerían beans fantasma en cada consumidor).
     */
    @Test
    void baseRepositoriesAreNotRegisteredAsBeans() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasNotFailed();
            assertThat(ctx).doesNotHaveBean(BaseMedidaHRepository.class);
            assertThat(ctx).doesNotHaveBean(BaseMedidaQHRepository.class);
            assertThat(ctx).doesNotHaveBean(BaseMedidaCCHRepository.class);
        });
    }

    /**
     * Regresión: la auto-configuración debe ser ADITIVA. El repositorio concreto
     * del servicio consumidor (fuera de com.com4energy.persistence) debe seguir
     * detectándose, junto con las entidades del kernel. Si se usara
     * {@code @EnableJpaRepositories} con basePackages fijo, este test fallaría
     * porque el escaneo de repositorios del consumidor quedaría desactivado.
     */
    @Test
    void consumerRepositoriesCoexistWithKernel() {
        contextRunner
                .withUserConfiguration(ConsumerProbeApplication.class)
                .run(ctx -> {
                    assertThat(ctx).hasNotFailed();
                    // Repo concreto del consumidor (fuera de com.com4energy.persistence)
                    assertThat(ctx).hasSingleBean(ConsumerProbeRepository.class);
                    // Entidades del kernel correctamente registradas junto a las del consumidor
                    assertThat(managedEntityTypes(ctx)).contains(
                            FileRecord.class, MedidaH.class, MedidaQH.class, MedidaCCH.class);
                });
    }

    private static java.util.Set<Class<?>> managedEntityTypes(
            org.springframework.context.ApplicationContext ctx) {
        return ctx.getBean(EntityManagerFactory.class).getMetamodel().getEntities().stream()
                .map(jakarta.persistence.metamodel.ManagedType::getJavaType)
                .collect(java.util.stream.Collectors.toSet());
    }
}

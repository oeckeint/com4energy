package com.com4energy.persistence.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * Auto-configuración para c4e-domain-model.
 *
 * Registra automáticamente:
 * - Entidades JPA bajo com.com4energy.persistence
 * - Repositorios Spring Data bajo com.com4energy.persistence
 *
 * Los servicios consumidores solo necesitan agregar la dependencia Maven:
 *
 * <dependency>
 *   <groupId>com.com4energy</groupId>
 *   <artifactId>c4e-domain-model</artifactId>
 * </dependency>
 *
 * Sin necesidad de @EntityScan ni @EnableJpaRepositories manualmente.
 *
 * Importante: se usa exclusivamente @AutoConfigurationPackage porque es aditivo.
 * Añade "com.com4energy.persistence" a los paquetes que Spring Boot ya escanea
 * (incluido el del consumidor), de modo que tanto las entidades como los
 * repositorios de ambos paquetes quedan registrados.
 *
 * NO usar @EnableJpaRepositories aquí: al ser una anotación explícita, desactiva
 * el JpaRepositoriesAutoConfiguration de Spring Boot y sustituye su escaneo por
 * un basePackages fijo, lo que dejaría sin detectar los repositorios del servicio
 * consumidor (p. ej. com.com4energy.processor.repository).
 */
@AutoConfiguration(before = HibernateJpaAutoConfiguration.class)
@AutoConfigurationPackage(basePackages = "com.com4energy.persistence")
public class PersistenceCoreAutoConfiguration {
}

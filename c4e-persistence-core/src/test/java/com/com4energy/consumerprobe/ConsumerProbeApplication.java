package com.com4energy.consumerprobe;

import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.context.annotation.Configuration;

/**
 * Simula la clase @SpringBootApplication de un servicio consumidor: registra su
 * propio paquete (com.com4energy.consumerprobe) en AutoConfigurationPackages,
 * tal como lo haría @SpringBootApplication para com.com4energy.processor.
 */
@Configuration
@AutoConfigurationPackage
public class ConsumerProbeApplication {
}

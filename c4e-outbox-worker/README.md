# c4e-outbox-worker

Microservicio Spring Boot (jar ejecutable con Tomcat embebido) que hace polling de la tabla `outbox_event` y publica a RabbitMQ.

## Que hace

- Reclama eventos `PENDING` en batches (`FOR UPDATE SKIP LOCKED`).
- Marca como `PROCESSING` por `workerId`.
- Publica a Rabbit por `eventType`.
- Marca `PROCESSED` o `FAILED`.
- Limpia eventos `PROCESSED` antiguos en batches.

## Configuracion minima

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/com4energy
    username: user
    password: pass

c4e:
  outbox:
    worker:
      enabled: true
      poll-interval-ms: 5000
      batch-size: 50
    cleanup:
      enabled: true
      retention-days: 7
      batch-size: 500
      cron: "0 0 * * * *"
    routing:
      types:
        FILE_REJECTED:
          exchange: c4e.exchange
          routing-key: c4e.file.rejected
        FILE_VALIDATION_INCIDENT:
          exchange: incident.validation.exchange
          routing-key: incident.validation.key
```

## Ejecutar

```bash
cd c4e-outbox-worker
mvn spring-boot:run
```

## Build

```bash
cd c4e-outbox-worker
mvn clean package
```


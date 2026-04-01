# ADR-0003: Records API como consumer de incidentes con listeners dinamicos

- Estado: Aceptado
- Fecha: 2026-03-20

## Contexto

El objetivo actual es centralizar el consumo de eventos de incidentes en `records-api`, mientras otros microservicios solo publican.

## Decision

Mantener `records-api` como consumer de incidentes y registrar listeners de RabbitMQ de forma dinamica segun `c4e.incidents.types.*`.

## Consecuencias

- Agregar nuevos tipos de incidentes no requiere cambiar codigo del consumer.
- Se consolida persistencia y trazabilidad de incidentes en un unico servicio.
- Si en el futuro se distribuye el consumo, debera revisarse ownership de colas y escalado por dominio.

## Referencias

- `src/main/java/com/com4energy/recordsapi/messaging/incident/IncidentConsumer.java`
- `src/main/java/com/com4energy/recordsapi/messaging/incident/IncidentConfig.java`
- `src/main/resources/application.yml`


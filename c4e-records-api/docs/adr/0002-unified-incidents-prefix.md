# ADR-0002: Prefijo unico c4e.incidents para publisher y consumer

- Estado: Aceptado
- Fecha: 2026-03-20

## Contexto

Existian configuraciones separadas para publisher y consumer, con riesgo de inconsistencia y duplicidad de clases de propiedades.

## Decision

Unificar la configuracion de incidentes bajo el prefijo `c4e.incidents`.

## Consecuencias

- Menos configuracion duplicada y menos errores por desalineacion.
- El publisher usa `exchange` y `routingKey`; el consumer reutiliza tambien `queue`, `deadLetterExchange` y `deadLetterQueue`.
- Se simplifica onboarding y soporte operativo.

## Referencias

- `src/main/resources/application.yml`
- `src/main/java/com/com4energy/recordsapi/messaging/incident/IncidentConfig.java`
- `c4e-event-publisher/src/main/java/com/com4energy/event/publisher/incident/config/IncidentPublisherProperties.java`


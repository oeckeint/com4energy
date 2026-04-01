# ADR-0001: Contrato base de publicacion con Publisher.send
- Estado: Aceptado
- Fecha: 2026-03-20
## Contexto
Se necesita un punto de extension comun para futuros publishers (incidents, payments, users) sin acoplar consumidores a implementaciones concretas.
## Decision
Usar la interfaz `Publisher` como contrato base y estandarizar el metodo `send(...)`.
## Consecuencias
- Los servicios dependen del contrato (`Publisher`) y no de la clase concreta.
- Se habilita agregar nuevas implementaciones sin romper consumidores existentes.
- Se requiere usar `@Qualifier` cuando haya multiples beans del tipo `Publisher`.
## Referencias
- `c4e-event-publisher/src/main/java/com/com4energy/event/publisher/core/Publisher.java`
- `c4e-event-publisher/src/main/java/com/com4energy/event/publisher/incident/publisher/IncidentPublisher.java`

# ADRs - Records API

Este directorio contiene los **Architecture Decision Records (ADR)** del proyecto.

## Convencion

- Un archivo por decision.
- Nombre: `000X-descripcion-corta.md`.
- Los ADRs son inmutables: si una decision cambia, se crea uno nuevo y se referencia al anterior.

## Estado sugerido

- `Propuesto`
- `Aceptado`
- `Deprecado`
- `Reemplazado por ADR-XXXX`

## Indice

- [0000 - Alcance y exclusiones de ADRs](./0000-scope-and-exclusions.md)
- [0001 - Contrato base de publicacion con Publisher.send](./0001-base-publisher-contract-send.md)
- [0002 - Prefijo unico c4e.incidents para publisher y consumer](./0002-unified-incidents-prefix.md)
- [0003 - Records API como consumer de incidentes con listeners dinamicos](./0003-records-api-single-consumer-dynamic-listeners.md)
- [0004 - Uso de @Qualifier con @RequiredArgsConstructor via lombok.config](./0004-qualifier-with-requiredargsconstructor.md)
- [0005 - Prohibir temporalmente Lombok @Data](./0005-prohibit-lombok-data-temporarily.md)

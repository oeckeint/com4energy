# ADR-0005: Prohibir temporalmente Lombok @Data

- Estado: Aceptado
- Fecha: 2026-03-21

## Contexto

El proyecto habilito la regla `lombok.data.flagUsage = error` para evitar nuevos usos de `@Data`.

Aunque `@Data` reduce boilerplate, en esta etapa el costo de ambiguedad supera el beneficio:

- Mezcla varias decisiones en una sola anotacion (`@Getter`, `@Setter`, `@EqualsAndHashCode`, `@ToString`, `@RequiredArgsConstructor`).
- En entidades/modelos puede introducir `equals/hashCode` y `toString` no deseados para JPA o para logs.
- Hace menos explicita la intencion del modelo (mutabilidad, identidad, campos sensibles).

## Decision

Mantener `@Data` prohibido por defecto en `c4e-records-api` y usar anotaciones explicitas segun necesidad (`@Getter`, `@Setter`, `@Builder`, etc.).

## Consecuencias

- Se gana control fino sobre identidad, mutabilidad y salida en logs.
- El costo es un poco mas de codigo explicito en clases de dominio/DTO.
- La regla se aplica automaticamente en IDE y compilacion via `lombok.config`.

## Indicadores para reevaluar y considerar habilitarlo

Se puede reabrir esta decision si se cumplen varios indicadores de forma estable:

1. Convenciones claras por tipo de clase (entidad, DTO, command/event) y reglas publicadas.
2. Calidad automatizada suficiente (tests de dominio y validaciones de mapeo) para detectar efectos colaterales de `equals/hashCode`.
3. Reglas de analisis estatico que limiten riesgos (por ejemplo, prohibiciones por paquete o checklist de PR).
4. Casos reales donde el beneficio en productividad sea medible y repetido, no puntual.

## Referencias

- `lombok.config`
- `docs/adr/README.md`


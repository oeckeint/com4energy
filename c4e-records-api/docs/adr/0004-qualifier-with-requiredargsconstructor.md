# ADR-0004: Uso de @Qualifier con @RequiredArgsConstructor via lombok.config

- Estado: Aceptado
- Fecha: 2026-03-20

## Contexto

Con multiples beans de tipo `Publisher`, la inyeccion por tipo produce ambiguedad. Se usa Lombok con `@RequiredArgsConstructor` para inyeccion por constructor.

## Decision

Mantener `@Qualifier("incidentPublisher")` en el campo y configurar Lombok para copiar anotaciones al constructor generado.

## Consecuencias

- Se evita crear constructores manuales solo para propagar `@Qualifier`.
- Se mantiene codigo conciso y consistente con estilo Lombok del proyecto.
- Si se elimina Lombok o `lombok.config`, este punto debe revisarse.

## Referencias

- `src/main/java/com/com4energy/recordsapi/messaging/test/TestIncidentRunner.java`
- `lombok.config`


# ADR-0000: Alcance y exclusiones de ADRs
- Estado: Aceptado
- Fecha: 2026-03-20
## Contexto
El proyecto necesita registrar decisiones tecnicas relevantes para facilitar mantenimiento, onboarding y futuras refactorizaciones.
## Decision
Adoptar ADRs en `docs/adr` para registrar decisiones de arquitectura y diseno tecnico que impactan comportamiento, integracion o evolucion del sistema.
## Exclusiones
Para evitar ruido historico, **no se documenta aqui la decision de renombre a `c4e-event-publisher`**.
## Consecuencias
- Mejora trazabilidad de decisiones.
- Las discusiones de naming historico quedan fuera de este set inicial.
- Nuevas decisiones deben referenciar ADRs previos cuando corresponda.

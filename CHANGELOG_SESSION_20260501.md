# Plan de Implementación - Revisiones `.0` a `.10` y Versionado por Registro

## Alcance
Este documento aplica **exclusivamente** a `c4e-ingestion-service`.

### Fuera de alcance
- No se conservará historial completo de versiones por registro.
- El modelo guardará únicamente estado actual + trazabilidad al archivo origen.

## Resumen ejecutivo
- El servicio ya deduplica por **archivo completo** usando `file_records.hash` (único).
- El servicio aún no aplica control de obsolescencia por revisión superior procesada.
- La persistencia de medidas es batch con `saveAll` (sin upsert por clave de negocio).
- El soporte de revisiones requiere ajuste para manejar `.10` de forma consistente.
- Se propone una evolución en 3 fases: control por archivo, preparación del modelo, upsert eficiente.
- Los hallazgos recientes de producción confirman deuda histórica masiva de duplicidad en `medida_h`, `medidaqh` y `medida_cch`.
- La hipótesis inicial de "problema principalmente por replay técnico" queda invalidada para `medidaqh`: hay un volumen dominante de `CONFLICTING_BUSINESS_PAYLOAD`.
- La implementación de Fase 1 se mantiene válida y recomendable de forma inmediata, independientemente de la estrategia elegida para deuda histórica.
- La activación de constraints únicos, deduplicación definitiva y estrategia final de upsert queda condicionada a decisión explícita de negocio.
- Se incorpora `Alternativa C - Historical Freeze + Forward Clean` para proteger el flujo nuevo sin bloquear la definición final sobre deuda histórica.

## Estado actual (confirmado)

### Lo que ya existe
- `file_records` con `hash` único:
  - `src/main/resources/db/changelog/automatic/001_CIS-004_create_file_records_table.sql`
- Persistencia batch de medidas con binary split:
  - `src/main/java/com/com4energy/processor/service/measure/persistence/JpaMeasurePersistenceAdapter.java`
- Entidades actuales de destino:
  - `src/main/java/com/com4energy/processor/model/measure/MedidaHEntity.java`
  - `src/main/java/com/com4energy/processor/model/measure/MedidaQHEntity.java`
  - `src/main/java/com/com4energy/processor/model/measure/MedidaCCHEntity.java`
- Lock por familia en procesamiento:
  - `src/main/java/com/com4energy/processor/service/FileRecordService.java`

### Gaps actuales
- No hay regla funcional de obsolescencia por revisión superior `SUCCEEDED`.
- No hay upsert por clave de negocio en `medida_h`, `medidaqh`, `medida_cch`.
- El manejo de extensión de medidas está acoplado a 1 dígito en puntos críticos.

---

## Fase 1 - Control de revisión por archivo

### Objetivo
Aceptar revisiones más nuevas y rechazar revisiones obsoletas cuando ya existe una superior `SUCCEEDED` para la misma familia.

### Cambios funcionales
- Extraer de `original_filename`:
  - `source_family_key`
  - `source_revision` (entero)
- Regla:
  - si `incoming_revision < max_succeeded_revision(family)` -> rechazar por obsolescencia
  - si `incoming_revision > max_succeeded_revision(family)` -> permitir
  - si `incoming_revision == max_succeeded_revision(family)` -> no reprocesar como revisión nueva; tratar como replay/duplicado/conflicto a nivel archivo según `file_records.hash`
  - comparar siempre como entero, nunca de forma lexicográfica (`.10` debe ser mayor que `.2`)
- El rechazo debe conservar auditoría:
  - registrar el `file_record`
  - persistir `source_family_key` y `source_revision`
  - mover el archivo a `rejected`
  - dejar `status = REJECTED` con razón/comentario de obsolescencia
- La Fase 1 solo decide replay/conflicto de archivo completo. La Fase 3 revalida la semántica definitiva por registro usando `payload_hash`.
- Política por defecto en Fase 1 para `incoming_revision == max_succeeded_revision(family)`:
  - mismo `file_records.hash` -> `DUPLICATED_CONTENT` (idempotente)
  - hash distinto -> `REJECTED` con `FailureReason.REVISION_CONFLICT`
  - opcional por feature flag: derivar conflictos de misma revisión a cuarentena para análisis posterior.
- Política para parseo fallido de revisión/familia:
  - si no se puede extraer `source_revision` o `source_family_key`, ambos quedan `NULL`
  - esos archivos no participan en el gate de obsolescencia y continúan por flujo legacy
  - se incrementa métrica `revision_parse_error_count` para trazabilidad operativa.

### Cambios técnicos
- Ajustar validaciones/clasificación para soportar `.10` de forma explícita.
- Añadir consulta de `max_succeeded_revision` por familia en `FileRecordRepository`.
- Evaluar el gate de obsolescencia y persistir `file_record` únicamente después de la decisión final, dentro de la misma transacción.
- Aplicar un segundo gate (re-check) al inicio de procesamiento de medidas para evitar carreras tardías:
  - si el archivo quedó obsoleto entre registro y procesamiento, mover a `rejected` y no persistir medidas.
- Añadir razón explícita para obsolescencia, idealmente `FailureReason.OBSOLETE_REVISION`.
- Añadir razón explícita para conflicto de misma revisión, idealmente `FailureReason.REVISION_CONFLICT`.
- Definir un comentario estándar, por ejemplo:
  - `Obsolete revision 1 because revision 3 already succeeded for family F5D_0031_0894_20250311`
- Centralizar el parseo de familia/revisión en una utilidad de dominio para evitar duplicar lógica.
- Normalizar `source_family_key` (trim + case estable) antes de lock/consulta para evitar familias duplicadas por formato.

### Estado transaccional exacto (Fase 1)
Para archivos con revisión numérica, el gate de obsolescencia debe ejecutarse en una transacción única:

`BEGIN TX`
1. Resolver/parsing de `source_family_key` y `source_revision`.
2. Insertar o asegurar fila en `file_revision_family_locks` (idempotente).
3. `SELECT ... FOR UPDATE` por `source_family_key`.
4. Calcular `max_succeeded_revision(family)`.
5. Decidir estado (`PENDING` o `REJECTED`).
6. Persistir `file_record` una única vez con `source_family_key`, `source_revision`, estado, reason y comentario finales.
`COMMIT`

Regla de implementación: no se permite `insert file_record + commit` antes de evaluar el gate.

### Concurrencia por familia
La decisión de obsolescencia debe ejecutarse con bloqueo transaccional por `source_family_key` y re-validación inmediata de `max_succeeded_revision` antes de persistir el estado final.

Estrategia preferida:
- Crear una tabla de lock lógico por familia, por ejemplo:
  - `file_revision_family_locks(source_family_key PRIMARY KEY, created_at, updated_at)`
- Al registrar una revisión numérica:
  1. resolver `source_family_key` y `source_revision`
  2. crear/asegurar fila ancla para la familia con operación idempotente
  3. tomar `SELECT ... FOR UPDATE` sobre la fila de esa familia
  4. recalcular `max_succeeded_revision`
  5. decidir `REJECTED` vs `PENDING`
  6. confirmar transacción
- Mantener la verificación existente de "family being processed" como protección adicional durante procesamiento, no como única garantía de consistencia.
- Manejar deadlocks/lock wait timeouts con retry acotado y backoff.
- Política operativa recomendada para lock contention:
  - retries máximos: `3`
  - backoff: exponencial con jitter (`100ms`, `250ms`, `500ms`)
  - timeout total de adquisición por operación: `<= 2s`
  - si no se adquiere lock: registrar incidente y dejar el archivo en estado recuperable según política (`RETRY` o `REJECTED` controlado).
- La tabla de locks puede ser permanente. Si el volumen de familias crece demasiado, agregar housekeeping periódico para familias sin actividad reciente.

Justificación:
- Evita ventanas TOCTOU entre "consultar max revision" y "persistir decisión".
- Permite escalar horizontalmente sin que dos nodos tomen decisiones inconsistentes para la misma familia.

### Migración BD
- Alter de `file_records`:
  - `source_family_key VARCHAR(191) NULL`
  - `source_revision INT NULL`
  - índice `(source_family_key, status, source_revision)`
- Crear tabla de lock lógico por familia:
  - `file_revision_family_locks` con PK explícita:

```sql
CREATE TABLE file_revision_family_locks (
  source_family_key VARCHAR(191) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  PRIMARY KEY (source_family_key)
);
```

- Backfill inicial desde `original_filename`.
- Para archivos no canónicos (`xml`, `UNKNOWN`, nombres inválidos, extensiones no numéricas):
  - `source_family_key = NULL`
  - `source_revision = NULL`

### Validación de índice para consulta caliente
Consulta crítica esperada:

```sql
SELECT MAX(source_revision)
FROM file_records
WHERE source_family_key = ?
  AND status = 'SUCCEEDED';
```

Acción obligatoria de diseño:
- validar con `EXPLAIN` sobre dataset real el índice `(source_family_key, status, source_revision)`;
- ajustar definición/orden del índice solo con evidencia de plan de ejecución.

### Failure Recovery (Fase 1)
- Si el nodo cae dentro de la transacción del gate, la transacción hace rollback.
- Los locks transaccionales (`SELECT ... FOR UPDATE`) se liberan automáticamente al rollback/cierre de sesión.
- El reproceso posterior es seguro e idempotente (mismas reglas de revisión y hash).
- Si ocurre timeout/deadlock y se agotan retries, el archivo debe quedar en estado recuperable según política (`RETRY` o `REJECTED` controlado).

### Riesgos
- Condiciones de carrera con múltiples revisiones concurrentes de la misma familia.
- Parseo ambiguo si hay nombres no canónicos.
- Falsos positivos si se usa orden alfabético en lugar de comparación numérica.

### Estrategia de tests
- Unit tests de parseo (`.0` a `.10`, inválidos).
- Unit tests de comparación numérica de revisiones.
- Integración:
  - existe `.3 SUCCEEDED`, llega `.1` -> rechazo
  - existe `.3`, llega `.4` -> permitido
  - existe `.3 SUCCEEDED`, llega otro `.3` con mismo hash de archivo -> duplicado/idempotente
  - existe `.3 SUCCEEDED`, llega otro `.3` con hash de archivo distinto -> conflicto de misma revisión
  - existe `.2 SUCCEEDED`, llega `.10` -> permitido
  - existe `.10 SUCCEEDED`, llega `.2` -> rechazo
  - archivo obsoleto queda registrado y auditable en `file_records`
  - archivo aceptado en registro, pero obsoleto al iniciar procesamiento -> rechazo en re-check sin persistencia de medidas
  - dos nodos intentan registrar revisiones de la misma familia al mismo tiempo -> decisión serializada por lock transaccional
  - caída de nodo durante gate transaccional -> rollback + reintento seguro
  - parseo inválido de revisión -> bypass de gate y flujo legacy con métrica de error

---

## Fase 2 - Preparación del modelo de versionado por registro

### Objetivo
Incorporar metadatos por registro para habilitar decisiones de corrección sin activar aún upsert masivo.

### Nuevas columnas en tablas de medidas
- `source_revision`
- `source_file_record_id`
- `payload_hash`
- `payload_hash_version` (default `1`)

### Clave de negocio inicial (provisional)
- `medida_h`: (`id_cliente`, `fecha`, `tipo_medida`)
- `medidaqh`: (`id_cliente`, `fecha`, `tipomed`)
- `medida_cch`: (`id_cliente`, `fecha`)

Estas claves se validarán con análisis de colisiones en datos legacy antes de declarar constraints definitivos.

### Semántica de campos
- `file_records.hash`: hash del archivo completo.
- `payload_hash`: hash del payload del registro lógico (misma clave de negocio).
- `payload_hash_version`: versión del algoritmo/canonicalización usado para calcular `payload_hash`.
- `source_revision`: revisión aplicada al estado actual del registro.
- `source_file_record_id`: trazabilidad al archivo que causó el estado actual.

### Migración BD
- Alter tables:
  - `medida_h`
  - `medidaqh`
  - `medida_cch`
- Añadir índices no únicos iniciales para soporte de lectura/merge.
- Backfill por lotes para no bloquear operación.

### Riesgos
- Hash no canónico puede generar falsos cambios.
- Backfill incompleto puede dejar datos heterogéneos temporalmente.

### Canonicalización de `payload_hash`
El hash por registro debe calcularse con una representación canónica:
- mismo orden de campos para cada tipo de medida
- fechas en formato fijo y zona/precisión definida
- valores numéricos normalizados de forma estable
- `NULL` representado con un token explícito
- excluir campos técnicos y de auditoría:
  - `id`
  - `created_on`
  - `created_by`
  - `updated_on`
  - `updated_by`
  - `source_revision`
  - `source_file_record_id`
  - `payload_hash`
- incluir únicamente los campos de negocio que determinan el contenido real de la medida
- registrar versión explícita del algoritmo (`payload_hash_version = 1` para `PayloadHashV1`)
- cambios futuros de canonicalización deben crear nueva versión (V2, V3...), no mutar retrospectivamente V1.

### Estrategia de tests
- Determinismo de `payload_hash`.
- Compatibilidad entre versiones de hash (`payload_hash_version`) cuando evolucione la canonicalización.
- Persistencia correcta de los nuevos campos de versionado por registro.
- Compatibilidad backward con el flujo actual de batch insert.

---

## Fase 3 - Upsert eficiente por registro

### Objetivo
Aplicar correcciones por revisión en volumen alto (100k+) con control de obsolescencia y trazabilidad.

### Estado actual de activación (actualizado)
- El diseño técnico de Fase 3 se conserva, pero su activación productiva queda **bloqueada** hasta resolver la decisión de remediación de deuda histórica.
- Con la evidencia actual, **no** se recomienda habilitar todavía `INSERT ... ON DUPLICATE KEY UPDATE`.
- La transición a upsert final debe ocurrir solo tras:
  - estrategia de saneamiento aprobada por negocio,
  - reconciliación de conflictos históricos,
  - y validación de duplicados en cero para las claves que se quieran restringir.

### Reglas de upsert
Árbol de decisión ordenado:
1. Si no existe registro -> insert.
2. Si existe y `incoming_revision < source_revision` -> obsoleto; no aplicar.
3. Si existe y `incoming_revision == source_revision`:
   - si `payload_hash` coincide -> no-op idempotente.
   - si `payload_hash` difiere -> conflicto de misma revisión; enviar a cuarentena y no sobrescribir.
4. Si existe y `incoming_revision > source_revision`:
   - si `payload_hash` coincide -> no-op de contenido con avance obligatorio de metadatos:
     - `source_revision = incoming_revision`
     - `source_file_record_id = incoming file`
   - si `payload_hash` difiere -> update + `source_revision = incoming_revision` + `source_file_record_id = incoming file`.
- No se permite sobrescritura silenciosa con la misma revisión.

### Alternativa A: `INSERT ... ON DUPLICATE KEY UPDATE`
**Pros**
- Implementación más simple.

**Contras**
- Menor control para clasificar `obsolete/no-op/update/insert` en lotes grandes.
- Requiere constraints únicos plenamente saneados antes de activación.

### Alternativa B: staging + `UPDATE JOIN` + `INSERT SELECT`
**Pros**
- Mejor rendimiento y control para lotes de 100k+.
- Mejor observabilidad por categoría de resultado.
- Menor riesgo operativo para reconciliaciones complejas.

**Contras**
- Mayor complejidad de implementación y operación.

### Recomendación para 100k+
- Usar **staging + merge set-based** (`UPDATE JOIN` + `INSERT SELECT`).
- Preferir staging específica por tabla:
  - `staging_medida_h`
  - `staging_medidaqh`
  - `staging_medida_cch`
- Cada staging debe incluir:
  - columnas de clave de negocio
  - columnas de payload
  - `source_revision`
  - `source_file_record_id`
  - `payload_hash`
  - identificador de corrida/lote para aislar ejecuciones concurrentes

### Estrategia de tests
- Integración con base real y lotes grandes.
- Casos de obsolescencia, no-op por hash y actualización por revisión válida.
- Caso explícito de misma revisión con payload distinto -> conflicto/cuarentena sin update.
- Caso explícito de revisión mayor con mismo payload -> no-op de contenido con avance de `source_revision` y `source_file_record_id`.
- Pruebas de rollback y reintento idempotente.

### Definición operativa de cuarentena
En este plan, "cuarentena" significa:
- generación de archivo de reporte `.sge_quarantine.jsonl` con los registros conflictivos/fallidos,
- publicación de evento de outbox `FILE_PERSISTENCE_QUARANTINE` con metadatos y referencia al reporte,
- sin sobrescribir registros de negocio en conflicto.

Evolución opcional posterior:
- tabla dedicada de conflictos (`measure_conflicts`) para consulta analítica.

---

## Estrategia de despliegue seguro

### Feature flags
- Fase 1:
  - flag para activar/desactivar gate de obsolescencia por archivo.
  - flag de modo `dry-run`: evalúa y reporta qué se rechazaría, sin rechazar efectivamente.
  - fallback: registrar y procesar con comportamiento actual.
- Fase 2:
  - deploy de columnas e índices no únicos.
  - poblar nuevos campos en write path.
  - backfill asíncrono por lotes.
- Fase 3:
  - flag para activar staging/upsert por tipo de medida.
  - fallback a flujo actual de `saveAll` mientras no haya constraints/listado de duplicados saneado.
  - canary por tipo de medida antes de expansión completa.

### Rollout recomendado
1. Deploy de schema compatible.
2. Backfill de `file_records.source_family_key/source_revision`.
3. Activar métricas sin rechazo duro si se requiere dry-run.
4. Activar gate Fase 1 por flag.
5. Deploy columnas de versionado por medida.
6. Activar escritura de metadatos nuevos en inserts actuales.
7. Ejecutar backfill por lotes.
8. Ejecutar diagnóstico fino y cuantificación de deuda histórica por tipo (`equivalente` vs `conflictivo`).
9. Tomar decisión de negocio sobre estrategia de remediación (reconstrucción desde histórico vs saneamiento incremental).
10. Ejecutar remediación aprobada con auditoría y trazabilidad.
11. Activar staging/upsert en canary solo para tablas/rangos saneados y validados.
12. Crear constraints únicos solo tras validación de duplicados en cero y aprobación final.
13. Expandir staging/upsert a todos los tipos de medida.

### Métricas mínimas obligatorias
- Fase 1:
  - `revision_accepted_count`
  - `obsolete_rejected_count`
  - `revision_conflict_count`
  - `revision_parse_error_count`
  - `revision_max_seen`
  - `family_lock_wait_ms`
  - `family_lock_timeout_count`
  - `family_lock_retry_count`
- Validación legacy:
  - `legacy_duplicate_keys_count`
  - `legacy_duplicate_rows_count`
- Fase 3:
  - `staging_rows_loaded_count`
  - `upsert_insert_count`
  - `upsert_update_count`
  - `upsert_noop_count`
  - `upsert_obsolete_count`
  - `upsert_conflict_same_revision_count`
  - `upsert_batch_duration_ms`
  - `staging_cleanup_duration_ms`
- Alternativa C / operación por zonas:
  - `legacy_correction_received_count`
  - `legacy_correction_quarantined_count`
  - `legacy_correction_reviewed_count`
  - `trusted_zone_duplicate_count`
  - `trusted_zone_conflict_count`

---

## Validación de duplicados legacy (antes de constraints únicos)

### Detección en `medida_h`
```sql
SELECT
  id_cliente,
  fecha,
  tipo_medida,
  COUNT(*) AS dup_count
FROM medida_h
GROUP BY id_cliente, fecha, tipo_medida
HAVING COUNT(*) > 1
ORDER BY dup_count DESC, id_cliente, fecha;
```

### Detección en `medidaqh`
```sql
SELECT
  id_cliente,
  fecha,
  tipomed,
  COUNT(*) AS dup_count
FROM medidaqh
GROUP BY id_cliente, fecha, tipomed
HAVING COUNT(*) > 1
ORDER BY dup_count DESC, id_cliente, fecha;
```

### Detección en `medida_cch`
```sql
SELECT
  id_cliente,
  fecha,
  COUNT(*) AS dup_count
FROM medida_cch
GROUP BY id_cliente, fecha
HAVING COUNT(*) > 1
ORDER BY dup_count DESC, id_cliente, fecha;
```

### Qué hacer si aparecen duplicados legacy
1. No crear constraints únicos todavía.
2. Definir regla de supervivencia (por revisión, timestamps o ID).
3. Mover no-supervivientes a tabla de auditoría/cuarentena.
4. Repetir detección hasta cero duplicados.
5. Crear constraints únicos y recién entonces activar upsert final.

### Modo compatibilidad si no se pueden sanear inmediatamente
Si aparecen duplicados legacy y no se pueden resolver en la misma ventana:
1. No activar `INSERT ... ON DUPLICATE KEY UPDATE`.
2. No crear constraints únicos.
3. Mantener operación con staging + reglas explícitas de supervivencia.
4. Para updates, seleccionar un único registro objetivo por clave de negocio usando la regla aprobada.
5. Reportar métricas de claves ambiguas y enviar casos no resolubles a cuarentena.
6. Activar constraints únicos solo cuando la detección de duplicados devuelva cero filas.

### Hallazgos reales actuales (producción)
Resultado agregado de la consulta de semáforo de duplicados ejecutada en producción:

```json
[
  {
    "tabla": "medida_h",
    "total_rows": 874387,
    "distinct_keys": 678752,
    "duplicate_keys": 166866,
    "duplicate_extra_rows": 195635
  },
  {
    "tabla": "medidaqh",
    "duplicate_keys": 10714301,
    "duplicate_extra_rows": 12080686
  },
  {
    "tabla": "medida_cch",
    "duplicate_keys": 1277941,
    "duplicate_extra_rows": 1520065
  }
]
```

Interpretación operativa:
- El volumen de duplicidad legacy es alto y representa riesgo de ruptura de despliegue/operación si se crean constraints únicos ahora.
- `medidaqh` concentra el mayor riesgo por cardinalidad de claves ambiguas.
- La evidencia actual no permite atribuir toda la causa a replay técnico: hay mezcla plausible de reprocesos, consolidaciones históricas y potenciales cargas manuales multi-origen.
- El saneamiento debe ejecutarse por iteraciones/lotes o mediante reconstrucción controlada; no en estrategia reactiva sin decisión de negocio.

Decisión obligatoria inmediata:
- **No crear en esta iteración constraints únicos** sobre claves de negocio en `medida_h`, `medidaqh` y `medida_cch`.
- **No habilitar** rutas que dependan de unicidad ya saneada (`INSERT ... ON DUPLICATE KEY UPDATE`).
- Mantener `saveAll`/staging en modo compatibilidad hasta reducir duplicados a cero de forma sostenida.

Consultas de control recomendadas para seguimiento continuo (producción):

```sql
SELECT 'medida_h' AS tabla,
       COUNT(*) AS duplicate_keys,
       COALESCE(SUM(dup_count - 1), 0) AS duplicate_extra_rows
FROM (
  SELECT id_cliente, fecha, tipo_medida, COUNT(*) AS dup_count
  FROM medida_h
  GROUP BY id_cliente, fecha, tipo_medida
  HAVING COUNT(*) > 1
) t
UNION ALL
SELECT 'medidaqh' AS tabla,
       COUNT(*) AS duplicate_keys,
       COALESCE(SUM(dup_count - 1), 0) AS duplicate_extra_rows
FROM (
  SELECT id_cliente, fecha, tipomed, COUNT(*) AS dup_count
  FROM medidaqh
  GROUP BY id_cliente, fecha, tipomed
  HAVING COUNT(*) > 1
) t
UNION ALL
SELECT 'medida_cch' AS tabla,
       COUNT(*) AS duplicate_keys,
       COALESCE(SUM(dup_count - 1), 0) AS duplicate_extra_rows
FROM (
  SELECT id_cliente, fecha, COUNT(*) AS dup_count
  FROM medida_cch
  GROUP BY id_cliente, fecha
  HAVING COUNT(*) > 1
) t;
```

Mitigaciones priorizadas (siguiente iteración):
1. Definir y aprobar regla de supervivencia determinística por tabla (revisión, timestamp, id).
2. Implementar tabla de auditoría/cuarentena para no-supervivientes con trazabilidad completa.
3. Ejecutar dedupe incremental por lotes pequeños (rango temporal/familia), con validación post-lote.
4. Publicar métricas diarias `legacy_duplicate_keys_count` y `legacy_duplicate_rows_count` por tabla.
5. Activar constraints únicos únicamente cuando el semáforo de duplicados sea `0` de forma sostenida.

### Diagnóstico fino adicional en `medidaqh`
Se ejecutó un segundo análisis para distinguir entre:
- duplicidad física/técnica del mismo registro lógico;
- conflicto real de payload de negocio para la misma clave (`id_cliente`, `fecha`, `tipomed`).

Aprendizaje importante del diagnóstico:
- una primera aproximación de fingerprint sobre "todas las columnas" sobreestimó el conflicto real porque incluyó columnas técnicas como la PK física (`id_medidaQH`) y columnas de auditoría (`created_on`, etc.);
- por tanto, ese resultado inicial **no debía** usarse para justificar dedupe de negocio ni decisiones definitivas de merge.

Consulta refinada aplicada:
- exclusión explícita de columnas técnicas/auditoría del fingerprint de payload;
- comparación del resto de columnas como contenido funcional del registro.

Resultado refinado en producción:

```json
[
  {
    "duplicate_type": "EXACT_OR_EQUIVALENT_PAYLOAD",
    "keys_count": 1214521,
    "rows_involved": 2547891
  },
  {
    "duplicate_type": "CONFLICTING_BUSINESS_PAYLOAD",
    "keys_count": 9499780,
    "rows_involved": 20247096
  }
]
```

Interpretación obligatoria de este resultado:
- existe una porción material de duplicidad técnica/replay (`EXACT_OR_EQUIVALENT_PAYLOAD`) que apunta a múltiples inserciones físicas del mismo registro lógico;
- aun corrigiendo el sesgo por columnas técnicas, sigue existiendo una masa crítica muy alta de claves con `CONFLICTING_BUSINESS_PAYLOAD` en `medidaqh`;
- por tanto, el problema **no** es solo replay técnico: también hay colisiones funcionales de contenido que impiden asumir unicidad segura hoy;
- cualquier estrategia futura de saneamiento deberá tratar por separado:
  1. duplicado técnico/equivalente, y
  2. conflicto real de negocio.

### Supuestos invalidados y contradicciones detectadas
- Supuesto inicial inválido: "la mayor parte del problema se resuelve con dedupe técnico por replay".
- Contradicción operativa: una activación temprana de constraints/upsert final contradice el volumen observado de `CONFLICTING_BUSINESS_PAYLOAD`.
- Supuesto pendiente de evidencia: no se puede afirmar todavía qué porcentaje de conflicto corresponde a:
  1. replay técnico,
  2. correcciones legítimas,
  3. consolidaciones históricas multi-fuente.
- Decisión derivada: la ruta de saneamiento definitivo no puede cerrarse sin validación de negocio sobre semántica histórica.

Conclusión operativa reforzada:
- el sistema no está en condiciones de soportar en esta iteración constraints únicos sobre la clave candidata de `medidaqh`;
- tampoco está en condiciones de aplicar dedupe masivo ciego con criterio puramente técnico (`keep latest`, `keep max(id)`, etc.) sin validación de negocio.

### Impactos colaterales y riesgos explícitos de introducir constraints ahora
Introducir ahora constraints únicos relacionados con esta duplicidad tendría impactos colaterales de alto riesgo:

1. **Ruptura de despliegue/migración**
   - la creación del constraint puede fallar inmediatamente por filas existentes que violan la unicidad;
   - el despliegue quedaría incompleto o requeriría rollback operativo.

2. **Ruptura del write path en producción**
   - cualquier inserción futura sobre claves actualmente ambiguas empezaría a fallar;
   - eso afectaría el procesamiento batch actual basado en `saveAll` y podría dejar archivos en error parcial o reintento continuo.

3. **Pérdida potencial de correcciones válidas**
   - si se intenta "resolver" el problema con dedupe ciego antes del constraint, se puede conservar la fila equivocada y descartar una corrección legítima;
   - el riesgo es mayor en el subconjunto clasificado como `CONFLICTING_BUSINESS_PAYLOAD`.

4. **Inconsistencias funcionales visibles aguas abajo**
   - reportes, conciliaciones, exportaciones o procesos consumidores pueden empezar a ver un estado distinto según qué fila sobreviva;
   - sin regla de supervivencia aprobada, el resultado sería difícil de justificar ante negocio/auditoría.

5. **Aumento de incidentes operativos**
   - más errores de persistencia, reprocesos, cuarentenas y necesidad de intervención manual;
   - riesgo de backlog si los archivos siguen entrando mientras la tabla permanece ambigua.

6. **Conclusiones erróneas si no se separa técnica vs negocio**
   - mezclar PK física/auditoría con payload de negocio produce falsos conflictos;
   - tomar decisiones estructurales sin esa separación llevaría a un plan de saneamiento incorrecto.

## Análisis de Deuda Histórica y Estrategias de Remediación

### Contexto
La deuda histórica detectada en producción es suficientemente grande para condicionar la secuencia de implementación y la estrategia de cierre de Fase 3.

### Hipótesis de origen (no concluyentes todavía)
- reprocesamientos/replays técnicos;
- consolidaciones históricas desde múltiples bases SQL Server;
- cargas manuales o reconciliaciones antiguas fuera del pipeline actual.

No existe todavía evidencia suficiente para cuantificar con precisión la contribución de cada causa.

### Alternativa A - Reconstrucción desde histórico (recomendada si negocio la aprueba)
Flujo propuesto:
1. Implementar Fase 1 y observabilidad completa.
2. Respaldar tablas de medidas.
3. Vaciar tablas de medidas.
4. Reprocesar histórico completo de archivos.
5. Reconstruir estado usando nuevas reglas de versionado.

Condiciones obligatorias:
- disponibilidad íntegra de todos los archivos históricos;
- aceptación formal de que el archivo histórico es la fuente oficial de verdad;
- capacidad de reproceso determinístico y repetible;
- ventana operativa y capacidad de cómputo para reproceso masivo.

Ventajas:
- reduce ambigüedad histórica de raíz;
- evita reglas de supervivencia ad-hoc sobre datos ambiguos;
- acelera llegada a constraints/upsert final en un dataset reconstruido.

Riesgos:
- dependencia total de completitud/calidad del repositorio histórico;
- riesgo operativo alto en tiempo de reconstrucción;
- posible diferencia funcional respecto al estado actual si hubo ajustes manuales no trazados en archivos.

### Alternativa B - Saneamiento incremental sobre base actual
Flujo propuesto:
1. Mantener base actual.
2. Definir reglas de supervivencia por tabla/tipo de conflicto.
3. Reconciliar conflictos y deduplicar progresivamente por lotes.
4. Introducir constraints al final, cuando haya cero duplicados en alcance.

Ventajas:
- menor disrupción inicial de operación;
- control fino por lotes y capacidad de rollback localizado.

Riesgos:
- horizonte más largo de convergencia;
- alta complejidad funcional para reglas de supervivencia en conflictos reales;
- mayor costo operativo en conciliación manual y seguimiento prolongado.

### Alternativa C - Historical Freeze + Forward Clean

#### Objetivo
Reducir riesgo operativo inmediato separando deuda histórica del flujo nuevo: preservar histórico sin modificación automática y garantizar que los nuevos periodos ingresen bajo reglas de revisión/versionado limpias.

#### Parámetro de control
- `freeze_cutoff_date` (configurable por entorno): fecha/hora de corte que divide histórico congelado de periodos nuevos.

#### Zonas operativas
- `LEGACY ZONE`: registros con periodo `< freeze_cutoff_date`; se preservan, no se corrigen automáticamente.
- `TRUSTED ZONE`: registros con periodo `>= freeze_cutoff_date`; aplican reglas nuevas (Fase 1 + metadatos Fase 2 y, cuando corresponda, estrategia final de upsert).
- `RECONCILIATION ZONE`: backlog de correcciones históricas y conflictos para análisis, decisión y eventual aplicación controlada.

#### Reglas operativas
1. Registros históricos previos al corte se consideran congelados para update automático.
2. Archivos nuevos posteriores al corte se procesan con reglas de revisión/versionado vigentes.
3. Si un archivo/corrección intenta modificar periodo congelado:
   - no actualiza tablas de negocio de medidas,
   - se persiste auditoría,
   - se mueve a cuarentena,
   - se genera evidencia para revisión posterior.

#### Estado y razón de fallo
- Nuevo estado operativo sugerido: `QUARANTINED` (o equivalente de dominio).
- Nuevo `FailureReason` sugerido: `LEGACY_PERIOD_CORRECTION` (o equivalente).

#### Flujo para correcciones históricas (periodo congelado)
1. archivo recibido;
2. detección de periodo objetivo (`min/max fecha`) y comparación con `freeze_cutoff_date`;
3. persistencia de `file_record` con auditoría completa y marca de cuarentena;
4. movimiento físico/lógico a cola/ubicación de cuarentena;
5. generación de evidencia (`.jsonl`/evento/outbox) para análisis;
6. sin modificación de `medida_h`, `medidaqh`, `medida_cch`.

#### Estructura de cuarentena
Opción 1 - Basada en archivos:
- almacenar payload original + metadatos en filesystem/object storage.
- Pros: rápida de implementar, bajo acoplamiento con schema relacional.
- Contras: consultas analíticas menos eficientes; requiere disciplina de indexación externa.

Opción 2 - Basada en tabla (`legacy_corrections_quarantine`):
- persistir metadatos normalizados (file_record_id, periodo, razón, hash, estado revisión, puntero a payload).
- Pros: consultas y reporting operativos directos, mejor trazabilidad SQL.
- Contras: mayor trabajo de modelado, crecimiento de BD, housekeeping requerido.

Recomendación práctica:
- corto plazo: archivo + evento (time-to-market).
- mediano plazo: tabla de cuarentena para gobierno operativo y auditoría.

#### Riesgos ocultos y escenarios límite de Alternativa C
- **Correcciones históricas críticas**: puede bloquear rectificaciones legítimas (facturación/regulatorio) si no existe ruta de excepción.
- **Archivo mixto (fechas legacy + trusted)**: requiere política explícita (split por registro o cuarentena total del archivo).
- **Timezone/normalización de fecha**: puede clasificar mal periodos respecto al corte si no se fija zona horaria única.
- **Cambios de cutoff en caliente**: mover el corte sin control puede generar comportamiento no determinista.
- **Acumulación en reconciliación**: backlog creciente si no existe proceso formal de revisión y SLA.
- **Percepción de "problema resuelto"**: C mitiga riesgo futuro, pero no elimina deuda histórica existente.

Controles obligatorios para mitigar riesgos en C:
- política de excepciones aprobada por negocio para casos regulatorios;
- regla explícita para archivos mixtos (preferido: split por registro; fallback: cuarentena total);
- cutoff inmutable por ventana operativa con cambio solo vía control de cambios;
- zona horaria oficial única para clasificación temporal;
- tablero de backlog y SLA de reconciliación.

#### Estrategia de validación continua por zonas
- medir duplicados y conflictos en `LEGACY ZONE` (observabilidad de deuda remanente);
- medir duplicados/conflictos en `TRUSTED ZONE` con objetivo de convergencia a cero;
- reportar tendencia semanal de `trusted_zone_duplicate_count` y `trusted_zone_conflict_count`;
- condición de avance para endurecimiento (constraints/upsert final): `TRUSTED ZONE` estable en cero durante ventana acordada.

### Criterios de elección entre A y B
- **Trazabilidad histórica disponible**: si falta histórico confiable, A pierde viabilidad.
- **Aceptación de fuente de verdad**: si negocio no acepta archivo como SoT, A no procede.
- **Tiempo para converger**: A puede converger más rápido si las precondiciones se cumplen.
- **Riesgo operativo**: B distribuye riesgo en el tiempo pero exige disciplina sostenida.

### Comparación A vs B vs C

| Criterio | Alternativa A: Reconstrucción total | Alternativa B: Saneamiento incremental | Alternativa C: Historical Freeze + Forward Clean |
|---|---|---|---|
| Complejidad técnica | Alta (reproceso integral) | Alta (reglas y reconciliación por lotes) | Media (segmentación por cutoff + cuarentena) |
| Riesgo operativo inmediato | Alto (cambio masivo) | Medio (riesgo distribuido) | Bajo/Medio (protege futuro, deuda queda aislada) |
| Tiempo de implementación inicial | Medio/Alto | Alto | Bajo/Medio |
| Costo de mantenimiento | Medio (si converge rápido) | Alto (operación prolongada) | Medio (operar reconciliación y zonas) |
| Dependencia de histórico completo | Muy alta | Baja/Media | Baja |
| Velocidad para estabilizar flujo nuevo | Media | Baja | Alta |
| Resolución definitiva de deuda histórica | Alta (si histórico es confiable) | Alta (más lenta) | Baja/Media (no resuelve, contiene) |

Evaluación arquitectónica con contexto actual:
- Si negocio necesita contención rápida con bajo riesgo operativo, **C es la mejor estrategia transitoria**.
- Si se confirma histórico completo y SoT por archivos, A puede ser estrategia definitiva posterior.
- B sigue vigente cuando no hay viabilidad de A y se requiere limpieza progresiva controlada.

## Decisión Arquitectónica Pendiente de Negocio

### Decisiones que requieren aprobación explícita
- estrategia de remediación de deuda histórica (`Alternativa A`, `Alternativa B` o `Alternativa C`);
- definición formal de reglas de supervivencia para conflictos reales;
- política de reconstrucción desde histórico y criterio de "fuente de verdad";
- momento y condiciones para constraints únicos;
- estrategia final de upsert en producción;
- umbral objetivo de calidad de datos previo a endurecimiento del modelo.
- definición y gobernanza de `freeze_cutoff_date` (incluyendo timezone y cambios controlados).

### Actividades que pueden continuar inmediatamente (seguras)
- soporte de revisiones `.0` a `.10+`;
- parseo de `source_family_key` y `source_revision`;
- gate de obsolescencia por familia;
- locking transaccional por familia;
- métricas y observabilidad reforzada;
- despliegue en `dry-run`;
- backfill de familia/revisión en `file_records`.
- activación funcional de Fase 2 (columnas/metadatos) en modo compatible.
- implementación de segmentación por `freeze_cutoff_date` y ruteo a cuarentena para correcciones legacy (si se aprueba C).

### Actividades bloqueadas hasta decisión de negocio
- saneamiento masivo definitivo;
- deduplicación final sobre tablas históricas;
- creación de constraints únicos en claves candidatas;
- habilitación de `INSERT ... ON DUPLICATE KEY UPDATE` como estrategia final;
- reconstrucción completa desde histórico (si no hay aprobación formal);
- cierre de reglas de supervivencia definitivas.

### Política transitoria obligatoria
Hasta que negocio tome la decisión arquitectónica:
- **prohibido crear constraints únicos** sobre `medida_h`, `medidaqh`, `medida_cch`;
- **prohibido habilitar** `INSERT ... ON DUPLICATE KEY UPDATE` como solución final;
- mantener flujo en **modo compatibilidad** con clasificación de duplicidad y métricas de riesgo;
- tratar cualquier dedupe como actividad controlada, auditable y reversible.

### Recomendación transitoria priorizada
- Adoptar **Alternativa C** como recomendación principal de corto plazo para iniciar ciclo operativo limpio sin tocar deuda histórica automáticamente.
- Mantener abierta la decisión definitiva entre A y B para remediación histórica profunda, con evidencia adicional y aprobación de negocio.

---

## Decisiones abiertas
- Clave de negocio final por tabla después de validar colisiones legacy.
- Confirmar `FailureReason` para obsolescencia (`OBSOLETE_REVISION` u otro nombre).
- Confirmar `FailureReason` para conflicto de misma revisión (`REVISION_CONFLICT` u otro nombre).
- Definir regla de supervivencia para duplicados legacy.
- Aprobar estrategia de remediación de deuda histórica (`Alternativa A`, `Alternativa B` o `Alternativa C`).
- Confirmar si el histórico de archivos se reconoce como fuente oficial de verdad para reconstrucción.
- Definir `freeze_cutoff_date` inicial y política de cambios (timezone, gobernanza y control de cambios).
- Definir campos exactos incluidos en `payload_hash` para cada tipo de medida.
- Confirmar tamaño final de `source_family_key` si hay restricciones de naming externas (default propuesto: `VARCHAR(191)`).

### Decisiones por defecto adoptadas en este plan
- Fase 1 conflictiva de misma revisión por archivo:
  - mismo hash de archivo -> `DUPLICATED_CONTENT`
  - hash distinto -> `REJECTED` con `FailureReason.REVISION_CONFLICT`
  - opcional por flag: cuarentena
- Rechazo de obsolescencia con auditoría:
  - evaluar gate y persistir `file_record` con estado final dentro de la misma transacción
  - re-check obligatorio al inicio de procesamiento
- Fase 3 con revisión mayor y mismo payload:
  - no-op de contenido con avance obligatorio de `source_revision` y `source_file_record_id`
- Rango de revisión soportado:
  - extensión numérica abierta `\d+` (entero no negativo), validada como `0 <= revision <= Integer.MAX_VALUE`
- Modo de despliegue Fase 1:
  - primer despliegue en `dry-run` obligatorio para medir impacto antes de rechazo duro

### Evolución futura de escalabilidad (`file_records`)
Si el volumen crece a decenas de millones de filas y la consulta de `MAX(source_revision)` se vuelve costosa, evaluar tabla materializada:
- `family_revision_state(source_family_key, max_succeeded_revision, last_file_record_id, updated_at)`

Esta optimización no es necesaria en Fase 1, pero queda registrada como evolución prevista.

## Recomendación final
- Implementar Fase 1 primero para bloquear obsoletos y normalizar revisiones.
- Implementar Fase 2 como transición segura de modelo, manteniendo writes en modo compatible.
- Adoptar **Alternativa C (Historical Freeze + Forward Clean)** como estrategia principal transitoria para proteger el flujo futuro desde el inicio del nuevo ciclo operativo.
- Mantener diseño de Fase 3 como destino técnico, pero **no activarlo en producción** hasta resolver deuda histórica según decisión de negocio.
- Elegir formalmente estrategia definitiva de remediación histórica (A/B) en paralelo a C, con evidencia adicional y aprobación de negocio.
- Activar constraints únicos solo tras remediación completada y validación consistente de duplicados en cero en el alcance objetivo.
- Mantener ruta de compatibilidad y observabilidad reforzada mientras persista ambigüedad histórica.
- Mantener prohibición temporal de constraints únicos y de `INSERT ... ON DUPLICATE KEY UPDATE` hasta cierre de decisión arquitectónica y validación técnica.
- Activar cambios mediante feature flags, métricas y gates de aprobación explícitos por etapa.


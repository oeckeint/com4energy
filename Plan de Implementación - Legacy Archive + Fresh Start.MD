Plan de Implementación - Legacy Archive + Fresh Start

Estado: Propuesto
Fecha: Junio 2026
Sistema: c4e-ingestion-service
Fuente Oficial de Verdad (Source of Truth): Archivos de medidas

⸻

1. Objetivo

Rediseñar la persistencia de medidas eliminando completamente la dependencia operativa de las tablas históricas actuales.

La nueva arquitectura asumirá que:

* Los archivos son la única fuente oficial de verdad.
* La base de datos es una proyección reconstruible.
* El histórico actual se conserva únicamente con fines de auditoría y consulta excepcional.
* Las nuevas tablas comenzarán vacías.
* Los archivos históricos podrán reprocesarse posteriormente bajo las nuevas reglas.

⸻

2. Principios Arquitectónicos

Principio #1 - Source of Truth

La fuente oficial de verdad son los archivos.

La base de datos:

* puede reconstruirse,
* puede eliminarse y regenerarse,
* no contiene información irrecuperable.

⸻

Principio #2 - Idempotencia

Procesar un mismo archivo varias veces debe producir exactamente el mismo resultado.

⸻

Principio #3 - Correcciones por revisión

Las revisiones representan versiones de una misma familia de archivos.

Ejemplos válidos:

F5D_0001.0
F5D_0001.1
F5D_0001.2
F5D_0025.3
F5D_0025.4

No existe ninguna regla que obligue a comenzar en .0.

Semántica oficial de revisiones (actualizada):

* Las revisiones son incrementales (correcciones y/o adiciones), no snapshots completos.
* Una revisión superior solo tiene precedencia para registros que compartan la misma clave de negocio.
* La ausencia de un registro en una revisión superior NO implica DELETE.

Regla válida:

revision mayor reemplaza revision menor por clave de negocio

Restricción de dominio:

revision >= 0

La revisión `0` tiene significado formal como valor mínimo válido. En el modelo aprobado el campo técnico se denomina `revision` (SMALLINT UNSIGNED).

⸻

Principio #5 - Procesamiento Ordenado de Revisiones

Las revisiones de una misma familia deben procesarse en orden ascendente.

Si ya existe una revisión superior aceptada para la familia:

incoming_revision < current_max_revision -> REJECTED (OBSOLETE_REVISION)

No se soporta:

* replay fuera de orden;
* reconstrucción automática con precedencias dinámicas;
* reconciliación de revisiones tardías.

Justificación:

* menor complejidad operativa;
* comportamiento determinístico y auditable;
* el cliente controla y garantiza el orden de entrega de archivos.

⸻

Principio #4 - Legacy Inmutable

Una vez migradas:

medida_h_legacy
medida_qh_legacy
medida_cch_legacy

se consideran:

READ ONLY

No participan más en el flujo de ingestión.

⸻

3. Estrategia de Migración

⸻

Paso 1 - Congelar histórico actual

Renombrar tablas existentes:

medida_h      -> medida_h_legacy
medidaqh      -> medida_qh_legacy
medida_cch    -> medida_cch_legacy

Nota de nomenclatura:

La tabla histórica `medidaqh` se migra a `medida_qh_legacy` para alinear el ecosistema a la convención `medida_*`.

⸻

Paso 2 - Crear tablas nuevas

Crear:

medida_h
medida_qh
medida_cch

completamente vacías.

⸻

Paso 3 - Activar nuevo pipeline

Todo archivo nuevo ingresará exclusivamente a:

medida_h
medida_qh
medida_cch

⸻

Paso 4 - Recargas históricas

Cuando negocio necesite información histórica:

Seleccionar período
↓
Reprocesar archivos
↓
Reconstruir información
↓
Persistir bajo nuevas reglas

Restricción obligatoria de recarga:

Los archivos históricos deben procesarse en orden ascendente de revisión por familia.

No se soporta recarga fuera de orden.

⸻

4. Modelo de Datos Nuevo

⸻

Campos funcionales

Se mantienen los campos actuales de negocio.

⸻


Campos técnicos obligatorios

Agregar en todas las tablas nuevas (modelo oficial aprobado):

- id_file_record BIGINT NOT NULL
- source_family_key VARCHAR(64) NOT NULL
- revision SMALLINT UNSIGNED NOT NULL
- processing_iteration SMALLINT UNSIGNED NOT NULL DEFAULT 0
- payload_hash CHAR(64) NOT NULL
- payload_hash_version SMALLINT NOT NULL DEFAULT 1
- created_on DATETIME NOT NULL
- updated_on DATETIME NOT NULL

Notas:

* Se elimina cualquier referencia a `source_revision`, `source_file_record_id`, `ingestion_run_id` y `source_file_name` en el modelo técnico.
* `id_file_record` referencia el identificador del `file_records.id` y debe mantenerse exactamente con ese nombre y tipo BIGINT por compatibilidad con servicios existentes.

⸻


Índices mínimos obligatorios

Las únicas combinaciones de índices aprobadas por arquitectura para las tablas de medidas son las siguientes (mantener estrictamente):

* `INDEX (source_family_key, revision)`
* `INDEX (id_file_record)`

Justificación:

* `source_family_key, revision` permite gate de obsolescencia y operaciones de decisión por familia.
* `id_file_record` permite trazabilidad hacia el `file_record` que introdujo/actualizó el registro.

No se deben proponer índices adicionales salvo que el documento ya los requiera explícitamente y hayan sido aprobados.

Nota de arquitectura sobre longitud de familia:

`source_family_key VARCHAR(64)` es la definición aprobada. Actualizar justificaciones, ejemplos e índices para reflejar esta longitud real aprobada.

Cualquier ajuste futuro de longitud requiere revisión de índices, constraints y compatibilidad MySQL.

⸻

Objetivo

Permitir:

* trazabilidad,
* auditoría,
* reconciliación,
* detección de conflictos,
* futuras migraciones.

Nota:

`id_file_record` se incorpora desde el inicio para trazabilidad hacia el registro de archivo (`file_records.id`) que introdujo o aplicó el cambio; permite debugging de reprocesos y auditoría operativa.

`source_family_key` se incorpora para:

* trazabilidad completa al archivo de origen;
* auditoría por familia;
* debugging de conflictos;
* reconstrucción histórica por familia;
* correlación directa entre registro y archivo fuente.

La revisión por sí sola no identifica de forma única el origen de un dato.

Canonicalización obligatoria de `source_family_key`:

* `source_family_key` se persiste únicamente en forma canónica.
* La canonicalización ocurre antes de cualquier comparación, locking, búsqueda, persistencia y validación de revisiones.
* Regla canónica: extraer familia desde nombre de archivo y convertir a `UPPERCASE`.

Ejemplos equivalentes:

F5D_001.0
f5d_001.0
F5d_001.0

=> `source_family_key = F5D_001`

Ejemplo:

F5D_0031_0894_20250311.4

source_family_key = F5D_0031_0894_20250311
revision   = 4

⸻

5. Control de Revisiones

⸻

Metadata derivada

Desde el nombre del archivo:

source_family_key
source_revision

Canonicalización de nombre de archivo (obligatoria):

* El nombre de archivo debe normalizarse a `UPPERCASE` antes de:
  * validaciones de duplicado,
  * extracción de familia,
  * extracción de revisión,
  * persistencia de metadata.
* `F5D_001.0` y `f5d_001.0` se consideran equivalentes.

Esta regla no modifica la estrategia de hash de archivo; únicamente define canonicalización previa de metadata y comparaciones.

Ejemplo:

F5D_0031_0894_20250311.4

Produce:

source_family_key =
F5D_0031_0894_20250311
revision =
4

⸻

Regla de aceptación

Si:

incoming_revision >
max_succeeded_revision

Aceptar.

⸻

Si:

incoming_revision <
max_succeeded_revision

Rechazar.

⸻

Si:

incoming_revision ==
max_succeeded_revision

Evaluar:

mismo hash

DUPLICATED_CONTENT

⸻

hash distinto

REVISION_CONFLICT

Regla de aplicación por registro (obligatoria):

* Si la clave de negocio no existe -> `INSERT`.
* Si la clave existe y `incoming_revision` es mayor -> `UPDATE`.
* Si la clave existe y `incoming_revision` es menor -> `OBSOLETE` (no aplicar).
* Si la clave existe y `incoming_revision` es igual:
  * `payload_hash` igual -> `NOOP` idempotente.
  * `payload_hash` distinto -> `CONFLICT` (sin sobrescritura silenciosa).

Semántica explícita de `UPDATE`:

`UPDATE` es una operación de corrección incremental sobre la clave de negocio.

Campos de negocio:

* Se sustituyen únicamente por los valores del nuevo registro.
* Solo aplica a campos funcionales presentes en el archivo procesado.
* Los campos de negocio no incluidos en el archivo de corrección NO se modifican.

Campos técnicos (actualizar siempre en cada UPDATE aceptado):

* `revision`             <- revisión del archivo que produjo el cambio.
* `processing_iteration` <- cuántas veces la misma revisión fue procesada (ver semántica desplegada más abajo).
* `id_file_record`       <- referencia al `file_records.id` del archivo aplicado.
* `source_family_key`    <- familia del archivo aplicado.
* `payload_hash`         <- hash del contenido funcional actualizado.
* `payload_hash_version` <- versión del algoritmo de canonicalización.
* `updated_on`           <- timestamp de la actualización.

Processing iteration (semántica y propósito):

* `processing_iteration` cuenta cuántas veces una misma `revision` fue procesada efectivamente por el pipeline. Su propósito es trazabilidad operativa y diagnóstico de reprocesos sin cambiar la semántica de `revision`.
* Comportamiento esperado:
  * Inicio: cuando una revisión se procesa por primera vez, `processing_iteration = 0`.
  * Reprocesos: cada vez que se reprocesa la misma revisión (mismo `source_family_key` + `revision`), se incrementa `processing_iteration` en 1 para los registros afectados por ese reproceso.
  * La iteración NO reemplaza la revisión: `revision` mantiene la semántica de corrección funcional y precedencia; `processing_iteration` solo aporta historial y visibilidad de re-ejecuciones.
  * En cada UPDATE aceptado, `processing_iteration` debe reflejar la iteración del `id_file_record` aplicado.

No confundir con:

* actualización total de fila (incompatible con semántica incremental);
* merge de columnas entre versiones (no soportado);
* reemplazo de PK o clave de negocio (prohibido).

Regla de no-eliminación implícita:

La ausencia de registros en una revisión posterior nunca se interpreta como `DELETE`.

Regla de procesamiento ordenado (obligatoria):

Si ya existe una revisión superior `SUCCEEDED` para la misma familia:

incoming_revision < max_succeeded_revision(family)

El archivo es rechazado con:

estado: REJECTED
motivo: OBSOLETE_REVISION

No se soporta recuperación automática de revisiones tardías.

Niveles de exclusión mutua (complementarios):

* `FileRecord` + `PROCESSING` = bloqueo por archivo.
* `file_revision_family_state` + `SELECT ... FOR UPDATE` = serialización por familia (usando `source_family_key` canónica).

El mecanismo de Family Revision State and Locking no evita que un mismo archivo sea procesado dos veces.
Ese problema ya está resuelto por `FileRecord.status = PROCESSING`.
Su objetivo es garantizar orden de procesamiento entre revisiones de una misma familia.

Implementación obligatoria:

La idempotencia por archivo y por revisión se habilita desde el primer despliegue productivo (no diferida).

Estados formales de `FileRecord` (obligatorio):

PENDING
PROCESSING
SUCCEEDED
REJECTED
DUPLICATED_CONTENT
REVISION_CONFLICT
QUARANTINED

Transiciones válidas mínimas:

PENDING -> PROCESSING
PROCESSING -> SUCCEEDED
PROCESSING -> REJECTED
PROCESSING -> DUPLICATED_CONTENT
PROCESSING -> REVISION_CONFLICT
PROCESSING -> QUARANTINED

Transiciones permitidas de gate previo (sin entrar a procesamiento de medidas):

PENDING -> REJECTED
PENDING -> DUPLICATED_CONTENT
PENDING -> REVISION_CONFLICT
PENDING -> QUARANTINED

No se permiten transiciones directas desde estados terminales a `PENDING`.

Formalización de error de procesamiento:

`PROCESSING_ERROR` se representa como:

REJECTED + reason = PROCESSING_ERROR

Semántica estricta:

`PROCESSING_ERROR` aplica únicamente cuando TX2 no puede completarse por fallo de infraestructura
o error irrecuperable del archivo entero.

Errores de registros individuales aislados por binary split NO generan `PROCESSING_ERROR` en el `FileRecord`.
El archivo puede finalizar `SUCCEEDED` con registros individuales exportados para corrección.

⸻

6. Persistencia por Registro

⸻

Clave de negocio

medida_h

(id_cliente, fecha)

⸻

medida_qh

(id_cliente, fecha)

⸻

medida_cch

(id_cliente, fecha)

⸻

Restricciones únicas

Las nuevas tablas iniciarán con constraints únicos desde el primer día.

Esto es posible porque:

* las tablas nacen vacías,
* no existe deuda histórica.

Definición formal de constraints únicos por tabla (modelo aprobado):

```sql
-- medida_h
UNIQUE KEY uk_medida_h_business (id_cliente, fecha)

-- medida_qh
UNIQUE KEY uk_medida_qh_business (id_cliente, fecha)

-- medida_cch
UNIQUE KEY uk_medida_cch_business (id_cliente, fecha)
```

Nota de compatibilidad con particionado (Fase 5):

En MySQL, todo `UNIQUE KEY` en tabla particionada debe incluir la columna de particionado.
Al activar `PARTITION BY RANGE (YEAR(fecha))` en Fase 5, los constraints anteriores deberán
redefinirse incluyendo la columna derivada de año o `fecha` directamente, según el DDL validado.

Requisito de inicio:

Las operaciones de escritura deben ser idempotentes desde el primer release, evitando dependencia de dedupe posterior.

⸻

7. Payload Hash

⸻

Objetivo

Determinar si dos registros representan exactamente el mismo contenido funcional.

⸻

Excluir

id
created_on
updated_on
revision
id_file_record
source_family_key
processing_iteration
payload_hash
payload_hash_version

⸻

Incluir

Exclusivamente campos de negocio.

⸻

Algoritmo

Versión inicial:

PayloadHashV1

registrada mediante:

payload_hash_version = 1

⸻

Canonicalización PayloadHashV1

Objetivo:

Garantizar que dos registros funcionalmente equivalentes produzcan exactamente el mismo hash.

Reglas aprobadas:

1. Strings

Normalizar a mayúsculas antes del hash.

Ejemplo:

ABC
abc
AbC

=> equivalentes

2. Valores numéricos

Los valores numéricos se comparan por valor matemático, no por representación textual.

Ejemplo:

123.4
123.40
123.4000

=> equivalentes

La representación usada para hash debe ser canónica y determinística.

2.1 Equivalencia NULL y string vacío

Para campos string, `NULL` y `""` son equivalentes para canonicalización.

Ejemplo:

cliente = NULL
cliente = ""

=> mismo valor canónico para hash.

Intención:

Evitar falsos conflictos y preservar idempotencia funcional.

2.2 Whitespace en strings

Regla canónica aprobada:

* Aplicar `trim` a strings antes de hash.
* Si el resultado queda vacío, el valor canónico es `""`.
* Dado 2.1, `""` y `NULL` son equivalentes para hash.

Ejemplos equivalentes:

""
" "
"    "
"\t"
"\n"
NULL

=> mismo valor canónico.

3. Fechas y timestamps

Formato requerido:

YYYY-MM-DD HH:MM:SS

Si el dato llega solo como fecha (por ejemplo `2025-01-01`), se considera inválido y se rechaza en validación.

No se permite inferir hora.

4. Campos técnicos excluidos

id
created_on
updated_on
revision
id_file_record
source_family_key
processing_iteration
payload_hash
payload_hash_version

5. Versionado

Cualquier cambio futuro de estas reglas requiere:

payload_hash_version++

y ADR específico.

6. Determinismo técnico obligatorio

Para garantizar determinismo absoluto de `PayloadHashV1` entre ejecuciones y nodos:

* Strings: aplicar `UPPERCASE` usando locale estable (`Locale.ROOT` o equivalente).
* Números: normalizar con representación canónica estable (sin ceros no significativos y sin notación dependiente de locale).
* Timestamps: normalizar en zona horaria única (UTC) antes de serializar `YYYY-MM-DD HH:MM:SS`.
* Serialización: orden de campos fijo y delimitadores fijos por tipo de medida.

Cualquier cambio de estas reglas requiere incremento de `payload_hash_version`.

⸻

8. Estrategia de Persistencia

⸻

Input Normalization

Decisiones aprobadas:

* La normalización ocurre antes de calcular `payload_hash`.
* La normalización ocurre antes de cualquier `INSERT` o `UPDATE`.
* La normalización usada por `PayloadHashV1` se reutiliza por persistencia cuando aplica.
* La canonicalización de `source_family_key` y nombre de archivo ocurre antes de locking por familia y validación de revisiones.
* Para strings, aplicar `trim`; strings que quedan vacíos tras `trim` se normalizan como vacío canónico (equivalente a `NULL` en hash).
* Objetivo: evitar persistir representaciones distintas para valores semánticamente equivalentes.

Ejemplo:

abc
ABC

deben terminar normalizados de forma consistente.

Nota:

La especificación detallada de normalización por columna queda definida por tabla en su ADR técnico y contrato de persistencia.

⸻

Corto plazo

 Habilitar persistencia idempotente desde el día 1:
 
 * validación por clave de negocio + `revision` + `payload_hash`,
* clasificación explícita (`insert`, `update`, `noop`, `conflict`, `obsolete`),
* sin sobrescritura silenciosa.

`saveAll()` solo se permite si está encapsulado en un flujo que preserve la semántica idempotente anterior.

Comportamiento requerido para revisión parcial:

Una revisión superior con contenido parcial solo modifica claves presentes en el archivo; las claves no presentes permanecen sin cambios.

Regla de consistencia técnica:

En cada `UPDATE` aceptado, los metadatos técnicos del registro deben quedar alineados con la revisión efectivamente aplicada.

⸻

Mediano plazo

Migrar a:

Staging Tables
+
Merge Set-Based

⸻

Objetivo

Soportar:

100k+
500k+
1M+

registros por carga.

⸻

9. Particionado

⸻

Decisión

Diseñar las tablas para ser:

Partition Ready

desde el primer release.

Decisión explícita de despliegue:

* Fase 2: crear tablas `Partition Ready` sin activar particionado físico.
* Fase 5: activar particionado físico una vez estabilizado el modelo y validados índices/constraints.

Justificación:

Reduce riesgo de despliegue inicial y evita acoplar de forma prematura constraints únicos con reglas de particionado.

⸻

Estrategia propuesta

MySQL:

PARTITION BY RANGE (YEAR(fecha))

Regla crítica de MySQL (obligatoria):

Todo `UNIQUE KEY` en tabla particionada debe incluir la columna de particionado (o una expresión/columna derivada equivalente usada para particionar).

Implicación de diseño:

Antes de crear constraints únicos definitivos, debe validarse compatibilidad entre:

* clave de negocio,
* estrategia de particionado,
* y plan de evolución de índices.

Recomendación práctica:

Si se requiere, introducir una columna derivada persistida de año (por ejemplo `partition_year`) para simplificar compatibilidad entre `UNIQUE` y particionado.

Control de diseño previo a Fase 5:

No activar particionado físico sin validar DDL final de `UNIQUE` y plan de migración de índices en entorno de staging con datos representativos.

⸻

Ejemplo:

p2025
p2026
p2027
p2028

Convención sugerida:

`p####` donde `####` representa año calendario.

⸻

Beneficios

Backups

Permite separar años completos.

⸻

Archiving

Permite mover años completos.

⸻

Mantenimiento

Reduce impacto de:

OPTIMIZE TABLE
ANALYZE TABLE

⸻

Purga

Operaciones tipo:

DROP PARTITION

en segundos.

⸻

10. Estrategia de Backup

⸻

Legacy

Backup único posterior a migración.

Tablas:

medida_h_legacy
medida_qh_legacy
medida_cch_legacy

Estado:

Congeladas

⸻

Nuevas tablas

Diario

Backup completo.

⸻

Continuo

Binlog.

⸻

Objetivo

RPO:

≤ 15 minutos

RTO:

≤ 1 hora

Future enhancement (documentado, pendiente de definición):

Registrar eventos de backup/restore en una tabla de auditoría operativa (por ejemplo `backup_events`) con estado, timestamps, alcance y resultado.

Estado actual:

Se deja abierto para redefinición posterior de modelo y gobernanza.

⸻

11. Observabilidad

⸻

Métricas mínimas

Ingesta

files_received_count
revision_accepted_count
obsolete_rejected_count
revision_conflict_count
processing_started_count
processing_error_count

⸻

Persistencia

insert_count
update_count
noop_count
conflict_count

⸻

Concurrencia y estado

file_processing_lock_contention_count
family_lock_wait_ms
family_lock_timeout_count
family_state_drift_count

⸻

Rendimiento

batch_duration_ms
lock_wait_ms
retry_count

⸻

12. Fases de Ejecución

⸻

Fase 0 - ADR y Preparación

Entregables:

* ADR Source of Truth.
* ADR Legacy Archive + Fresh Start.
* ADR PayloadHashV1.
* ADR Control de Revisiones.
* ADR Nomenclatura de tablas `medida_*`.
* ADR Particionado + Constraints únicos en MySQL.
* ADR Business Key Definition por tabla.
* ADR FileRecord State Machine y transiciones.
* ADR Incremental Revision Processing (no-snapshot semantics).
* ADR Ordered Revision Processing (ordered delivery, no late reconciliation).
* ADR Family Revision State and Locking Mechanism (serialization, auditable state, multi-instance).

Resultado:

Decisiones arquitectónicas formalizadas.

⸻

Fase 1 - Infraestructura de Revisiones

Implementar:

* source_family_key
* revision
* processing_iteration
* mecanismo de estado y locking por familia
* gate de obsolescencia
* métricas

Resultado:

Control correcto de revisiones.

⸻

Arquitectura del Mecanismo de Locking y Estado por Familia

Objetivo:

Garantizar serialización de procesamiento de revisiones por familia en entorno multi-instancia.

Alcance explícito:

El mecanismo de Family Revision State and Locking no evita el procesamiento duplicado del mismo archivo.
Ese control ya existe mediante `FileRecord.status = PROCESSING` (bloqueo por archivo).

Este mecanismo coordina revisiones de archivos diferentes que pertenecen a la misma familia (bloqueo por familia).

Decisión de diseño:

Tabla persistente `file_revision_family_state` que mantiene el estado de control de revisiones completado por familia, no meramente un lock efímero.

Razones:

* auditoría de cuándo fue última revisión aceptada;
* performance: consultas O(1) en tabla caché en lugar de MAX() sobre archivo histórico;
* debugging: estado visible en producción;
* escalabilidad: tabla pequeña, índice PK, menor contention en lock.

Modelo de datos:

```sql
CREATE TABLE file_revision_family_state (
    source_family_key VARCHAR(64) PRIMARY KEY,
    
    last_succeeded_revision INT NOT NULL DEFAULT -1,
    last_file_record_id BIGINT NULL,
    
    created_on DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_on DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
        ON UPDATE CURRENT_TIMESTAMP(3),
    
    INDEX idx_updated_on (updated_on),
    CHECK (last_succeeded_revision >= -1)
) ENGINE=InnoDB
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci
  ROW_FORMAT=DYNAMIC;
```

Semántica:

* `last_succeeded_revision`: revisión más alta aceptada exitosamente para la familia.
* `last_file_record_id`: identificador informativo para trazabilidad y debugging operativo.
* `updated_on`: timestamp de última actualización de estado (auditoría de concurrencia).

Convención de inicialización:

* `last_succeeded_revision = -1` significa "familia sin revisiones aceptadas".
 * Dado que `revision >= 0`, la primera revisión válida (`0`) se procesa como nueva y no como duplicada.

No existe integridad referencial obligatoria entre `file_revision_family_state` y `file_records`.

Flujo transaccional:

Para cada archivo con `source_family_key`:

```
TX1 - Gate y reserva de ejecución (lock corto por familia)

BEGIN TRANSACTION
│
├─ INSERT IGNORE INTO file_revision_family_state
│    (source_family_key, last_succeeded_revision, last_file_record_id, created_on, updated_on)
│    VALUES (?, -1, NULL, NOW(), NOW())
│
├─ SELECT * FROM file_revision_family_state
│  WHERE source_family_key = ? FOR UPDATE
│
├─ Verificar si ya existe PROCESSING para la misma familia
│  (consulta sobre file_records por source_family_key + status=PROCESSING)
│
├─ si existe PROCESSING activo:
│    COMMIT
│    reintentar (backoff)
│
├─ Evaluación de revisión:
│    last_succeeded := file_revision_family_state.last_succeeded_revision
│
│    si incoming_revision < last_succeeded:
│    │  INSERT file_record (status=REJECTED, reason=OBSOLETE_REVISION)
│    │  COMMIT
│    │
│    elif incoming_revision == last_succeeded:
│    │  comparar hash de archivo
│    │  ├─ si coincide: INSERT file_record (status=DUPLICATED_CONTENT)
│    │  └─ si difiere: INSERT file_record (status=REVISION_CONFLICT)
│    │  COMMIT
│    │
│    else (incoming_revision > last_succeeded):
│       INSERT file_record (status=PROCESSING)
│       COMMIT

TX2 - Procesamiento de medidas (sin lock de familia)

Comportamiento de TX2 - Procesamiento tolerante a fallos por registro:

Los errores de validación o persistencia sobre registros individuales NO provocan el rechazo completo del archivo.

El sistema aplica el algoritmo de Binary Split existente en `JpaMeasurePersistenceAdapter`:

1. Procesar batch completo con `saveAll`.
2. Si el batch falla, dividir recursivamente por mitad hasta aislar el registro culpable.
3. Excluir el registro fallido del batch.
4. Continuar procesando el resto del contenido válido.
5. Acumular registros fallidos en `BatchSplitContext` para exportación.

Resultado de TX2:

* registros válidos -> persistidos en base de datos.
* registros inválidos/problemáticos -> aislados y exportados para corrección posterior.
* el archivo puede finalizar `SUCCEEDED` aunque existan registros individuales rechazados.

`PROCESSING_ERROR` queda reservado exclusivamente para:

* fallos de infraestructura (base de datos inaccesible, timeout irrecuperable);
* errores que impidan completar el procesamiento del archivo entero;
* excepciones no recuperables que abortan TX2 completamente.

procesar archivo en batches con binary split

TX3 - Finalización y actualización de estado (lock corto por familia)

BEGIN TRANSACTION
│
├─ SELECT * FROM file_revision_family_state
│  WHERE source_family_key = ? FOR UPDATE
│
├─ si procesamiento exitoso:
│    UPDATE file_revision_family_state
│      SET last_succeeded_revision = ?,
│          last_file_record_id = ?,
│          updated_on = NOW()
│    WHERE source_family_key = ?
│      AND last_succeeded_revision < ?
│
│    si row_count = 1:
│       UPDATE file_records SET status=SUCCEEDED WHERE id=?
│    si row_count = 0:
│       UPDATE file_records
│       SET status=REJECTED,
│           reason=OBSOLETE_REVISION
│       WHERE id=?
│
├─ si procesamiento falla:
│    UPDATE file_records
│    SET status=REJECTED,
│        reason=PROCESSING_ERROR
│    WHERE id=?
│
└─ COMMIT
```

Riesgo identificado y corrección aplicada:

* Mantener `SELECT ... FOR UPDATE` durante todo el procesamiento de archivos grandes (150k, ~21s) genera lock prolongado y contention.
* Corrección: lock corto en TX1/TX3 y procesamiento batch fuera del lock.
* Se preservan orden estricto por familia, idempotencia y comportamiento multi-instancia.

Garantía anti-regresión de estado:

`last_succeeded_revision` nunca retrocede.

Se garantiza por condición de actualización:

`WHERE last_succeeded_revision < incoming_revision`

Si la condición falla (`row_count = 0`), el archivo en finalización se marca `REJECTED + OBSOLETE_REVISION`.

Garantías de concurrencia multi-instancia:

* `FileRecord.status=PROCESSING` evita procesamiento concurrente del mismo archivo.
* `SELECT ... FOR UPDATE` sobre PK de `file_revision_family_state` serializa todas las operaciones por familia.
* Dos instancias simultáneas intentando actualizar misma familia: la primera gana, la segunda obtiene estado actualizado y toma decisión coherente.
* Política de timeouts: si el lock no se adquiere en 2 segundos, reintentar con backoff exponencial (100ms, 250ms, 500ms, fallar).
* Deadlocks: improbables porque siempre se accede por familias en orden (PK string normalizado).

Validación de consistencia (post-operación):

Para evitar drift entre `file_revision_family_state` y `file_records`, agregar validación periódica:

```sql
SELECT source_family_key, last_succeeded_revision,
       (SELECT COALESCE(MAX(revision), -1)
        FROM file_records fr
        WHERE fr.source_family_key = frfs.source_family_key
          AND fr.status IN ('SUCCEEDED')) AS actual_max_rev
FROM file_revision_family_state frfs
WHERE last_succeeded_revision != actual_max_rev;
```

Nota de evolución:

La validación de drift debe considerar únicamente estados terminales que actualizan `file_revision_family_state`.
Si en el futuro se agregan estados terminales equivalentes a éxito, deben incorporarse explícitamente en esta consulta.

Si hay resultados: escalable a equipo de operaciones como inconsistencia.

Recovery de `PROCESSING` huérfano (obligatorio):

Definir un job operativo periódico para detectar `file_records` en `PROCESSING` sin progreso dentro de una ventana de seguridad configurable.

Regla mínima:

* si `status = PROCESSING` y `updated_on` supera umbral de stale:
  * mover a `REJECTED` con `reason = PROCESSING_ERROR` y comentario de recovery,
  * habilitar reproceso idempotente del mismo archivo,
  * registrar evento/auditoría de recuperación.

Objetivo:

Evitar bloqueo indefinido de familia/archivo tras crash entre TX2 y TX3 y garantizar convergencia por reproceso.

Observabilidad:

Métricas adicionales:

* `family_state_table_rows`: cardinalidad de familias en seguimiento.
* `family_lock_wait_ms`: latencia de adquisición de lock por familia.
* `family_lock_timeout_count`: reintentos fallidos por timeout.
* `family_state_drift_count`: inconsistencias detectadas en validación periódica.

⸻

Fase 2 - Nuevo Modelo de Datos

Crear:

medida_h
medida_qh
medida_cch

con:

* constraints
* índices
* metadata técnica
* reglas de idempotencia activas desde el primer despliegue
* sin particionado físico aún (solo `Partition Ready`)

Resultado:

Nuevo modelo operativo listo.

⸻

Fase 3 - Corte Productivo

Ejecutar:

rename legacy
↓
crear tablas nuevas
↓
activar ingestión

Resultado:

Producción operando sobre modelo limpio.

⸻

Fase 4 - Recarga Piloto

Reprocesar:

1 mes

de archivos históricos.

Validar:

* rendimiento
* consistencia
* tiempos
* procesamiento en orden ascendente de revisiones por familia (prerequisito de recarga)

Resultado:

Proceso de reconstrucción validado.

⸻

Fase 5 - Optimización

Evaluar:

* staging tables
* merge set-based
* activación de particionado físico
* archivado automático

Resultado:

Escalabilidad de largo plazo.

⸻

13. Revisión Arquitectónica Crítica (Pre-GoLive)

Objetivo:

Verificar inconsistencias, riesgos ocultos, decisiones incompletas y mejoras necesarias antes de iniciar implementación.

Resultado de revisión (estado actual):

1) Supuesto de snapshot completo

* Corregido en este documento: el procesamiento de revisiones es incremental por clave.
* Bloqueo de aprobación si algún componente interpreta revisión superior como reemplazo total.

2) Cobertura de estrategia incremental

* Reflejada en constraints, persistencia e idempotencia.
* Procesamiento ordenado incorporado como principio y regla operativa.
* Debe verificarse en pruebas de replay histórico con archivos procesados en orden ascendente.

3) Concurrencia por archivo vs por familia

* El procesamiento concurrente del mismo archivo ya está protegido por `FileRecord.status = PROCESSING`.
* El mecanismo de `file_revision_family_state` resuelve coordinación entre archivos distintos de la misma familia.
* Ambos mecanismos son complementarios y no se sustituyen.

4) ADR específico

* Obligatorio: `ADR Incremental Revision Processing`.
* Obligatorio: `ADR Ordered Revision Processing`.

5) `payload_hash`

* `PayloadHashV1` queda definido con canonicalización aprobada (strings, numéricos y timestamps).
* Cualquier cambio futuro de canonicalización debe incrementar `payload_hash_version` y generar ADR específico.

6) Campos técnicos

* Base actualizada: `revision` (SMALLINT UNSIGNED), `processing_iteration` (SMALLINT UNSIGNED), `id_file_record` (BIGINT), `source_family_key` (VARCHAR(64)), `payload_hash` (CHAR(64)), `payload_hash_version`.
* Mejora recomendada: evaluar `applied_at`, `conflict_reason`, `quarantine_ref` en fases posteriores.

7) Riesgo de corrupción/inconsistencia

Riesgos principales:

* entrega fuera de orden por parte del cliente;
* canonicalización incorrecta/inestable de `PayloadHashV1`;
* inconsistencia entre `file_revision_family_state` y `file_records`;
* lock contention excesivo sobre familias de alta actividad.

Mitigación obligatoria:

* pruebas de propiedad idempotente,
* pruebas de rechazo de revisión tardía (OBSOLETE_REVISION),
* validación de no-delete por ausencia,
* monitoreo de drift de estado y lock contention.

8) Comportamiento para revisión superior parcial

* Debe ser estrictamente "upsert por clave presente".
* Nunca ejecutar borrado implícito por diferencias de cardinalidad entre revisiones.

9) Edge-cases obligatorios en testing

* llegada fuera de orden: `.4` antes que `.3` -> debe rechazarse como OBSOLETE_REVISION;
* `F5D_001.0` vs `f5d_001.0` -> mismo `source_family_key` canónico y misma validación de revisión;
* misma revisión con payload distinto (conflicto);
* revisiones con superposición parcial de claves;
* replay de archivo idéntico (idempotencia fuerte);
* mezcla de archivos históricos y nuevos en misma ventana de ejecución;
* recarga histórica en orden incorrecto -> debe fallar con error claro, no corromper estado.
* finalización concurrente de revisiones (p.ej. rev5 termina antes que rev4) -> `last_succeeded_revision` no puede retroceder.
* familia nueva con llegada concurrente -> `INSERT IGNORE` + `SELECT FOR UPDATE` sin duplicate key fatal.
* crash entre TX2 y TX3 -> `PROCESSING` huérfano recuperado a `REJECTED + PROCESSING_ERROR` y reproceso convergente.

Casos obligatorios adicionales de hashing/normalización:

* `ABC` vs `abc` => mismo hash.
* `123.4` vs `123.40` => mismo hash.
* `2025-01-01` => error de validación.
* `2025-01-01 10:30:00` => válido.
* `NULL` vs `""` => mismo hash canónico.
* `""` vs `" "` => mismo hash canónico.
* `""` vs `"\t"` => mismo hash canónico.
* `""` vs `"\n"` => mismo hash canónico.
* mismo payload en distintos hosts/timezones/locales => mismo hash.
* registro con minúsculas => persistencia normalizada.
* registro ya normalizado => mismo resultado.

10) Estándares y prácticas recomendadas

* ADRs obligatorios antes de corte productivo;
* contratos de invariantes de dominio ("no delete por ausencia");
* pruebas de regresión con datasets de replay realistas;
* observabilidad por resultado de upsert (`insert/update/noop/obsolete/conflict`).

11) Cambios concretos requeridos antes de aprobar ejecución

* Formalizar y aprobar `ADR Incremental Revision Processing`.
* Formalizar y aprobar `ADR Ordered Revision Processing`.
* Formalizar y aprobar `ADR Family Revision State and Locking Mechanism`.
* Especificar política de timeouts exacta en FamilyRevisionLockService.
* Agregar casos de prueba de revisión parcial y no-delete implícito.
* Agregar casos de prueba de rechazo OBSOLETE_REVISION.
* Agregar tests de concurrencia multi-instancia (dos instancias, misma familia).
* Agregar tests de exclusión mutua por archivo (`PROCESSING`) y por familia (`SELECT ... FOR UPDATE`).
* Agregar tests obligatorios de canonicalización y normalización de `PayloadHashV1`.
* Agregar tests de no-regresión de `last_succeeded_revision` en finalización concurrente.
* Publicar runbook de conflictos (`REVISION_CONFLICT` / `QUARANTINED`) con SLA de resolución.
* Verificar propagación de `source_family_key` en índices y queries del gate de obsolescencia.
* Validación de drift entre `file_revision_family_state` y `file_records` (query de auditoría).

⸻

14. ADR - Ordered Revision Processing

ID: ADR-Ordered-Revision-Processing
Estado: Aprobado
Fecha: Junio 2026

Decision:

Las revisiones de una misma familia de archivos deben procesarse en orden ascendente estricto.

Una revisión con número inferior al máximo aceptado para la familia es rechazada con estado REJECTED y motivo OBSOLETE_REVISION.

Context:

* Las revisiones son incrementales, no snapshots completos.
* Procesar revisiones fuera de orden puede producir estados intermedios incorrectos.
* El cliente garantiza la entrega de archivos en orden ascendente.
* El comportamiento determinístico reduce complejidad operativa y de auditoría.

Consequences:

Positivas:

* comportamiento predecible y trazable;
* eliminación de lógica de reconciliación de revisiones tardías;
* reducción de riesgo de corrupción por precedencias dinámicas.

Negativas:

* una revisión entregada fuera de orden por error operativo requiere intervención manual;
* no existe recuperación automática ante entrega tardía.

Rejected Alternatives:

* Reconciliación dinámica: descartada por complejidad y riesgo de inconsistencia.
* Aceptar revisiones tardías con merge: descartada por ambigüedad semántica y riesgo de regresión.

⸻

15. Dictamen Final de Preparación

A) Lista de cambios aplicados en esta revisión

* `source_family_key VARCHAR(64) NOT NULL` agregado al modelo técnico de todas las tablas.
* `source_family_key` excluido de `payload_hash` (es metadato técnico, no contenido funcional).
* Semántica formal de `UPDATE` redactada sin ambigüedad (corrección incremental, campos técnicos actualizados siempre).
* Principio #5 de procesamiento ordenado incorporado como principio arquitectónico.
* Regla de procesamiento ordenado (OBSOLETE_REVISION) añadida en Control de Revisiones.
* Estado `PROCESSING` documentado como estado oficial y transición obligatoria.
* `PROCESSING_ERROR` formalizado como `REJECTED + reason=PROCESSING_ERROR`.
* Restricción de orden en recarga histórica documentada en Paso 4.
* `ADR Ordered Revision Processing` formalizado.
* Fase 4 (recarga piloto) actualizada con prerequisito de orden.
* Edge-cases de revisión tardía y recarga desordenada añadidos en testing obligatorio.
* Separación explícita de responsabilidades: bloqueo por archivo (`PROCESSING`) y serialización por familia (`FOR UPDATE`).
* Foreign Key removida de `file_revision_family_state` por ser tabla operacional/auditoría.
* Sección de revisión crítica actualizada con nuevos riesgos y cambios.

B) Riesgos residuales

* Error operativo de entrega fuera de orden: requiere intervención manual, no hay recuperación automática.
* Implementación defectuosa de la canonicalización aprobada en `PayloadHashV1`.
* Inconsistencia entre `file_revision_family_state` y `file_records` si falla disciplina transaccional.
* Lock contention elevado en familias de alta actividad.
* `source_family_key VARCHAR(64)`: validar contra nombres de familia más largos conocidos.

C) Validación final de preparación

✅ Source of Truth formalizado (archivos).
✅ Semántica incremental de revisiones documentada y propagada.
✅ Procesamiento ordenado como principio, regla y ADR.
✅ Semántica de UPDATE formal: sin ambigüedad entre reemplazo total y corrección incremental.
✅ No-delete implícito documentado y protegido en reglas de persistencia.
✅ `source_family_key` (VARCHAR(64)) en modelo técnico, en exclusión de hash, y con justificación.
✅ Campos técnicos completos para trazabilidad y auditoría.
✅ Estados y transiciones de FileRecord formalizados.
✅ Constraints únicos viables desde inicio (tablas vacías).
✅ ADRs obligatorios identificados; ADR Ordered Revision Processing formalizado.
✅ Mecanismo de locking y estado por familia formalizado; ADR Family Revision State and Locking Mechanism documentado.
✅ Flujo transaccional de serialización por `SELECT ... FOR UPDATE` definido.
✅ Política de timeouts y retry en lock documentada.
✅ Validación de consistencia entre state y file_records especificada.
✅ Estado `PROCESSING` documentado como protección de concurrencia a nivel archivo.
✅ Separación clara de responsabilidades: bloqueo por archivo (`PROCESSING`) y bloqueo por familia (`FOR UPDATE`).
✅ `file_revision_family_state` definido como tabla operacional y de auditoría sin Foreign Keys.

Pendientes abiertos:

⚠️  Runbook de conflictos y cuarentena con SLA pendiente (pre-GoLive).

D) Dictamen final

READY FOR IMPLEMENTATION

Justificación:

El documento está en condiciones de iniciar Fase 0 (ADRs y preparación) de forma inmediata.

Requisitos de cierre operativo previos a GoLive (no bloquean inicio de implementación):

1. Runbook operativo de conflictos y cuarentena con SLA definido.

Ningún principio fundamental, decisión de negocio ni regla operativa crítica permanece sin resolver.

⸻

17. Nuevos Hallazgos de Revisión Arquitectónica

CRÍTICO - Lock de familia prolongado durante procesamiento completo

Estado: APLICADO

Problema:

El flujo original mantenía lock por familia mientras se procesaban archivos grandes (~21s), degradando throughput y elevando contention.

Riesgo:

Bloqueo innecesario de familias activas y regresión de rendimiento multi-instancia.

Acción aplicada:

Se adoptó flujo en 3 transacciones (TX1 gate, TX2 procesamiento, TX3 finalización), manteniendo lock corto y garantías de orden.

IMPORTANTE - Posible regresión de `last_succeeded_revision` en finalización concurrente

Estado: APLICADO

Problema:

Sin condición de monotonicidad, una revisión menor podría sobrescribir estado de una mayor si finaliza después.

Riesgo:

Corrupción de estado de familia y decisiones de obsolescencia incorrectas.

Acción aplicada:

Actualización del state con guardia `WHERE last_succeeded_revision < incoming_revision`; si falla, el archivo finaliza como `REJECTED + OBSOLETE_REVISION`.

IMPORTANTE - Ambigüedad de canonicalización de `source_family_key`

Estado: APLICADO

Problema:

Sin canonicalización explícita, variantes de mayúsculas/minúsculas generan familias distintas.

Riesgo:

Locks, consultas e índices inconsistentes por la misma familia lógica.

Acción aplicada:

Se formalizó canonicalización `UPPERCASE` previa a comparación, locking, validación y persistencia.

MENOR - Deriva futura de query de drift por ampliación de estados terminales

Estado: APLICADO

Problema:

La query puede quedar incompleta si aparecen nuevos estados de éxito.

Riesgo:

Falsos positivos/negativos en auditoría de drift.

Acción aplicada:

Se documentó explícitamente que la consulta debe incluir únicamente estados terminales que actualizan `file_revision_family_state`.

MENOR - Propuestas cosméticas/optimización prematura sin impacto de integridad

Estado: NO APLICADO

Justificación:

No corrigen inconsistencias reales ni reducen riesgos de concurrencia/idempotencia/integridad, por lo que se excluyen de esta revisión.

⸻

18. ADR - Family Revision State and Locking Mechanism

ID: ADR-Family-Revision-State-Locking
Estado: Propuesto
Fecha: Junio 2026

Decision:

Crear tabla persistente `file_revision_family_state` que mantenga estado de control de revisiones por familia.

Utilizar `SELECT ... FOR UPDATE` sobre esta tabla como mecanismo de serialización para multi-instancia.

Aplicar lock por familia en ventanas cortas (TX1 gate, TX3 finalización) y ejecutar procesamiento batch fuera del lock.

No implementar locks efímeros; en su lugar, mantener estado auditable y observable.

Context:

* Múltiples instancias de `c4e-ingestion-service` pueden procesar archivos simultáneamente.
* El modelo ya dispone de exclusión por archivo mediante transición `PENDING -> PROCESSING` en `FileRecord`.
* Las revisiones de una familia deben procesarse **obligatoriamente en orden ascendente**.
* Una revisión fuera de orden (< última aceptada) debe rechazarse como OBSOLETE_REVISION.
* El sistema necesita trazabilidad de cuál fue la última revisión aceptada.
* Performance: evitar consultas N+1 sobre `file_records` para determinar `MAX(source_revision)`.

Consequences:

Positivas:

* serialización estricta por familia sin locks sofisticados;
* auditoría persistente (`updated_on`);
* performance O(1) en gate de obsolescencia;
* observabilidad: tabla pequeña, visible en producción para debugging;
* escalabilidad multi-instancia garantizada;
* estado coherente en transacciones de gate/finalización;
* separación explícita de responsabilidades: `PROCESSING` bloquea por archivo y `FOR UPDATE` serializa por familia.

Negativas:

* tabla adicional en schema;
* riesgo de drift si UPDATE de state se ejecuta fuera de transacción (mitigado por diseño);
* timeout en lock genera reintentos (mitigado por policy explícita).

Rejected Alternatives:

1. **Solo locks efímeros (SELECT ... FOR UPDATE en file_records directamente):**
   - Contras: tabla grande, índice complejo; N+1 queries para MAX(); difícil debug posterior.

2. **Redis/Memcached para state:**
   - Contras: adiciona dependencia externa; cache decay en fallos; complejidad de consistencia.

3. **Optimistic locking con version column:**
   - Contras: retry loop indefinido; no garantiza orden entre instancias.

Trade-offs aceptados:

* Mantener estado persistente en lugar de puramente transaccional: **justificado por auditoría y observabilidad**.
* Tabla separada vs columnas en file_records: **justificado por performance (tabla small) y separación de concerns**.

Implementation Notes:

* Usar `DATETIME(3)` para precisión milisegundos.
* `last_file_record_id` es informativo para trazabilidad y debugging operativo.
* No usar Foreign Keys en `file_revision_family_state`.
* Validación periódica de deriva entre state y file_records.
* Política explícita: lock timeout = 2s, retries = 3, backoff exponencial.
* Garantía anti-regresión: actualizar `last_succeeded_revision` con condición `last_succeeded_revision < incoming_revision`.
* Si la condición no se cumple en finalización, marcar file_record como `REJECTED + OBSOLETE_REVISION`.

⸻

Estado Final

Al concluir este plan:

✅ Revisiones controladas correctamente.

✅ Constraints únicos activos desde el inicio.

✅ Legacy preservado sin afectar operación.

✅ Base reconstruible desde archivos.

✅ Source of Truth formalizado.

✅ Preparado para recargas históricas.

✅ Preparado para crecimiento multi-anual.

✅ Arquitectura alineada con prácticas modernas de ingestión de datos y corrección incremental.

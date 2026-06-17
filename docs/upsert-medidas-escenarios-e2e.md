# Procesamiento de medidas — Escenarios E2E

Documento de validación del flujo de ingesta y **upsert con detección de cambios y control de
revisiones** del servicio `c4e-ingestion-service`. Pensado para mostrar al cliente los escenarios
cubiertos y el comportamiento esperado en producción.

## Beneficios de la implementación

- **Reenvío del archivo completo, sin marcar qué cambió**: el cliente reenvía la revisión corregida
  entera y el sistema decide fila por fila qué insertar, corregir u omitir. No hay que indicar deltas.
- **Reproceso en segundos, no minutos**: la detección de cambios por `payload_hash` evita reescribir
  lo que no cambió (un reproceso casi-idéntico baja de ~8s a ~2s; solo se escriben las filas reales).
- **Idempotente**: reprocesar un archivo idéntico = **0 escrituras** (no duplica ni reescribe).
- **No se pierden datos buenos por una fila mala**: tres capas tolerantes (parseo, validación, BD)
  aíslan y reportan la fila problemática y **persisten el resto** (`PARTIAL_SUCCEEDED`), con commit
  parcial real. Cada defecto queda trazado en su reporte (`.jsonl`) y su evento al outbox.
- **Integridad temporal e independencia del orden**: la precedencia revisión/iteración se resuelve
  **por fila**, así que los archivos se pueden procesar en **cualquier orden** y un archivo viejo
  (incluso si llega tarde) **nunca pisa datos más nuevos**; las correcciones se aplican in-place
  conservando el `id` de la medida. Un archivo totalmente obsoleto queda como `SUPERSEDED` (no-op).
- **Protección entre familias**: si un archivo intentara escribir sobre `(cliente, fecha)` de otra
  familia, se rechaza **antes de tocar la BD** (cero escrituras, BD intacta).
- **Observabilidad**: cada archivo emite una línea de evento que **reconcilia el total** del archivo
  con una sola fórmula (ver más abajo), apta para monitoreo/alertas.
- **Costo de almacenamiento controlado**: tablas dimensionadas al mínimo (`payload_hash` BINARY(8),
  columnas right-sized, sin auditoría redundante) + particionado anual por fecha para respaldos y
  retención — reduce drásticamente el tamaño frente al esquema legado (~30 GB/año).
- **Eficiencia de escritura**: resolución de clientes en **una sola consulta** por archivo (sin N+1),
  ids generados en la app (TSID) para habilitar **JDBC batching**, escritura por lotes.

## Conceptos

- **Familia de archivos** (`source_family_key`): el nombre del archivo sin extensión de versión.
  Ej. `P1D_0031_0894_20260502.3` y `P1D_0031_0894_20260502.3.1` son la misma familia.
- **Revisión / iteración**: las extensiones numéricas del archivo (`.{revision}` y `.{revision}.{iteration}`).
  Gana siempre la `(revisión, iteración)` **más alta**. Una nueva revisión reinicia la iteración.
  La precedencia se resuelve **POR FILA** (no rechazando el archivo): cada `(cliente, fecha)` conserva
  el dato de la revisión/iteración más alta vista. Por eso los archivos pueden procesarse en
  **cualquier orden** y un archivo viejo que llega tarde **no pisa** datos más nuevos (sus filas
  obsoletas se omiten; sus filas únicas sí se insertan).
- **Clave de negocio**: `(id_cliente, fecha)`. Identifica unívocamente una medida.
- **Detección de cambios (`payload_hash`)**: por cada medida se guarda un hash de su contenido.
  Al reprocesar, si el hash no cambió → se **omite** (no se reescribe); si cambió → se **actualiza
  en su lugar** (mismo registro).
- **Redondeo (techo)**: las magnitudes horarias (P1) con decimales se guardan redondeadas hacia
  arriba al entero siguiente (`7.001 → 8`, `7.999 → 8`), por especificación del cliente.

## Archivo mixto: correcciones + altas + sin cambios, en una sola pasada

Un mismo archivo (típicamente una revisión de corrección reenviada **completa**) puede contener
cualquier mezcla, y el sistema la resuelve fila por fila **sin que haya que marcar qué cambió**:

- `(cliente, fecha)` que no existe → **INSERT** (alta).
- `(cliente, fecha)` que existe con el mismo contenido → **SKIP** (no se reescribe).
- `(cliente, fecha)` que existe con contenido distinto → **UPDATE in-place** (corrección).

Ejemplo: un archivo de 140,952 filas con 140,900 idénticas, 50 corregidas y 2 nuevas →
`persisted=2, updated=50, skipped=140,900`, en una sola pasada y en segundos (solo 52 escrituras).
El cliente manda el archivo corregido completo; el sistema aplica únicamente los cambios reales.

## Filtros de subida (antes de procesar)

En orden, un archivo se rechaza en la subida si:
1. **Nombre duplicado** → `DUPLICATED_ORIGINAL_FILENAME`.
2. **Contenido duplicado** (mismo hash de archivo, aunque cambie el nombre) → `DUPLICATED_CONTENT`.

> Nota: ya **no** se rechaza por "revisión superada" a nivel archivo. Una revisión inferior se procesa
> y la precedencia se resuelve por fila (sus filas obsoletas se omiten como `skipped-obsoleto`).

## Escenarios validados (BD real, dev)

| # | Entrada | Resultado | Notas |
|---|---|---|---|
| 1 | Carga inicial `.3` (BD fresca, 140,952 filas) | `persisted=140,952, updated=0, skipped=0` | Todo INSERT. Techo aplicado (`7.957 → 8`), `payload_hash` BINARY(8). ~8.2s. |
| 2 | Reprocesar el mismo `.3` (mismo nombre y contenido) | Rechazado en subida: `DUPLICATED_ORIGINAL_FILENAME` | No llega a procesarse. |
| 3 | `.3.1` con **mismo contenido**, nombre nuevo | Rechazado en subida: `DUPLICATED_CONTENT` | El hash del archivo es idéntico → no hay nada que procesar. |
| 4 | `.3.1` con **1 valor modificado** en una `(cliente,fecha)` existente | `persisted=0, updated=1, skipped=140,951` | UPDATE in-place (mismo `id_medida_h`). 140,951 omitidas → **~2s** (vs 8s). `COUNT(*)` no cambia. |
| 5 | `.3.2` cambiando la **fecha** de una línea (2026→2027) | `persisted=1, updated=1, skipped=140,951` | La línea movida a 2027 = clave nueva → **INSERT**; la fecha vieja vuelve a su valor original → **UPDATE**. `COUNT(*)` sube en 1. Editar la identidad (cliente/fecha) crea fila nueva y deja la vieja. |
| 6 | Subir a **revisión `.4`** teniendo aplicado hasta `(3,3)` | Aceptado; `file_records`: `revision=4, processing_iteration=0`. Upsert: `persisted=1, skipped=140,952` | **La revisión mayor gana aunque su iteración (0) sea menor que la última (3)**: la revisión domina sobre la iteración, y la iteración se reinicia a 0 con la nueva revisión. |
| 7 | `.4.1` (bump de iteración dentro de la misma revisión) | Aceptado; `file_records`: `revision=4, processing_iteration=1`. Upsert aplica los cambios. | Iteración mayor dentro de la misma revisión gana. |
| 8 | `.4.3` teniendo aplicado `(4,4)` | **Procesado, sin escrituras netas**: cada `(cliente,fecha)` ya existe de `(4,4)` ≥ `(4,3)` → todo `skipped-obsoleto`; `business_result=SUPERSEDED`, BD intacta. | Revisión/iteración **menor** ya no se rechaza: se procesa y la precedencia por fila omite lo obsoleto sin pisar datos nuevos. **(comportamiento nuevo, pendiente de re-validar E2E)** |

| 9 | `.6` con **1 fila válida modificada + 1 fila con `actent` fuera de rango** (`INT UNSIGNED`) | `updated=1, defects=1, skipped=140,951`; `PARTIAL_SUCCEEDED`, va a `/processed`. La fila mala se aísla a `.sge_quarantine.jsonl` + evento `FILE_PERSISTENCE_QUARANTINE`. | **Commit parcial real validado**: en el mismo lote que falló, la fila buena se guardó y la mala se aisló (binary-split + `REQUIRES_NEW`). Es la pieza que ningún test unitario podía cubrir. |
| 10 | `.7` con **1 fila ingarseable (`actent=abc`) + 1 fila `actent` fuera de rango + 1 cambio válido** | `updated=1, defects=2, skipped=140,950`; `PARTIAL_SUCCEEDED`. La de `abc` → `.sge_defect.jsonl` (`phase=parse`), la de overflow → `.sge_quarantine.jsonl`. | **Parseo tolerante validado** + las 3 capas conviviendo: una fila mala (de cualquier tipo) ya no tumba el archivo; se persiste el resto. |
| 11 | **Alta inicial (BD fresca)** del `.3` con **1 fila ingarseable (`abc`) + 1 fila `actent` fuera de rango** al final | `total=140,954, persisted=140,952, updated=0, skipped=0, parseDefects=1, quarantined=1`; `PARTIAL_SUCCEEDED`. ~8.3s. | **Binary-split en el camino INSERT validado**: las 140,952 buenas insertan, la de overflow se aísla a cuarentena (búsqueda binaria converge a 1 fila) y la de `abc` a `.sge_defect.jsonl`. Destapó y validó el fix del bug de reintento (`StaleObjectStateException` por reuso de instancias con id ya asignado). |
| 12 | **Colisión cross-familia**: archivo de OTRA familia (`...20260503`, sin previo aplicado) con `(cliente,fecha)` ya cargadas por la familia `...20260502` | **Rechazado**: `CROSS_FAMILY_COLLISION` → `/rejected`, **BD intacta** (0 escrituras), evento `FILE_REJECTED`. ~88ms. | **Pre-check antes de escribir validado**: el prefetch detecta `(cliente,fecha)` existentes + familia sin previo aplicado → rechaza sin tocar la BD (sin binary-split ni cuarentena). `uk_business` es global; esto protege contra que una familia pise datos de otra. |

## Escenarios pendientes de validar (precedencia por fila — comportamiento nuevo)

| # | Entrada | Resultado esperado |
|---|---|---|
| 13 | En un mismo lote, `.0` y `.1` de la misma familia procesados en **orden inverso** (`.1` antes que `.0`) | Estado final idéntico al orden natural: las `(cliente,fecha)` de `.1` ganan; las únicas de `.0` se insertan. Independiente del orden. |
| 14 | Rezagado: llega `.0` cuando `.1` ya está aplicado | Sin pérdida: filas que chocan con `.1` → `skipped-obsoleto`; filas únicas de `.0` → INSERT. BD nunca pierde datos. |
| 15 | Archivo totalmente obsoleto (todas las filas ya existen de revisión ≥) | `business_result=SUPERSEDED`, `quality_status=CLEAN`, 0 escrituras netas. |

## Línea de evento por archivo (`measure_file_processed`)

Cada archivo emite un resumen operativo. `total` = **todas las filas de medida que llegaron** en el
archivo (las que parsearon OK + las que fallaron al parsear). Los defectos se desglosan por capa, de
modo que **una sola invariante reconcilia el archivo completo**:

```
total = persisted + updated + skippedIdentical + skippedStale + parseDefects + validationDefects + quarantined
```

| Campo | Significado |
|---|---|
| `total` | Filas de medida en el archivo (parsearon OK + fallaron al parsear) |
| `persisted` / `updated` | Insertadas / corregidas in-place |
| `skippedIdentical` | Omitidas porque el contenido era idéntico (mismo `payload_hash`) |
| `skippedStale` | Omitidas porque ya existía una revisión/iteración igual o más reciente (obsoletas) |
| `skipped` | Total de omitidas (`skippedIdentical + skippedStale`) |
| `parseDefects` | Líneas que no se pudieron parsear → `.sge_defect.jsonl` (`phase=parse`) |
| `validationDefects` | Parsearon pero fallaron validación de negocio (CUPS, rangos…) → `.sge_defect.jsonl` (`phase=validation`) |
| `quarantined` | Parsearon y validaron OK pero rompieron una constraint de BD → `.sge_quarantine.jsonl` |
| `defects` | Agregado (= `parseDefects + validationDefects + quarantined`), se mantiene por compatibilidad |

Ejemplo (escenario #11): `total=140,954 persisted=140,952 updated=0 skipped=0 parseDefects=1
validationDefects=0 quarantined=1` → `140,952 + 1 + 1 = 140,954`. ✓

## Capas de error (semánticas distintas)

Las **tres capas son tolerantes**: una línea defectuosa nunca tumba el archivo; se aísla/reporta y el resto se persiste. Solo se rechaza el archivo completo si NINGUNA línea es procesable.

| Capa | Cuándo | Comportamiento |
|---|---|---|
| **Parseo** | valor imposible de convertir, columnas faltantes | **Tolerante**: omite la línea mala, la reporta en `.sge_defect.jsonl` (`phase=parse`) y persiste el resto. Solo si NINGUNA línea parsea → archivo rechazado (`INVALID_FILE_FORMAT`). |
| **Validación** (TOLERANT) | reglas de negocio (CUPS, rangos, no-negativos) | Reporta la fila como defecto, persiste las buenas. |
| **Binary-split** (BD) | parsea y valida OK, pero rompe una constraint de BD | Aísla la fila a cuarentena (`.sge_quarantine.jsonl`), persiste las buenas (commit parcial por `REQUIRES_NEW`). |

## Hallazgos corregidos vía E2E

- **Parseo tolerante (implementado)**: antes un solo error de parseo rechazaba el archivo completo
  (todo-o-nada). Ahora omite la línea mala, la reporta y persiste el resto; el archivo queda
  `PARTIAL_SUCCEEDED` (WITH_DEFECTS). Cambio en `MeasureFileTypeProcessor`.

- **Outcome erróneo con cuarentena**: la marca de "no se persistió nada" usaba solo el conteo de
  INSERTs; un archivo de corrección (updates/skips) + 1 fila a cuarentena se marcaba `FAILED`
  engañosamente. Corregido para usar inserted+updated+skipped (`handledMeasures`).

- **Binary-split en el camino INSERT corrompía las filas buenas** (detectado al probar alta inicial
  con defectos, BD fresca): el binary-split reintenta las **mismas instancias**. Tras el primer flush
  fallido, `@Tsid`/IDENTITY ya había asignado el `id` en memoria y el rollback no lo limpia. En el
  reintento, `saveAll` veía `id != null` → lo trataba como *detached* → `merge` → `UPDATE` de una fila
  inexistente → `StaleObjectStateException` ("Row was updated or deleted by another transaction"),
  haciendo fallar **también las filas buenas** y dejando el archivo en bucle de reproceso. El camino
  UPDATE no se veía afectado (recarga managed con `findAllById` en cada intento). Corregido en
  `MeasureBatchWriter.insertBatch`: resetea el `id` antes de guardar para forzar la ruta INSERT
  (`isNew == true`) en cada intento.

## Pendientes de investigar (posibles bugs)

- **Posible falso `DUPLICATED_CONTENT`**: al cambiar el contenido y renombrar a `.4.4`, se marcó como
  contenido duplicado (colisión con `.4.0`). Si el contenido realmente cambió, el hash SHA-256 del
  archivo debería diferir y no debería marcarse. **Diagnóstico**: comparar el hash del archivo
  rechazado contra la columna `hash` de `file_records` para la familia; si no coincide con ninguno,
  es un bug en `DuplicatedContentByHashValidator`.

## Observaciones de rendimiento

- Carga inicial (todo INSERT): ~8s para 140,952 filas. Piso dominado por el índice único de negocio.
- Reproceso casi-idéntico (todo omitido + pocos cambios): ~2s. El hash evita reescribir lo que no cambió.

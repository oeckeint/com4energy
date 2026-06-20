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
- **Redondeo (HALF_EVEN / bancario)**: las magnitudes horarias (P1) con decimales se guardan
  redondeadas al entero más cercano; el empate exacto `.5` va al entero **par** (`7.001 → 7`,
  `7.499 → 7`, `7.999 → 8`; empates: `0.5 → 0`, `7.5 → 8`, `8.5 → 8`). Se eligió HALF_EVEN porque
  estos valores se **suman** para decisiones de compra de energía y así el agregado no acumula sesgo
  (HALF_UP siempre subiría el `.5`). La política está centralizada en una constante (`MAGNITUDE_ROUNDING`).
  **Solo aplica a P1.** Las medidas **P2 (cuarto-horario) se ingieren como enteros** (sin redondeo);
  si un archivo P2 trajera un decimal, esa fila se reporta como **defecto de parseo** (`phase=parse`)
  y no se guarda — visibilidad sin corromper. (Confirmado: las muestras P2 reales son enteras.)

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
| 1 | Carga inicial `.3` (BD fresca, 140,952 filas) | `persisted=140,952, updated=0, skipped=0` | Todo INSERT. Redondeo HALF_EVEN (`7.957 → 8`), `payload_hash` BINARY(8). ~8.2s. |
| 2 | Reprocesar el mismo `.3` (mismo nombre y contenido) | Rechazado en subida: `DUPLICATED_ORIGINAL_FILENAME` | No llega a procesarse. |
| 3 | `.3.1` con **mismo contenido**, nombre nuevo | Rechazado en subida: `DUPLICATED_CONTENT` | El hash del archivo es idéntico → no hay nada que procesar. |
| 4 | `.3.1` con **1 valor modificado** en una `(cliente,fecha)` existente | `persisted=0, updated=1, skipped=140,951` | UPDATE in-place (mismo `id_medida_h`). 140,951 omitidas → **~2s** (vs 8s). `COUNT(*)` no cambia. |
| 5 | `.3.2` cambiando la **fecha** de una línea (2026→2027) | `persisted=1, updated=1, skipped=140,951` | La línea movida a 2027 = clave nueva → **INSERT**; la fecha vieja vuelve a su valor original → **UPDATE**. `COUNT(*)` sube en 1. Editar la identidad (cliente/fecha) crea fila nueva y deja la vieja. |
| 6 | Subir a **revisión `.4`** teniendo aplicado hasta `(3,3)` | Aceptado; `file_records`: `revision=4, processing_iteration=0`. Upsert: `persisted=1, skipped=140,952` | **La revisión mayor gana aunque su iteración (0) sea menor que la última (3)**: la revisión domina sobre la iteración, y la iteración se reinicia a 0 con la nueva revisión. |
| 7 | `.4.1` (bump de iteración dentro de la misma revisión) | Aceptado; `file_records`: `revision=4, processing_iteration=1`. Upsert aplica los cambios. | Iteración mayor dentro de la misma revisión gana. |
| 8 | `.4.3` teniendo aplicado `(4,4)` | **Procesado, sin escrituras netas**: cada `(cliente,fecha)` ya existe de `(4,4)` ≥ `(4,3)` → todo `skipped-obsoleto`; `business_result=SUPERSEDED`, BD intacta. | Revisión/iteración **menor** ya no se rechaza: se procesa y la precedencia por fila omite lo obsoleto sin pisar datos nuevos. |

| 9 | `.6` con **1 fila válida modificada + 1 fila con `actent` fuera de rango** (`INT UNSIGNED`) | `updated=1, defects=1, skipped=140,951`; `PARTIAL_SUCCEEDED`, va a `/processed`. La fila mala se aísla a `.sge_quarantine.jsonl` + evento `FILE_PERSISTENCE_QUARANTINE`. | **Commit parcial real validado**: en el mismo lote que falló, la fila buena se guardó y la mala se aisló (binary-split + `REQUIRES_NEW`). Es la pieza que ningún test unitario podía cubrir. |
| 10 | `.7` con **1 fila ingarseable (`actent=abc`) + 1 fila `actent` fuera de rango + 1 cambio válido** | `updated=1, defects=2, skipped=140,950`; `PARTIAL_SUCCEEDED`. La de `abc` → `.sge_defect.jsonl` (`phase=parse`), la de overflow → `.sge_quarantine.jsonl`. | **Parseo tolerante validado** + las 3 capas conviviendo: una fila mala (de cualquier tipo) ya no tumba el archivo; se persiste el resto. |
| 11 | **Alta inicial (BD fresca)** del `.3` con **1 fila ingarseable (`abc`) + 1 fila `actent` fuera de rango** al final | `total=140,954, persisted=140,952, updated=0, skipped=0, parseDefects=1, quarantined=1`; `PARTIAL_SUCCEEDED`. ~8.3s. | **Binary-split en el camino INSERT validado**: las 140,952 buenas insertan, la de overflow se aísla a cuarentena (búsqueda binaria converge a 1 fila) y la de `abc` a `.sge_defect.jsonl`. Destapó y validó el fix del bug de reintento (`StaleObjectStateException` por reuso de instancias con id ya asignado). |
| 12 | **Colisión cross-familia**: archivo de OTRA familia con `(cliente,fecha)` ya cargadas por otra familia | **Rechazado**: `CROSS_FAMILY_COLLISION` → `/rejected`, `business_result=NOT_PERSISTED`, **BD intacta** (0 escrituras), evento `FILE_REJECTED`. | **Detección por familia real validada**: el prefetch trae la familia de cada fila existente y la compara con la del archivo entrante; si difiere → rechazo antes de escribir. (Reemplazó la heurística anterior; más preciso.) |

## Escenarios de precedencia por fila (validados, BD real)

| # | Entrada | Resultado | Notas |
|---|---|---|---|
| 13 | Mismo lote, `.0` y `.1` en cualquier orden | Estado final idéntico sin importar el orden; `.1` gana, únicas de `.0` insertan | Cubierto por el mecanismo de #14 (la precedencia es por fila, no por orden de proceso). |
| 14 | Rezagado: revisión inferior `.2` llega **después** de aplicar `.3`/`.4` | `persisted=2, skippedStale=2`; filas que chocan → `skippedStale` (conservan su valor nuevo, p.ej. `actent=202`, `id_file_record` sin cambiar); filas únicas → INSERT. `COUNT` 10→12 | **Núcleo del rediseño**: un archivo viejo que llega tarde **no pisa** lo nuevo y **no pierde** sus filas únicas. |
| 15 | `.1` totalmente obsoleto (todas las `(cliente,fecha)` ya existen de revisión ≥) | `skippedStale=3, persisted=0`; `business_result=SUPERSEDED`, `quality_status=CLEAN`, `COUNT` sin cambio | No-op limpio, distinguible en el reporte. |

> Validación E2E completa en BD real (dev, MySQL strict mode) sobre archivos de control:
> - **2026-06-17**: redondeo HALF_EVEN (incl. empate al par), UPDATE in-place, `skippedIdentical`/`skippedStale`, precedencia por fila (#14/#15), overflow con los topes reducidos (SMALLINT UNSIGNED → cuarentena) y cross-familia.
> - **2026-06-19**: precedencia de **iteración** (`.4` alta → `.4.1` iteración mayor gana → `.4.0` iteración menor `skipStale` → `.5` revisión mayor domina aunque su iteración sea 0), y **smoke test P2** (alta + corrección en `medida_qh`: insert/update/skipIdentical, y un decimal → defecto de parseo, parse-tolerante en QH).

### Detalle — precedencia de iteración (archivo de control, 3 filas A/B/C)

Familia `P1D_0031_0894_20260618`. Solo se cambia la fila **A**; B y C quedan idénticas en cada paso.

| # | Entrada | Resultado | Estado de A | Prueba |
|---|---|---|---|---|
| 16 | `.4` (alta) | `persisted=3` | `(rev 4, iter 0)` | `.4` "pelado" se guarda como **iteración 0** (no NULL) |
| 17 | `.4.1` (cambia A) | `updated=1, skippedIdentical=2` | `(4,1)`, `actent` actualizado | **iteración mayor** (misma revisión) → UPDATE |
| 18 | `.4.0` (cambia A) | `updated=0, skippedIdentical=2, skippedStale=1`; `business_result=SUPERSEDED` | **intacta en `(4,1)`** | **iteración menor** no pisa → `skipStale` |
| 19 | `.5` (cambia A) | `updated=1, skippedIdentical=2` | `(5,0)`, `actent` actualizado | **revisión mayor domina** aunque su iteración (0) < la existente (1) |

> Nota: `.4` y `.4.0` son la **misma versión lógica** `(4,0)` con nombre distinto; el guard de duplicado compara el nombre crudo, así que ambos se procesan (el row-level los resuelve sin corromper). Endurecimiento pendiente: deduplicar por `(familia, revisión, iteración)` — ver tarea secundaria.

### Detalle — smoke test P2 (cuarto-horario → `medida_qh`)

| # | Entrada | Resultado | Prueba |
|---|---|---|---|
| 20 | `P2D_..._20260618.0` (alta, 3 filas enteras) | `measureType=MEDIDA_QH_P2, persisted=3, targetTable=medida_qh` | parseo P2 (enteros), INSERT en `medida_qh`, boolean 0/1, tipos, procedencia |
| 21 | `P2D_..._20260618.1` (corrige 1 fila + 1 fila con decimal `300.5`) | `total=3, updated=1, skippedIdentical=1, parseDefects=1`; `PARTIAL_SUCCEEDED` | UPDATE + skipIdentical en QH; el decimal → **defecto de parseo** (`phase=parse`), no se guarda ni se redondea (P2 es solo-enteros) |

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
| **Validación** (TOLERANT) | reglas de negocio (CUPS, no-negativos) y **rango de almacenamiento** (`STORAGE_RANGE`: magnitudes/calidad ≤ 65535 SMALLINT UNSIGNED, `metodObt` 0..255 TINYINT UNSIGNED) | Reporta la fila como defecto, persiste las buenas. |
| **Binary-split** (BD) | parsea y valida OK, pero rompe una constraint de BD | Aísla la fila a cuarentena (`.sge_quarantine.jsonl`), persiste las buenas (commit parcial por `REQUIRES_NEW`). |

> **Nota sobre el overflow (Path A, implementado)**: el rebase de una columna right-sized (p. ej. `actent` > 65535) ahora se atrapa en la capa de **validación** (`MagnitudeRangeRecordValidator`, `@Order(60)`), usando el mismo redondeo HALF_EVEN que la persistencia (centralizado en `MeasureMagnitudes`). Así el caso predecible se reporta como defecto limpio ANTES de la BD y **deja de disparar el binary-split y su ruido de Hibernate** (`SqlExceptionHelper` / `HHH100503`). El binary-split queda como red de último recurso solo para errores de BD genuinamente inesperados — donde el log SÍ debe gritar.

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

## Guard de versión duplicada (`.4` ≡ `.4.0`) — implementado

**Problema**: `P1D_..._20260502.4` y `P1D_..._20260502.4.0` son nombres distintos (el guard de
nombre exacto no los empareja) y pueden tener contenido distinto (el guard de hash tampoco), pero
representan la **misma versión lógica** `(familia, revisión=4, iteración=0)`. Sin guard, el segundo
se procesaba como un last-write-wins silencioso sobre el primero — lo que se identificó como un
"bypass".

**Solución**: nuevo `DuplicatedMeasureVersionValidator` (`@Order(220)`, en la cadena de subida,
después del guard de nombre `210` y antes del de contenido `300`). Parsea la versión del nombre con
`FileNameVersionParserUtil`; si el nombre no sigue la convención (familia `null`, p. ej. XML) no
aplica; si la tupla `(familia, revisión, iteración)` ya existe en `file_records`
(`FileRecordRepository.existsByMeasureVersion`, sin importar estado), rechaza con
`FailureReason.DUPLICATED_VERSION` y el archivo cae a la carpeta de **duplicados** (no a
rechazados). El reporte conserva `DUPLICATED_VERSION` como motivo, distinto de
`DUPLICATED_ORIGINAL_FILENAME`, para que diga la verdad: rechazado por versión equivalente, no por
nombre.

**Importante**: NO rechaza versiones mayores ni menores — solo la tupla exacta. Bumpear la
iteración (`.4.1`) sigue siendo una versión nueva válida.

**Cubierto por unit tests** (`DuplicatedMeasureVersionValidatorTest`): empareja `.4.0` cuando `.4`
ya existe; acepta versión nueva; acepta iteración distinta de la misma revisión; no toca BD para
archivos fuera de convención.

**Pendiente de validación E2E**: subir `.4`, luego `.4.0` con contenido distinto → el segundo debe
caer a duplicados con `DUPLICATED_VERSION` y NO escribir en `medida_h`.

## Investigaciones cerradas

- **Falso `DUPLICATED_CONTENT` — DESCARTADO (no es bug)**, verificado E2E el 2026-06-19 (reproducción
  controlada, familia nueva `P1D_0031_0894_20260721`): se cargó `.4.0` (base), luego `.4.4` con **un
  valor realmente cambiado** y luego `.4.5` **byte-idéntico** al `.4.0`.
  - `.4.4` (contenido distinto → hash distinto) **se procesó** (`updated=1 skippedIdentical=2`), NO se
    rechazó.
  - `.4.5` (byte-idéntico → mismo hash) **sí** se rechazó `DUPLICATED_CONTENT`.

  El guard de hash funciona correctamente: solo rechaza cuando los bytes son idénticos. El caso
  reportado en su momento fue contenido byte-idéntico (copia renombrada sin cambio efectivo), no un
  defecto del validador.

## Observaciones de rendimiento

- Carga inicial (todo INSERT): ~8s para 140,952 filas. Piso dominado por el índice único de negocio.
- Reproceso casi-idéntico (todo omitido + pocos cambios): ~2s. El hash evita reescribir lo que no cambió.

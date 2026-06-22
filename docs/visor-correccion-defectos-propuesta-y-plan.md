# Visor de corrección de defectos — Propuesta y plan de implementación

> Estado: **propuesta / diseño acordado**. No implementado todavía. Documento de trabajo para
> retomar en un futuro cercano. La ingesta de medidas H/QH con upsert por fila ya está en producción
> (ver [upsert-medidas-escenarios-e2e.md](./upsert-medidas-escenarios-e2e.md)); esto se construye encima.

---

## PARTE 1 — Propuesta (resumen para el cliente)

### El problema que resuelve

Cuando se carga un archivo de medidas, puede traer **filas con errores** (un valor fuera de rango, un
CUPS con formato inválido, una línea mal formada, etc.). Hoy el sistema ya hace lo correcto: **guarda
las filas buenas y aísla las malas** en un reporte de defectos, sin perder datos ni bloquear el archivo
completo. Pero **corregir esas filas es hoy un trabajo manual y externo**: alguien tiene que leer el
reporte, entender qué falló, preparar un archivo corregido y volver a subirlo por su cuenta.

### Qué proponemos

Un **visor de corrección de defectos** dentro del dashboard que convierte ese trabajo manual en un flujo
guiado, tratándolo como si fuera la **resolución de un "conflicto de fusión" (merge conflict)**:

1. El visor muestra **exactamente qué filas fallaron y por qué** (línea, regla incumplida, mensaje, y el
   contenido original de la línea).
2. El operador **corrige cada fila** dentro de la misma pantalla.
3. Al terminar todas las correcciones, se hace **"flush"**: el sistema genera automáticamente una
   **nueva versión corregida** (una "iteración" del mismo archivo) **solo con las filas corregidas** y la
   reintroduce al proceso normal.
4. El sistema **fusiona las correcciones de forma segura**: aplica solo lo corregido, sin tocar lo que ya
   estaba bien, y **nunca sobrescribe datos más nuevos**.
5. Si tras corregir aún quedan errores, el ciclo se repite sobre una nueva versión, hasta quedar limpio.

### Beneficios

- **Sin pérdida de datos**: las filas buenas se guardan siempre; las malas quedan trazadas y corregibles.
- **Corrección guiada, no a ciegas**: el operador ve la causa exacta de cada error en una sola pantalla.
- **Seguro por diseño**: una corrección **no puede pisar datos más recientes** (si entretanto llegó una
  versión más nueva del origen, esa prevalece). Esto ya es una propiedad probada del sistema actual
  ("a prueba de rezagados").
- **No hay que reenviar archivos completos**: se reintroduce solo lo corregido.
- **Trazabilidad y auditoría completas**: cada corrección genera su propio registro encadenado (archivo
  original → reporte de defectos → archivo de corrección → …), útil para auditoría y *compliance*.
- **Retención eficiente**: los reportes "fríos" (ya resueltos, solo para compliance) pueden archivarse a
  almacenamiento externo sin recargar la base de datos ni los respaldos.

### Escenarios donde funciona bien

- **Errores de contenido corregibles por el operador**: un valor mal tecleado, un CUPS con formato
  inválido, una magnitud fuera de rango, una línea con formato incorrecto. Son los casos del visor.
- **Correcciones tardías y fuera de orden**: aunque la corrección llegue días después y entretanto haya
  entrado una versión más nueva, el sistema resuelve la precedencia **fila por fila** sin corromper nada.

### Qué NO entra en este flujo (y por qué)

- **Fallos a nivel de base de datos (cuarentena)**: errores que sobreviven a la validación y aun así
  fallan al insertar (constraint inesperada, etc.). Son **raros** y normalmente **no se arreglan
  editando un valor** — son señal de un problema técnico, no de un dato mal tecleado. Por eso se tratan
  como un **bucket separado con alerta**, no en el visor de edición.
- **Medidas CCH (archivos F5)**: su tabla está en retirada; ya no se ingieren (ver Parte 2, contexto).

### Sobre el alcance temporal

Esto **lleva tiempo** y hay otras prioridades antes. Este documento deja la propuesta y el plan
acordados para retomarlos cuando corresponda, sin perder el análisis ya hecho.

---

## PARTE 2 — Plan de implementación (técnico)

### Contexto: lo que ya existe hoy

- **Upsert por fila (H y QH)**: insert / skip-idéntico (por hash) / update-in-place / skip-obsoleto, con
  **precedencia por fila** `(revisión, iteración)`. Orden-independiente y a prueba de rezagados.
- **Concepto de iteración**: el archivo se versiona por nombre `familia.{revisión}` o
  `familia.{revisión}.{iteración}`. Gana la `(revisión, iteración)` más alta; una nueva revisión reinicia
  la iteración. La precedencia se resuelve **por fila**.
- **Reportes de defecto/cuarentena** (`MeasureDefectReportService`), escritos en `{failedPath}/defects/`:
  - `*.sge_defect.jsonl` (+ hoy también `.csv`): defectos de **parse** y **validación** (+ errores string
    de persistencia). Cada entrada: `originalFile, phase, line, brokenRule, message, rawLine`.
  - `*.sge_quarantine.jsonl`: filas aisladas por el binary-split (fallos a nivel BD).
- **Eventos al dashboard**: `FILE_DEFECT_REPORT_CREATED` / `FILE_PERSISTENCE_QUARANTINE` llevan
  **conteo + ruta** del reporte (no el contenido) a `file_record_events`; el dashboard recibe notificación
  vía SSE (`FileProcessingNotificationSseService`).
- **Quién escribe medidas**: **ingestion-service** (tiene el motor de upsert: `JpaMeasurePersistenceAdapter`
  + `MeasureBatchWriter`). `records-api` es **read-only** para medidas. ⟹ Cualquier "retry" con la misma
  semántica debe pasar por ingestion; un camino de escritura aparte duplicaría el motor.

#### Ya implementado en esta línea de trabajo (sesión previa)

- **Rechazo de CCH (F5)**: `FailureReason.UNSUPPORTED_MEASURE_TYPE`; al inicio de
  `MeasureFileTypeProcessor.process()` se rechaza sin escribir y emite `fileRejected` → notificación SSE.
  Cubre carga manual y escáner.
- **Carga `.0`–`.9` re-habilitada** en el dashboard (el backend ya las aceptaba).

### Decisiones de diseño acordadas

1. **Visor estilo "merge conflict"**: se deben **editar todas** las líneas detectadas; no hay flush hasta
   resolverlas todas. **No** se permite omitir/descartar líneas (evita el limbo de "¿qué pasa con las
   omitidas?"). Consecuencia aceptada: una línea irresoluble bloquea el lote.
2. **Iteración sparse**: la corrección contiene **solo las filas que estaban mal** (el reporte ya las
   extrajo). El upsert por fila las aplica; las buenas ya están persistidas.
3. **Iteración destino = `max(iteración existente para (familia, revisión)) + 1`**, en la **misma
   revisión**. No se cambia la revisión (mantener procedencia limpia; la precedencia arbitra el resto).
4. **Escenario de revisión superior cubierto por diseño**: si entretanto entró una revisión mayor
   (`.4.0`), la corrección de revisión 3 (`.3.x`) **no puede pisarla** — rellena solo las celdas que la
   revisión 4 no tocó; el resto se omite como *superseded*. **Sin riesgo de integridad.** (Opcional/UX:
   el visor podría avisar "existe una revisión mayor; esto podría quedar superseded".)
5. **Cuarentena separada**: no entra al visor (fallos a nivel BD, no corregibles editando un valor). Se
   trata como bucket con **alerta**. Bloqueo técnico adicional: hoy guarda `recordString` (un `toString`),
   no la línea original → no es re-subible sin trabajo extra. Si algún día se quiere, primero hay que
   capturar la línea original.
6. **Formatos**: se **elimina el CSV**; queda **solo el JSONL** como fuente que lee el visor (CSV se
   podría exportar on-demand si alguien lo pide).
7. **Tope de versión**: **iteración pasa a multi-dígito** (`.N.10`, `.N.11`, …); **revisión se queda 0–9**.
   Razón: las correcciones (iteraciones) pueden necesitar muchas rondas; las revisiones son reediciones
   oficiales, raras. BD sin cambios (`revision`/`processing_iteration` son `SMALLINT UNSIGNED`).
8. **Tabla dedicada `defect_report`** (no meterlo en `file_records`): evita mezclar dos lifecycles
   (ingesta vs revisión humana) y el guardrail frágil de que el job no la procese.
9. **Payload en archivo (puntero), metadatos/dimensiones en BD**:
   - El payload pesado (`raw_line`, `message`) vive en el `.jsonl` (caliente mientras se revisa; frío y
     archivable una vez resuelto → encaja con el futuro job de tiering/archivado en ghost-flows).
   - La BD guarda metadatos pequeños, durables y consultables (estado, conteos, FKs, clave del archivo).
   - **Clave relativa** (sufijo), no ruta absoluta; el fetcher resuelve por **ubicaciones base
     configurables** (interno → archivado → falla con error claro), con **hint `storage_location`**
     (`LOCAL`/`ARCHIVED`) que el job de archivado actualiza para no stat-ear la unidad lenta en cada lectura.
   - **Regla de retención**: no archivar/purgar el `.jsonl` mientras el estado sea `NOT_REVIEWED`/`SUBMITTED`.
10. **No almacenar el status final de la corrección**: se **deriva** siguiendo el FK al `file_record` de
    corrección (evita estado duplicado/stale). Tampoco un flag "enviado": lo codifica el estado
    `SUBMITTED` + `correction_file_record_id` no nulo.
11. **Separar a otra BD para métricas = prematuro**: una tabla en la misma BD basta al volumen de defectos.

### Modelo de datos

```
defect_report
  id
  source_file_record_id       FK  -> archivo que generó los defectos
  file_key                    clave relativa al payload .sge_defect.jsonl
  storage_location            LOCAL | ARCHIVED   (hint; lo actualiza el job de archivado)
  defect_count                = parseDefects + validationDefects   (NO incluye cuarentena)
  parse_count / validation_count   (contadores por tipo; "gratis", ya existen como buckets)
  review_status               NOT_REVIEWED | SUBMITTED | REVIEWED
  correction_file_record_id   FK nullable -> la iteración que lo atendió
  created_at / submitted_at / reviewed_at

defect_line   (FUTURE ENHANCEMENT — para filtros y métricas por tipo/archivo)
  defect_report_id   FK
  phase / broken_rule / line / source_file_record_id
  (raw_line / message se quedan en el archivo, no en BD)
```

Notas:
- `defect_count` **excluye** la cuarentena (que vive en `.sge_quarantine.jsonl`, lifecycle aparte), para
  que el conteo cuadre con el contenido del `.sge_defect.jsonl` que lee el visor.
- "Qué archivo" = `source_file_record_id` (FK). La cadena de corrección se reconstruye:
  `defect_report A → A.correction_file_record_id = B → si B genera defectos, C.source_file_record_id = B`.

### Máquina de estados (review_status)

```
NOT_REVIEWED  --(operador hace flush: se genera iteración y entra al pipeline)-->  SUBMITTED
SUBMITTED     --(la iteración de corrección termina de procesarse)-->              REVIEWED
```

- `REVIEWED` significa **"atendido"**, NO "datos limpios". Si la iteración trajo defectos nuevos, nace un
  **nuevo** `defect_report` (con `source_file_record_id` = el `file_record` de la corrección) y el ciclo
  sigue sobre nuevos archivos/iteraciones.
- El estado intermedio `SUBMITTED` cubre el hueco asíncrono entre "envié" y "terminó de procesar".
- "Si quedó limpio" se **deriva** recorriendo la cadena de FKs, no se almacena.

### Cuándo se pobla `defect_report`

- **Al final de `MeasureFileTypeProcessor.process()`**, en ingestion-service, cuando los conteos ya están
  calculados (buckets `parseDefects`, `validationDefects`) y el `.sge_defect.jsonl` ya está escrito.
- **Solo si** `parseDefects + validationDefects > 0`.
- **Lo escribe ingestion directamente** (ya posee la tabla, tiene BD y ya guarda el `FileRecord`); no hace
  falta dar la vuelta por el outbox/RabbitMQ. El evento `FILE_DEFECT_REPORT_CREATED` puede seguir solo para
  el log de `file_record_events`.
- **Idempotencia en RETRY**: el job de RETRY puede reprocesar el archivo fuente → **upsert por
  `source_file_record_id`** (una fila por archivo fuente) para no duplicar.
- Orden: `.jsonl` durante el `process()`, fila `defect_report` al cierre (si el insert falla, queda un
  `.jsonl` huérfano, menor/limpiable; para consistencia estricta, crear la fila en la misma transacción
  que el cierre del `FileRecord`).

### Flujo end-to-end del visor

1. Ingestion procesa un archivo con defectos de contenido → escribe `.sge_defect.jsonl` + crea
   `defect_report` (`NOT_REVIEWED`).
2. El dashboard lista los `defect_report` pendientes (vía endpoint de ingestion) y, al abrir uno, pide el
   contenido del `.jsonl` (el "extractor": ingestion resuelve la clave relativa y sirve el archivo).
3. El operador edita **todas** las líneas (estilo merge conflict).
4. Flush → endpoint de ingestion: calcula `max(iteración para (familia, revisión)) + 1`, genera el archivo
   de iteración **sparse** (solo las filas corregidas) y lo mete al pipeline → nuevo `file_record`;
   `defect_report.correction_file_record_id` = ese file_record; estado → `SUBMITTED`.
5. La iteración se procesa por el pipeline normal (re-parse, re-validación, upsert por fila, eventos, SSE).
6. Al terminar: `defect_report` → `REVIEWED`. Si la iteración generó defectos nuevos → nuevo
   `defect_report` y vuelve al paso 2.

### Cambios concretos por módulo (cuando se implemente)

- **Kernel / BD (db-migrations)**: tabla `defect_report` (+ `defect_line` cuando se aborden métricas).
- **ingestion-service**:
  - Quitar la escritura de `.csv` en `MeasureDefectReportService` (dejar solo JSONL).
  - Crear `defect_report` al cierre de `process()` (con upsert idempotente por `source_file_record_id`).
  - Multi-dígito en iteración: `FileNameVersionParserUtil` (revisión = 1 dígito, iteración = 1+ dígitos) y
    `FileExtensionValidator` (aceptar último segmento multi-dígito cuando es iteración); ajustar
    `DuplicatedMeasureVersionValidator` y tests (`FileExtensionValidatorTest`, `FileNameVersionParserUtilTest`).
  - Endpoints (servidos vía proxy del dashboard): listar `defect_report`, obtener contenido del `.jsonl`
    (resolver clave relativa con fallback de ubicaciones + hint), recibir correcciones → generar iteración
    sparse → submit; cálculo de iteración destino (server-side, para evitar choques de concurrencia que
    darían `DUPLICATED_VERSION`).
  - Hook al terminar la iteración de corrección → marcar `defect_report` `REVIEWED`.
- **dashboard**: visor merge-conflict (lee JSONL, edita todas las líneas, flush), listado de pendientes,
  alerta de cuarentena (bucket separado).
- **ghost-flows (futuro)**: job de archivado/tiering que mueve `.jsonl` fríos a almacenamiento externo y
  actualiza `storage_location` (move-primero-luego-actualizar; respetar la regla de retención).

### Puntos abiertos / a definir

- **Aviso de revisión superior** en el visor (UX, no integridad): ¿avisar/auto-marcar obsoletos los
  defectos cuyas `(cliente, fecha)` ya estén cubiertos por una revisión mayor?
- **Alertado de `NOT_REVIEWED`**: ¿alerta tras N días sin atender? ¿umbral configurable?
- **`defect_line`**: cuándo se aborda (depende de la necesidad real de filtros/métricas). De hacerse:
  índice consultable en BD + payload en archivo (duplicación intencional índice/blob).
- **Caché (radar, por directiva)**: si las métricas de defectos terminan siendo un agregado del dashboard
  consultado seguido → candidato a Caffeine en records-api. Hoy prematuro.
- **Limpieza**: borrar `MeasureQuarantineService` (código muerto, sin llamadores) cuando se toque esta área.

### Referencias

- [upsert-medidas-escenarios-e2e.md](./upsert-medidas-escenarios-e2e.md) — base de upsert/precedencia.
- Código clave: `MeasureFileTypeProcessor`, `MeasureDefectReportService`, `FileNameVersionParserUtil`,
  `FileExtensionValidator`, `FileRecordConsumer` (records-api), `FileProcessingNotificationSseService`.

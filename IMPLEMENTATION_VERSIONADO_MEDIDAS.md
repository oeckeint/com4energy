# Implementación: Versionado de Medidas por Extensión de Archivo

**Fecha:** 2026-05-25  
**Estado:** 📋 Ready for implementation  
**Proyecto:** `c4e-ingestion-service`

---

# 🎯 Objetivo

Permitir versionado incremental de medidas mediante extensiones numéricas de archivo:

- `.0`
- `.1`
- `.2`
- etc.

Una revisión más nueva puede:

- actualizar registros existentes,
- insertar nuevos registros,
- corregir parcialmente información previa.

El sistema debe:

- aceptar revisiones no secuenciales,
- ignorar revisiones obsoletas,
- ser idempotente,
- mantener compatibilidad con datos legacy.

---

# ✅ Reglas de Negocio (Invariantes)

## 1. Revision mayor gana

```text
incoming_revision > current_revision
→ UPDATE permitido
```

---

## 2. Revision vieja se ignora

```text
incoming_revision < current_revision
→ IGNORE
```

---

## 3. Replay idéntico es idempotente

```text
same revision + same payload
→ NOOP
```

No debe generar cambios ni incrementar contadores.

---

## 4. Conflicto lógico

```text
same revision + different payload
→ CONFLICT
```

Acción mínima:

- no actualizar,
- registrar warning/error en logs,
- incrementar contador de conflictos en métricas,
- continuar procesamiento del resto del lote.

---

## 5. Correcciones parciales NO borran datos

NULL significa:

```text
campo ausente / no enviado
```

NO significa:

```text
borrar valor existente
```

---

## 6. Revisiones no secuenciales son válidas

Ejemplo válido:

```text
.0 → .2
```

No es obligatorio recibir `.1`.

---

# 📋 Claves de Negocio

## medida_h

```text
(id_cliente, fecha, tipo_medida)
```

---

## medidaqh

```text
(id_cliente, fecha, tipomed)
```

---

## medida_cch

Hipótesis actual:

```text
(id_cliente, fecha)
```

⚠️ Pendiente validar en producción.

---

# ⚠️ Fase 0 — Validación de Datos Legacy

Antes de cualquier hardening:

ejecutar validación de duplicados históricos.

## medida_h

```sql
SELECT id_cliente, fecha, tipo_medida, COUNT(*) as cnt
FROM medida_h
GROUP BY id_cliente, fecha, tipo_medida
HAVING cnt > 1
LIMIT 100;
```

## medidaqh

```sql
SELECT id_cliente, fecha, tipomed, COUNT(*) as cnt
FROM medidaqh
GROUP BY id_cliente, fecha, tipomed
HAVING cnt > 1
LIMIT 100;
```

## medida_cch

```sql
SELECT id_cliente, fecha, COUNT(*) as cnt
FROM medida_cch
GROUP BY id_cliente, fecha
HAVING cnt > 1
LIMIT 100;
```

---

# Resultado esperado de Fase 0

## Si NO existen duplicados

Se podrá planificar:

- UNIQUE constraints,
- ON DUPLICATE KEY UPDATE,
- simplificación futura.

---

## Si SÍ existen duplicados

La feature operará inicialmente en:

```text
modo compatibilidad
```

Sin UNIQUE duro.

---

# 📦 Fase 1 — Cambios de Schema

## Nuevas columnas

Agregar a:

- `medida_h`
- `medidaqh`
- `medida_cch`

```sql
source_revision INT NOT NULL DEFAULT 0
correction_count INT NOT NULL DEFAULT 0
payload_hash CHAR(64) NULL
```

---

# Índices

## medida_h

```sql
CREATE INDEX idx_medida_h_business_key
ON medida_h(id_cliente, fecha, tipo_medida);

CREATE INDEX idx_medida_h_revision
ON medida_h(source_revision);
```

Aplicar equivalente para:

- `medidaqh`
- `medida_cch`

---

# 🧠 Fase 2 — Canonicalización de Payload

El hash debe representar:

```text
identidad semántica
```

NO identidad textual.

---

# Reglas

## ❌ NO usar

```java
SHA256(rawLine)
```

---

## ✅ Sí usar

Payload canónico:

- trim,
- orden fijo,
- normalización decimal,
- representación estable.

---

# Ejemplo

```java
private String normalizeDecimal(Number value) {
    if (value == null) return null;

    return new BigDecimal(value.toString())
        .stripTrailingZeros()
        .toPlainString();
}
```

---

# Canonicalización

```java
Map<String, Object> canonical = new LinkedHashMap<>();

canonical.put("actent", normalizeDecimal(record.actent()));
canonical.put("qactent", normalizeDecimal(record.qactent()));
```

Luego:

```java
objectMapper.writeValueAsString(canonical)
```

Finalmente:

```java
SHA-256
```

---

# ⚙️ Fase 3 — Persistencia

## Estrategia actual

Modo compatibilidad:

```text
merge app-level
```

Sin depender todavía de:

```sql
ON DUPLICATE KEY UPDATE
```

---

# Flujo

## 1. Parse batch

## 2. Canonicalizar payload

## 3. Calcular payload_hash

## 4. Buscar existentes por clave de negocio

## 5. Clasificar outcome

Outcomes posibles:

```text
INSERTED
UPDATED
IGNORED_OLD_REVISION
IGNORED_IDENTICAL
CONFLICT_SKIPPED
```

## 6. Ejecutar inserts

## 7. Ejecutar updates válidos

## 8. Registrar conflictos por log/métrica (sin auditoría persistente)

---

# 📊 correction_count

`correction_count` representa únicamente la cantidad de revisiones efectivamente aplicadas sobre un registro con payload semánticamente distinto.

NO representa:

- cantidad neta de cambios,
- historial completo,
- número total de archivos recibidos.

---

# ⚠️ Concurrencia

Los archivos llegan desde una fuente única, por lo que la concurrencia real es baja.
Sin embargo, existen dos jobs (`pending` y `retry`) con schedulers independientes que pueden
procesar archivos distintos de la misma familia simultáneamente.

## Mitigación implementada: Family Check

Antes de procesar un archivo, se verifica que ningún archivo de la misma **familia**
(mismo base name, distinta extensión) esté actualmente locked/en proceso:

```java
// FileRecordService.isFamilyBeingProcessed()
String familyPattern = FilenameUtils.getBaseName(filename) + ".%";
repository.existsFamilyFileBeingProcessed(familyPattern, fileRecord.getId());
```

Si existe un hermano en proceso → **defer** (skip en ese ciclo, se reintenta al siguiente tick).

Ejemplo:

```text
P2_12345.0  →  PROCESSING (locked)
P2_12345.1  →  PENDING    → deferred hasta que .0 termine
```

Archivos afectados:

- `FileRecordRepository` → `existsFamilyFileBeingProcessed()`
- `FileRecordService`    → `isFamilyBeingProcessed()`
- `FileProcessingServiceImpl` → check antes de reclamar

⚠️ Mientras no existan UNIQUE constraints reales, la consistencia depende parcialmente del flujo de procesamiento y del control de concurrencia a nivel aplicación. Durante esta fase inicial se prioriza compatibilidad legacy sobre garantías duras.

La prioridad actual es:

```text
consistencia > throughput extremo
```

---

# 📁 Fase 4 — Versionado de Extensión

## Extensiones válidas

- `xml`
- números enteros no negativos

Ejemplos:

```text
0
1
2
15
999
```

---

# Validación

```java
private boolean isValidMeasureExtension(String ext) {

    if ("xml".equals(ext)) {
        return true;
    }

    return ext.matches("^\\d+$");
}
```

---

# Conversión

```java
Integer sourceRevision =
    Integer.parseInt(fileRecord.getExtension());
```

---

# 📈 Fase 5 — Métricas y Logging

Agregar métricas mínimas:

- parse time,
- persist time,
- batch duration,
- inserted count,
- updated count,
- ignored count,
- conflict count.

---

# Logging

Registrar eventos importantes:

```text
UPDATED
IGNORED_OLD_REVISION
CONFLICT
```

No implementar auditoría en esta fase.

---

# ❌ Fuera de Scope (Por Ahora)

No implementar todavía:

- auditoría enterprise,
- immutable audit trail,
- object storage,
- CDC,
- event sourcing,
- replay engine,
- staging tables complejas,
- distributed locking,
- compliance retention,
- S3 archival.

También fuera de scope por ahora:

- UNIQUE constraints en tablas legacy (hasta limpiar duplicados históricos).

---

# 🚀 Plan de Implementación

## Paso 1

Ejecutar Fase 0 sobre BD real.

---

## Paso 2

Agregar columnas e índices.

---

## Paso 3

Implementar canonicalización + payload hash.

---

## Paso 4

Implementar merge app-level.

---

## Paso 5

Agregar métricas y logging mínimo.

---

## Paso 6

QA funcional:

- revisiones nuevas,
- revisiones viejas,
- replay idéntico,
- conflictos,
- correcciones parciales,
- versiones no secuenciales.

---

# ✅ Resultado Esperado

Sistema capaz de:

- soportar correcciones incrementales,
- mantener idempotencia,
- ignorar revisiones obsoletas,
- detectar conflictos lógicos,
- operar sobre datos legacy sin romper producción,
- evolucionar posteriormente hacia hardening con UNIQUE constraints.
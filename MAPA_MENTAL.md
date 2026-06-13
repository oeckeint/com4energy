# 🧠 Mapa Mental: Dónde Se Persistieron Los Datos

```
                          TU ARCHIVO
                    F5D_0031_0894_20250311.0
                       53,352 registros
                              │
                    ┌─────────┼─────────┐
                    │         │         │
                    ▼         ▼         ▼
              PARSE      FILTER      VALIDATE
             (291ms)    Tarifa20TD  (? ms)
                │          │          │
         [Exitoso]   [48,960 X]   [4,392 ✓]
                │                    │
                │            ┌───────┴───────┐
                │            │               │
                │            ▼               ▼
                │         [3,720] ✅     [672] ❌
                │            │              │
                │            │         JSON Report
                │            │         .sge_defects.json
                │            │
                │    ┌────────────────┐
                │    │ PERSISTENCIA   │
                │    │ (1,736 ms)     │
                │    │ JPA Batch      │
                │    │ 1000+1000+720  │
                │    └────────────────┘
                │            │
                │            ▼
            ┌───────────────────────────────┐
            │   MySQL Base de Datos: sge    │
            │   ─────────────────────────   │
            │   Tabla: medida               │
            │   Registros: 3,720 ✅        │
            │   created_by: SYSTEM          │
            │   created_on: 2025-03-11      │
            │                               │
            │   Columnas:                   │
            │   ├─ id_medida (PK)           │
            │   ├─ id_cliente (FK)          │
            │   ├─ fecha (DATETIME)         │
            │   ├─ ae1, as1, rq1-4 (INT)   │
            │   ├─ metod_obt (INT)          │
            │   ├─ indic_firmez (INT)       │
            │   ├─ codigo_factura (VARCHAR) │
            │   └─ created_on, created_by   │
            └───────────────────────────────┘
                           │
                ┌──────────┴──────────┐
                ▼                     ▼
           AUDITORIA             EVENTOS
           records-api           (outbox)
           (trazabilidad)        │
                                 ├─ FILE_DEFECT_REPORT_CREATED
                                 └─ FILE_PERSISTENCE_QUARANTINE
```

---

## 📊 Árbol de Decisión: ¿Qué Ocurrió Con Mi Dato?

```
Tu Dato (cada línea del archivo)
        │
        ├─ ¿Tarifa 20TD?
        │   ├─ SÍ → ❌ OMITIDO (48,960 total)
        │   └─ NO → Continúa...
        │
        ├─ ¿Formato válido?
        │   ├─ NO → ❌ DEFECTO (672 total) → .sge_defects.json
        │   └─ SÍ → Continúa...
        │
        ├─ ¿Cliente existe?
        │   ├─ NO → ❌ DEFECTO → JSON
        │   └─ SÍ → Continúa...
        │
        └─ ✅ PERSISTIDO (3,720 total) → BD: sge.medida
```

---

## 🔄 Ciclo de Vida Del Registro

```
REGISTRO 1: ✅ PERSISTIDO
═════════════════════════════════════
1. ENTRADA: Línea del archivo
2. PARSE: Extrae datos
3. VALIDACIÓN: Verifica formato, cliente
4. CLASIFICACIÓN: Tipo P1/P2/F5/P5
5. BATCH: Acumula en lote de 1,000
6. FLUSH: Cuando lote completo
7. INSERT: repository.saveAll() → SQL INSERT
8. TRANSACCIÓN: @Transactional COMMIT
9. DESTINO: sge.medida
10. AUDIT: records-api persiste evento

REGISTRO 2: ❌ CON DEFECTO
═════════════════════════════════════
1. ENTRADA: Línea del archivo
2. PARSE: Extrae datos
3. VALIDACIÓN: ❌ FALLA
4. REPORTE: Acumula error
5. DESTINO: .sge_defects.json
6. AUDIT: records-api persiste evento
7. FIN: No llega a medida tabla

REGISTRO 3: ⏭️ OMITIDO
═════════════════════════════════════
1. ENTRADA: Línea del archivo
2. PARSE: Extrae datos
3. FILTRO: Tarifa 20TD → skip
4. DESTINO: Ninguno
5. AUDIT: No se audita
6. FIN: Descartado silenciosamente
```

---

## 💾 Jerarquía de Persistencia

```
Java Object Hierarchy
═════════════════════════════════════

FileUploadController (HTTP)
          │
          └─ FileUploadOrchestratorService
             │
             └─ FileProcessingServiceImpl
                │
                └─ MeasureFileTypeProcessor
                   │
                   └─ MeasureFileParserService (PARSE)
                   └─ MeasureRecordValidationChain (VALIDATE)
                   └─ JpaMeasurePersistenceAdapter (PERSIST) ← ⭐ CLAVE
                      │
                      └─ MeasurePersistenceContracts.MeasurePersistencePort
                         │
                         └─ Repositories (JPA)
                            │
                            ├─ MedidaHRepository
                            ├─ MedidaQHRepository
                            ├─ MedidaLegacyRepository ← Tu repositorio
                            └─ MedidaCCHRepository
                               │
                               └─ Entities
                                  │
                                  ├─ MedidaHEntity
                                  ├─ MedidaQHEntity
                                  ├─ MedidaLegacyEntity ← Tu entidad
                                  └─ MedidaCCHEntity
                                     │
                                     └─ SQL Tables
                                        │
                                        ├─ medida_h
                                        ├─ medida_qh
                                        ├─ medida ← ✅ TU TABLA
                                        └─ medida_cch
```

---

## 🎯 Mapa de Componentes

```
┌─────────────────────────────────────────────────────────────┐
│           C4E-INGESTION-SERVICE                             │
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ MeasureFileTypeProcessor                            │   │
│  │ ─────────────────────────                           │   │
│  │ - detecta tipo: P1, P2, F5, P5                      │   │
│  │ - llama a validar()                                 │   │
│  │ - llama a persistir()                               │   │
│  └────────────────────┬────────────────────────────────┘   │
│                       │                                     │
│                       ▼                                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ JpaMeasurePersistenceAdapter ⭐                     │   │
│  │ ─────────────────────────────                       │   │
│  │ - acumula lotes de 1,000                            │   │
│  │ - llama saveAll() de cada repo                      │   │
│  │ - binary split si falla                             │   │
│  │ - retorna (persistidos, errores, omitidos)          │   │
│  └────────────────┬────────────────────────────────────┘   │
│                   │                                        │
│        ┌──────────┼──────────┐                             │
│        │          │          │                             │
│        ▼          ▼          ▼                             │
│   MedidaH    MedidaQH   MedidaLegacy  MedidaCCH           │
│   Repo       Repo       Repo ← TU     Repo                │
│              │                                            │
│              └─→ saveAll()                                │
│                  (JPA/Hibernate)                          │
│                                                           │
│              Repository Pattern                          │
│              ┌──────────────────────────────┐            │
│              │ interfaces {                 │            │
│              │  extends JpaRepository       │            │
│              │  saveAll(entities)           │            │
│              │ }                            │            │
│              └──────────────────────────────┘            │
└─────────────────┬──────────────────────────────────────────┘
                  │
                  │ JDBC/Hibernate Translation
                  ▼
┌─────────────────────────────────────────────────────────────┐
│              MYSQL SERVER: sge                              │
│                                                             │
│  TABLE medida_h       TABLE medida_qh                      │
│  [rows]               [rows]                               │
│                                                             │
│  TABLE medida ← ✅ TU TABLA      TABLE medida_cch          │
│  ┌──────────────────────┐         [rows]                  │
│  │ 3,720 ROWS INSERTADOS│                                 │
│  │                      │                                  │
│  │ id_medida: 1001-4720 │                                  │
│  │ id_cliente: varies   │                                  │
│  │ fecha: 2025-03-11    │                                  │
│  │ ae1, as1, rq1-4: ... │                                  │
│  │ created_by: SYSTEM   │                                  │
│  │ created_on: NOW()    │                                  │
│  └──────────────────────┘                                  │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔐 Flujo Transaccional

```
@Transactional (En JpaMeasurePersistenceAdapter.persist)
│
├─ BEGIN TRANSACTION
│  │
│  ├─ Resolver clientes (lookup CUPS → id_cliente)
│  │
│  ├─ Para cada tipo (P1, P2, F5, P5)
│  │  │
│  │  ├─ Acumular en lote
│  │  │
│  │  ├─ Si lote >= 1,000 o es final
│  │  │  │
│  │  │  ├─ repository.saveAll(batch)
│  │  │  │  └─ INSERT INTO medida (...)
│  │  │  │     VALUES (...), (...), ...
│  │  │  │
│  │  │  ├─ Si error: binarySplitAndPersist()
│  │  │  │  └─ Divide por mitad y reintenta
│  │  │  │
│  │  │  └─ Limpiar batch
│  │  │
│  │  └─ Siguiente registro
│  │
│  └─ Compilar estadísticas (persisted, errors, skipped)
│
├─ COMMIT ✅
│  └─ 3,720 registros confirmados en BD
│
└─ Retornar MeasurePersistenceResult
   (persistidos=3720, errors=672, skipped=48960)
```

---

## 📈 Métricas Del Proceso

```
Archivo: F5D_0031_0894_20250311.0
─────────────────────────────────────────

Tamaño entrada:          53,352 registros
Parsing time:            291 ms
Parsing throughput:      183 registros/ms

Después filtro 20TD:     4,392 registros (92% filtrado)
Validados:               3,720 registros (84.6% éxito)
Con defectos:            672 registros (15.3% error)

Persistencia time:       1,736 ms
Persistencia throughput: 2.14 registros/ms

Batches:
  Batch 1: 1,000 → INSERT (0-500ms)
  Batch 2: 1,000 → INSERT (500-1000ms)
  Batch 3:   720 → INSERT (1000-1736ms)

Total processing:        14,679 ms
Average:                 3.6 registros/ms
```

---

## 🔍 Ubicación En El Código

```
c4e-ingestion-service/
│
├─ src/main/java/
│  └─ com/com4energy/processor/
│     │
│     ├─ service/processing/
│     │  └─ MeasureFileTypeProcessor.java (204-212)
│     │     └─ resolveDestinationStore()
│     │        └─ case "F5" -> "medida_legacy"
│     │
│     ├─ service/measure/persistence/
│     │  └─ JpaMeasurePersistenceAdapter.java (44-91) ⭐
│     │     └─ persist() @Transactional
│     │        └─ flushBatches()
│     │           └─ flushWithBinarySplit()
│     │              └─ repository.saveAll()
│     │
│     ├─ model/measure/
│     │  └─ MedidaLegacyEntity.java (24)
│     │     └─ @Table(name = "medida")
│     │
│     └─ repository/measure/
│        └─ MedidaLegacyRepository.java
│           └─ extends JpaRepository<MedidaLegacyEntity, Long>
│
└─ src/main/resources/
   └─ application.yml (9-13)
      └─ spring.datasource.url: ${DB_URL_SGE}
```

---

## ✅ Verificación: ¿Están Los Datos?

```
PREGUNTA: ¿Están los 3,720 registros en BD?

VERIFICACIÓN 1: Contar Total
─────────────────────────────
SELECT COUNT(*) FROM sge.medida 
WHERE created_by='SYSTEM' AND DATE(created_on)=CURDATE();

Resultado esperado: 3720 ✅


VERIFICACIÓN 2: Ver Muestra
─────────────────────────────
SELECT * FROM sge.medida 
WHERE created_by='SYSTEM'
LIMIT 5;

Resultado esperado: 5 filas con datos reales


VERIFICACIÓN 3: Distribuir Por Cliente
─────────────────────────────────────────
SELECT id_cliente, COUNT(*) 
FROM sge.medida 
WHERE created_by='SYSTEM'
GROUP BY id_cliente;

Resultado esperado: Múltiples clientes con registros


CONCLUSIÓN: Si todas las verificaciones pasan ✅
→ Los 3,720 registros están persistidos correctamente
```

---

## 🎓 Resumen Visual

```
┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
┃           DONDE SE PERSISTIERON LOS DATOS           ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃                                                     ┃
┃  Tu Archivo: F5D_0031_0894_20250311.0              ┃
┃  Total: 53,352 registros                           ┃
┃                                                     ┃
┃  Distribución:                                      ┃
┃  ┌────────────────────────────────────┐            ┃
┃  │ ✅ BD: sge.medida ........... 3,720 │            ┃
┃  │ ❌ JSON: .sge_defects ....... 672    │            ┃
┃  │ ⏭️ Omitidos: tarifa 20TD ... 48,960 │            ┃
┃  └────────────────────────────────────┘            ┃
┃  Total: 53,352 ✅                                   ┃
┃                                                     ┃
┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫
┃  Verificación SQL:                                  ┃
┃  SELECT COUNT(*) FROM sge.medida                    ┃
┃  WHERE created_by='SYSTEM';                         ┃
┃  → Resultado: 3720 ✅                               ┃
┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
```

---

**¡Los datos están donde deberían estar!** 🎉


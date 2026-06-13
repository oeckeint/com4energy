# 📍 Ubicaciones de Código: Dónde se Persisten los Datos

## Rutas de Archivos Clave

### 🔴 Punto 1: Detección del Tipo de Archivo
**Archivo:** `c4e-ingestion-service/src/main/java/com/com4energy/processor/service/processing/MeasureFileTypeProcessor.java`

**Línea 204-212:** Mapeo de tipo → tabla de destino
```java
private String resolveDestinationStore(String measureKind) {
    return switch (measureKind) {
        case "F5" -> "medida_legacy";  // ← Tu archivo va aquí
        ...
    };
}
```

**Línea 134-146:** Log que ves en logs
```
"Archivo '{}' [{}]: {} registros (persistidas: {}, defectos: {}, ... destino: {})"
```

---

### 🟡 Punto 2: Entrada al Flujo de Persistencia
**Archivo:** `c4e-ingestion-service/src/main/java/com/com4energy/processor/service/processing/MeasureFileTypeProcessor.java`

**Línea 100-106:** Llamada a persistencia
```java
MeasurePersistenceContracts.MeasurePersistenceResult persistenceResult = 
    measurePersistencePort.persist(
        new MeasurePersistenceContracts.PersistMeasuresCommand(...)
    );
```

---

### 🟢 Punto 3: LA PERSISTENCIA (El Corazón)
**Archivo:** `c4e-ingestion-service/src/main/java/com/com4energy/processor/service/measure/persistence/JpaMeasurePersistenceAdapter.java`

**Línea 44-91:** Método principal @Transactional
```java
@Transactional
public MeasurePersistenceContracts.MeasurePersistenceResult persist(...) {
    // Aquí es donde se insertan tus 3,720 registros
}
```

**Línea 59-88:** Loop principal
```java
for (MeasureRecord measureRecord : command.measureRecords()) {
    // Resuelve cliente
    // Clasifica por tipo
    // Acumula en lotes
    // Si lote >= 1000: flushBatches()
}
```

**Línea 100-126:** Flush de lotes
```java
private int flushBatches(...) {
    if (!f5Batch.isEmpty()) {
        // Tu lote F5 va aquí
        persistedCount += flushWithBinarySplit(f5Batch, medidaLegacyRepository, EntityType.F5);
        f5Batch.clear();
    }
    ...
}
```

**Línea 132-153:** Flush con binary split
```java
private <T> int flushWithBinarySplit(...) {
    try {
        repository.saveAll(new ArrayList<>(batch));  // ← INSERT en BD
        return batch.size();
    } catch (Exception e) {
        // Binary split recursivo si falla
        return binarySplitAndPersist(batch, repository, entityType, e.getMessage());
    }
}
```

---

### 🔵 Punto 4: Entidad Mapped a BD
**Archivo:** `c4e-ingestion-service/src/main/java/com/com4energy/processor/model/measure/MedidaLegacyEntity.java`

**Línea 18:** Mapeo a tabla BD
```java
@Entity
@Table(name = "medida")  // ← Tu tabla de destino
public class MedidaLegacyEntity {
```

**Línea 24-81:** Todas las columnas mapeadas
```
id_medida (Long) → PK AUTO_INCREMENT
id_cliente (Long) → FK
fecha (LocalDateTime)
ae1, as1, rq1-4 (Integer)
...
created_on, created_by (Audit)
```

---

### 🟣 Punto 5: Repositorio JPA
**Archivo:** `c4e-ingestion-service/src/main/java/com/com4energy/processor/repository/measure/MedidaLegacyRepository.java`

```java
public interface MedidaLegacyRepository extends JpaRepository<MedidaLegacyEntity, Long> {
    // Aquí JPA toma tus objetos y los convierte en:
    // INSERT INTO sge.medida (...) VALUES (...)
}
```

**Inyección en JpaMeasurePersistenceAdapter.java línea 36:**
```java
private final MedidaLegacyRepository medidaLegacyRepository;
```

**Uso en línea 142:**
```java
repository.saveAll(new ArrayList<>(batch));
```

---

### ⚫ Punto 6: Verificación en BD
**Herramienta:** MySQL

```bash
# Terminal:
mysql -u root -p sge -e "SELECT COUNT(*) FROM medida WHERE created_by='SYSTEM';"

# MySQL prompt:
mysql> SELECT COUNT(*) FROM sge.medida 
       WHERE DATE(created_on)=CURDATE() 
       AND created_by='SYSTEM';
```

---

## Flujo Completo de Código

### Entrada
1. **FileUploadController.java** línea X
   ```java
   @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
   public ResponseEntity<?> uploadFiles(MultipartFile[] files)
   ```

2. **FileUploadOrchestratorService.java**
   ```java
   processNewFile() → FileProcessingServiceImpl.processFile()
   ```

### Procesamiento
3. **MeasureFileTypeProcessor.java** (línea 58-198)
   - Parse (línea 61)
   - Validación (línea 93-96)
   - **Persistencia** (línea 100-106) ← CLAVE
   - Reportes (línea 172-176)

### Persistencia (La Caja Negra)
4. **JpaMeasurePersistenceAdapter.java** (línea 44-91) ← **AQUÍ SUCEDE LA MAGIA**
   ```
   - Resuelve clientes
   - Acumula en lotes
   - flushBatches() → flushWithBinarySplit()
   - repository.saveAll() → INSERT SQL
   ```

### Entity Mapping
5. **MedidaLegacyEntity.java**
   - `@Entity @Table(name="medida")`
   - Mapeo de campos
   - Hibernate genera SQL

### Base de Datos
6. **sge.medida** ← Tu tabla destino
   - 3,720 filas insertadas
   - `created_by = "SYSTEM"`
   - `created_on = NOW()`

---

## Configuración

**Conexión a BD:**
- **Archivo config:** `c4e-ingestion-service/src/main/resources/application.yml` línea 9-13
  ```yaml
  spring:
    datasource:
      url: ${DB_URL_SGE}
      username: ${DB_USER_SGE}
      password: ${DB_PASSWORD_SGE}
      driver-class-name: com.mysql.cj.jdbc.Driver
  ```

- **Variables de entorno:**
  ```bash
  DB_URL_SGE=jdbc:mysql://localhost:3306/sge
  DB_USER_SGE=root
  DB_PASSWORD_SGE=<password>
  ```

---

## Tipo de Archivo → Entidad → Tabla

```
MedidaLegacyEntity
    │
    ├─ @Entity
    ├─ @Table(name="medida")
    └─ Mapeo de columnas
           │
           ▼
    MedidaLegacyRepository
           │
           ├─ extends JpaRepository<MedidaLegacyEntity, Long>
           └─ saveAll() → INSERT SQL
                  │
                  ▼
    sge.medida (tabla física en MySQL)
           │
           └─ 3,720 registros insertados ✅
```

---

## Quick Reference: Dónde Buscar Qué

| Pregunta | Archivo | Línea |
|----------|---------|-------|
| ¿Cómo se mapea F5 a tabla? | MeasureFileTypeProcessor.java | 204-212 |
| ¿Dónde se persiste? | JpaMeasurePersistenceAdapter.java | 44-91 |
| ¿Qué entidad es F5? | MedidaLegacyEntity.java | 1-81 |
| ¿Qué tabla BD? | MedidaLegacyEntity.java | 18 |
| ¿Cómo se inyecta repo? | JpaMeasurePersistenceAdapter.java | 36 |
| ¿Dónde se hace INSERT? | JpaMeasurePersistenceAdapter.java | 142 |
| ¿Config BD? | application.yml | 9-13 |
| ¿Dónde verificar? | MySQL | sge.medida |

---

## Breakpoints Para Debug

Si quieres debuggear el proceso, pon breakpoints aquí:

1. **MeasureFileTypeProcessor.process()** línea 58
   - Inicio del procesamiento

2. **JpaMeasurePersistenceAdapter.persist()** línea 45
   - Inicio de persistencia

3. **JpaMeasurePersistenceAdapter.flushBatches()** línea 116
   - Punto donde se ejecuta INSERT

4. **MedidaLegacyRepository.saveAll()** (dentro de Hibernate)
   - Conversión a SQL

---

## Logs Relevantes

**Cuando ves este log:**
```
Archivo 'F5D_0031_0894_20250311.0' [MEDIDA_QH_F5]: ... persistidas: 3720 ... destino: medida_legacy
```

**Significa:**
- Generado en: `MeasureFileTypeProcessor.java` línea 134-146
- 3,720 registros ya fueron persistidos en BD
- Ubicación: tabla `sge.medida`
- Verificable con: `SELECT COUNT(*) FROM sge.medida WHERE created_by='SYSTEM'`

---

## Resumen

🎯 **Tus datos están acá:**
```
c4e-ingestion-service/
  ├─ src/main/java/.../
  │   ├─ MeasureFileTypeProcessor.java        [DETECTA tipo F5]
  │   └─ JpaMeasurePersistenceAdapter.java    [INSERTA en BD] ✅
  │
  └─ src/main/resources/
      └─ application.yml                       [CONFIG BD]
           ↓
        sge.medida (MySQL tabla física)        [DESTINO FINAL]
```

**Verifica con:**
```sql
SELECT COUNT(*) FROM sge.medida WHERE created_by='SYSTEM';
-- Respuesta esperada: 3720 ✅
```


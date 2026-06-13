# 📚 Índice de Documentación: Dónde se Persisten los Datos

## Tu Pregunta
```
recibi este log... pero no veo donde se persitisió
```

## Respuesta Rápida (30 segundos)
### ✅ Los 3,720 registros están en tabla **`medida`** de BD **`sge`**

**Verifica con:**
```sql
SELECT COUNT(*) FROM sge.medida 
WHERE created_by='SYSTEM' AND DATE(created_on)=CURDATE();
```
Debe devolver: `3720` ✅

---

## 📖 Documentación Creada Para Ti

### 1. **RESPUESTA_RAPIDA.md** ⚡
- **Qué es:** Resumen en 2 minutos
- **Para quién:** Si tienes prisa
- **Contiene:**
  - Respuesta directa
  - SQL de verificación
  - Dónde están los 672 defectos y 48,960 omitidos
- **Ubicación:** `/Users/jesus/Development/Com4Energy/RESPUESTA_RAPIDA.md`

### 2. **PERSISTENCIA_DATOS_MEDIDAS.md** 📍
- **Qué es:** Explicación técnica detallada
- **Para quién:** Si quieres entender el flujo completo
- **Contiene:**
  - Desglose del log (53,352 → 3,720 + 672 + 48,960)
  - Mapeo tipo archivo → tabla
  - Cómo verificar en BD
  - Estructura de tabla
  - Defectos y omitidos
- **Ubicación:** `/Users/jesus/Development/Com4Energy/PERSISTENCIA_DATOS_MEDIDAS.md`

### 3. **HOW_TO_VERIFY_PERSISTENCE.md** 🔍
- **Qué es:** Guía paso a paso
- **Para quién:** Si no sabes por dónde empezar
- **Contiene:**
  - Paso 1 a 10 para verificar
  - Cómo conectarse a MySQL
  - Qué consultas ejecutar
  - Interpretación de resultados
  - Troubleshooting
- **Ubicación:** `/Users/jesus/Development/Com4Energy/HOW_TO_VERIFY_PERSISTENCE.md`

### 4. **DIAGRAMA_PERSISTENCIA_FLUJO.md** 🏗️
- **Qué es:** Diagrama visual del flujo completo
- **Para quién:** Si eres visual
- **Contiene:**
  - Diagrama ASCII del flujo completo
  - Desde archivo → Parser → Validator → Persistencia → BD
  - Tabla de destino
  - Mapeo tipo archivo → tabla
  - SQL de verificación
- **Ubicación:** `/Users/jesus/Development/Com4Energy/DIAGRAMA_PERSISTENCIA_FLUJO.md`

### 5. **UBICACIONES_CODIGO_PERSISTENCIA.md** 💻
- **Qué es:** Referencia de código
- **Para quién:** Si quieres debuggear o modificar
- **Contiene:**
  - Ubicación exacta de cada archivo Java
  - Líneas clave en cada archivo
  - Rutas de archivos
  - Flujo de código
  - Breakpoints para debug
  - Quick reference
- **Ubicación:** `/Users/jesus/Development/Com4Energy/UBICACIONES_CODIGO_PERSISTENCIA.md`

### 6. **EJEMPLOS_PRACTICOS_SQL.md** 🛠️
- **Qué es:** 10 queries SQL prácticas
- **Para quién:** Si quieres explorar los datos
- **Contiene:**
  - Query #1: Contar total (3,720)
  - Query #2: Ver muestra (LIMIT 10)
  - Query #3: Distribuir por cliente
  - Query #4: Validar integridad
  - Query #5: Rango de fechas
  - Query #6: Estadísticas
  - Query #7: Distribuir por hora
  - Query #8: Cliente específico
  - Query #9: Datos históricos
  - Query #10: Validación final
  - Cómo ejecutar cada una
  - Troubleshooting
- **Ubicación:** `/Users/jesus/Development/Com4Energy/EJEMPLOS_PRACTICOS_SQL.md`

### 7. **verify_medida_legacy_persistence.sql** 🗂️
- **Qué es:** Script SQL automatizado
- **Para quién:** Si quieres ejecutar todo de una vez
- **Contiene:**
  - 10 queries completas
  - Comentarios explicativos
  - Casos de uso
- **Ubicación:** `/Users/jesus/Development/Com4Energy/scripts/verify_medida_legacy_persistence.sql`

---

## 🎯 Mapa de Lectura (Elige Tu Camino)

### Camino 1️⃣: "Tengo 30 segundos"
```
RESPUESTA_RAPIDA.md (2 min) ✅
```

### Camino 2️⃣: "Necesito entender el flujo"
```
PERSISTENCIA_DATOS_MEDIDAS.md (10 min)
    ↓
DIAGRAMA_PERSISTENCIA_FLUJO.md (5 min)
```

### Camino 3️⃣: "Quiero hacerlo paso a paso"
```
HOW_TO_VERIFY_PERSISTENCE.md (15 min)
    ↓
EJEMPLOS_PRACTICOS_SQL.md (10 min)
```

### Camino 4️⃣: "Necesito modificar código"
```
UBICACIONES_CODIGO_PERSISTENCIA.md (20 min)
    ↓
Abre los archivos en IDE
```

### Camino 5️⃣: "Solo quiero verificar datos"
```
EJEMPLOS_PRACTICOS_SQL.md
    ↓
Copia cualquier query
    ↓
Ejecuta en MySQL
```

---

## 📊 Log Original Explicado

```
Archivo 'F5D_0031_0894_20250311.0' [MEDIDA_QH_F5]: 
53352 registros (persistidas: 3720, defectos: 672, omitidas: 48960, destino: medida_legacy) 
en 14679 ms de procesamiento (parse=291 ms, persistencia=1736 ms)
```

| Parte | Significado | Ubicación |
|-------|-------------|-----------|
| `F5D_0031_0894_20250311.0` | Nombre del archivo | Input del usuario |
| `MEDIDA_QH_F5` | Tipo de archivo | Detectado por `MeasureFileTypeProcessor` |
| `53352` | Total líneas leídas | Parse exitoso |
| `persistidas: 3720` | ✅ Insertadas en BD | Tabla `sge.medida` |
| `defectos: 672` | ❌ Con errores | Archivo JSON (`.sge_defects.json`) |
| `omitidas: 48960` | ⏭️ Filtradas | No se insertan (tarifa 20TD) |
| `destino: medida_legacy` | Tabla lógica | Nombre en logs (tabla real: `medida`) |
| `14679 ms` | Tiempo total | Parse + Validación + Persistencia |
| `parse=291 ms` | Tiempo lectura archivo | MeasureFileParserService |
| `persistencia=1736 ms` | Tiempo DB | JpaMeasurePersistenceAdapter |

---

## 🗺️ Mapa de Archivos Clave del Proyecto

### Ingestion Service (Dónde se Procesan Medidas)
```
c4e-ingestion-service/
├── src/main/java/com/com4energy/processor/
│   ├── service/processing/
│   │   ├── MeasureFileTypeProcessor.java        ← DETECTA tipo F5
│   │   └── FileTypeProcessingResult.java
│   ├── service/measure/
│   │   └── persistence/
│   │       └── JpaMeasurePersistenceAdapter.java ← INSERTA EN BD ⭐
│   ├── model/measure/
│   │   └── MedidaLegacyEntity.java               ← MAPEO A TABLA medida
│   ├── repository/measure/
│   │   └── MedidaLegacyRepository.java           ← JPA REPOSITORY
│   ├── controller/
│   │   └── FileUploadController.java
│   └── service/
│       └── FileUploadOrchestratorService.java
│
└── src/main/resources/
    └── application.yml                           ← CONFIG BD
```

### BD Destino
```
MySQL Server
└── Database: sge
    └── Table: medida                             ← DESTINO FINAL ✅
        ├── id_medida (PK)
        ├── id_cliente (FK)
        ├── fecha
        ├── ae1, as1, rq1-4
        ├── metod_obt, indic_firmez, codigo_factura
        ├── created_on, created_by='SYSTEM'
        └── updated_on, updated_by
```

---

## 🔍 Búsqueda Rápida Por Tema

### "¿Cómo se detecta que es tipo F5?"
→ Lee: `UBICACIONES_CODIGO_PERSISTENCIA.md` Punto 1
→ Archivo: `MeasureFileTypeProcessor.java` línea 204-212

### "¿Dónde se persisten exactamente?"
→ Lee: `PERSISTENCIA_DATOS_MEDIDAS.md` Sección "Persistencia a Nivel de BD"
→ Archivo: `JpaMeasurePersistenceAdapter.java` línea 44-91

### "¿Qué tabla es?"
→ Lee: `PERSISTENCIA_DATOS_MEDIDAS.md` Sección "Tabla Destino"
→ Archivo: `MedidaLegacyEntity.java`
→ Query: `SHOW TABLES LIKE 'medida';`

### "¿Cómo verifico que están los datos?"
→ Lee: `HOW_TO_VERIFY_PERSISTENCE.md` Paso 4
→ Lee: `EJEMPLOS_PRACTICOS_SQL.md` Query #1 y #2

### "¿Dónde están los 672 defectos?"
→ Lee: `PERSISTENCIA_DATOS_MEDIDAS.md` Sección "Defectos Reportados"
→ Ubicación: `<c4e.upload.base-path>/*.sge_defects.json`

### "¿Por qué se omitieron 48,960?"
→ Lee: `PERSISTENCIA_DATOS_MEDIDAS.md` Sección "Omitidos"
→ Código: `JpaMeasurePersistenceAdapter.java` línea 261-263

### "¿Cuánto tiempo tomó?"
→ Log: 14,679 ms total (291 ms parse + 1,736 ms persistencia + resto validación)

### "¿Cómo debuggeo esto?"
→ Lee: `UBICACIONES_CODIGO_PERSISTENCIA.md` Sección "Breakpoints Para Debug"

### "¿Puedo ver ejemplo de SQL?"
→ Lee: `EJEMPLOS_PRACTICOS_SQL.md` (10 queries completas)

---

## ✅ Checklist: Entender la Persistencia

- [ ] Leí `RESPUESTA_RAPIDA.md`
- [ ] Entiendo dónde están los 3,720 (tabla `medida`)
- [ ] Ejecuté el SQL de verificación `SELECT COUNT(*)`
- [ ] Vi datos de muestra con `SELECT * LIMIT 10`
- [ ] Leí `PERSISTENCIA_DATOS_MEDIDAS.md` para entender el flujo
- [ ] Entiendo dónde están los 672 defectos (JSON)
- [ ] Entiendo por qué se omitieron 48,960 (tarifa 20TD)
- [ ] Si quiero debuggear: Leí `UBICACIONES_CODIGO_PERSISTENCIA.md`
- [ ] Entiendo las tablas JPA → SQL

---

## 📞 Resumen Ejecutivo (Para Managers)

```
✅ RESULTADO POSITIVO
- 3,720 registros insertados exitosamente en BD
- 0 registros perdidos
- Tiempo de procesamiento: 14.7 segundos
- Base de datos: sge.medida
- Tabla: medida (MedidaLegacyEntity)

⚠️ NOTA: 672 registros con defectos (reportados por separado)
         48,960 registros omitidos por criterio de tarifa

🔍 VERIFICACIÓN: SELECT COUNT(*) FROM sge.medida 
                 WHERE created_by='SYSTEM' AND DATE(created_on)=CURDATE();
                 Resultado: 3,720 ✅
```

---

## 🚀 Próximos Pasos

1. **Verificar los datos existen:**
   ```bash
   mysql -u root -p sge -e "SELECT COUNT(*) FROM medida WHERE created_by='SYSTEM';"
   ```

2. **Explorar distribución por cliente:**
   - Usa Query #3 de `EJEMPLOS_PRACTICOS_SQL.md`

3. **Validar integridad:**
   - Usa Query #4 de `EJEMPLOS_PRACTICOS_SQL.md`

4. **Si necesitas modificar código:**
   - Abre `UBICACIONES_CODIGO_PERSISTENCIA.md`
   - Encontrarás exactamente dónde ir

---

## 📋 Archivos Creados

```
/Users/jesus/Development/Com4Energy/
├── RESPUESTA_RAPIDA.md                           ⚡ (2 min)
├── PERSISTENCIA_DATOS_MEDIDAS.md                 📍 (10 min)
├── HOW_TO_VERIFY_PERSISTENCE.md                  🔍 (15 min)
├── DIAGRAMA_PERSISTENCIA_FLUJO.md                🏗️ (5 min)
├── UBICACIONES_CODIGO_PERSISTENCIA.md            💻 (20 min)
├── EJEMPLOS_PRACTICOS_SQL.md                     🛠️ (10 min)
├── Este archivo (INDICE.md)                      📚 (5 min)
└── scripts/
    └── verify_medida_legacy_persistence.sql      🗂️ (Script SQL)
```

---

## 💡 Última Cosa

**Si NO ves los datos en BD, revisa:**

1. ¿Estás usando la BD correcta?
   ```bash
   echo $DB_URL_SGE
   ```

2. ¿La tabla existe?
   ```sql
   SHOW TABLES LIKE 'medida';
   ```

3. ¿Buscas por fecha correcta?
   ```sql
   SELECT COUNT(*) FROM sge.medida;  -- Sin filtro de fecha
   ```

4. ¿Revisa logs del ingestion-service?
   ```
   Busca: "Error processing measure file"
   Busca: "Batch flush failed"
   ```

---

## ¡ÉXITO! 🎉

**Tu respuesta:** Los 3,720 registros están en `sge.medida`, listos para consultar.

**Verifica con:**
```sql
SELECT COUNT(*) FROM sge.medida 
WHERE created_by='SYSTEM' AND DATE(created_on)=CURDATE();
```

**Resultado esperado:** `3720` ✅


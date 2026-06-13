# 🔍 Instrucciones Paso a Paso: Verificar Persistencia de Datos

## Tu Log
```
Archivo 'F5D_0031_0894_20250311.0' [MEDIDA_QH_F5]: 
  53352 registros (persistidas: 3720, defectos: 672, omitidas: 48960, destino: medida_legacy) 
  en 14679 ms de procesamiento (parse=291 ms, persistencia=1736 ms)
```

---

## ¿Dónde Fueron los 3,720 Registros?

### **Respuesta Corta**
→ A la tabla **`medida`** en la base de datos **`sge`** (MySQL)

### **Respuesta Técnica**
```
c4e-ingestion-service
    ↓
MeasureFileTypeProcessor (detecta MEDIDA_QH_F5)
    ↓
JpaMeasurePersistenceAdapter.persist()
    ↓
MedidaLegacyRepository.saveAll()
    ↓
INSERT INTO sge.medida (id_cliente, fecha, ae1, as1, rq1, ...)
```

---

## Paso 1️⃣: Identificar Tu Base de Datos

El **ingestion-service** se conecta usando variables de entorno:

```bash
# Desde tu terminal, busca estas variables:
echo $DB_URL_SGE
echo $DB_USER_SGE
echo $DB_PASSWORD_SGE
```

**Ejemplo esperado:**
```
DB_URL_SGE=jdbc:mysql://localhost:3306/sge
DB_USER_SGE=root
DB_PASSWORD_SGE=mypassword
```

Si no ves nada, está en `application.yml` (variable por defecto: vacía, usa credenciales locales)

---

## Paso 2️⃣: Conectar a MySQL

```bash
# Opción A: Desde terminal
mysql -h localhost -u root -p

# Opción B: Con credential file
mysql --defaults-file=/path/to/.my.cnf sge
```

### **En Docker** (si usas docker-compose):
```bash
docker exec -it <mysql-container> mysql -u root -p sge
```

---

## Paso 3️⃣: Verificar Tabla Existe

Dentro de MySQL:

```sql
-- Listar todas las tablas del esquema sge
USE sge;
SHOW TABLES;

-- Verificar que "medida" existe
DESC medida;
```

**Output esperado:**
```
Field               Type        Null  Key  Default  Extra
id_medida           bigint      NO    PRI  NULL    auto_increment
id_cliente          bigint      NO    MUL  NULL
fecha               datetime    NO         NULL
bandera_inv_ver     int         YES        NULL
ae1                 int         YES        NULL
as1                 int         YES        NULL
...
```

---

## Paso 4️⃣: Consulta Simple (La Más Rápida)

```sql
-- Contar cuántos registros hay de hoy
SELECT COUNT(*) 
FROM sge.medida 
WHERE DATE(created_on) = CURDATE()
  AND created_by = 'SYSTEM';
```

**Resultado esperado**: `3720` (o similar)

---

## Paso 5️⃣: Ver Muestra de Datos

```sql
-- Ver los primeros 10 registros insertados hoy
SELECT 
    id_medida,
    id_cliente,
    fecha,
    ae1,
    as1,
    rq1,
    created_on,
    created_by
FROM sge.medida 
WHERE DATE(created_on) = CURDATE()
  AND created_by = 'SYSTEM'
ORDER BY id_medida ASC
LIMIT 10;
```

**Resultado esperado**: Verás 10 filas con datos reales de medidas

---

## Paso 6️⃣: Validar Cantidad Exacta

```sql
-- Contar exactamente 3,720
SELECT 
    COUNT(*) as total_insertados,
    CASE 
        WHEN COUNT(*) = 3720 THEN '✅ EXACTO: 3,720'
        WHEN COUNT(*) > 3720 THEN '⚠️ MÁS: ' || COUNT(*)
        WHEN COUNT(*) < 3720 THEN '❌ MENOS: ' || COUNT(*)
    END as estado
FROM sge.medida 
WHERE DATE(created_on) = CURDATE()
  AND created_by = 'SYSTEM';
```

---

## Paso 7️⃣: Validar Integridad

```sql
-- Verificar que NO hay NULOS en campos obligatorios
SELECT 
    COUNT(*) as con_nulos_en_cliente
FROM sge.medida
WHERE id_cliente IS NULL
  AND DATE(created_on) = CURDATE()
  AND created_by = 'SYSTEM';

-- Debería retornar: 0

-- Verificar que TODAS las fechas son válidas
SELECT 
    COUNT(*) as con_nulos_en_fecha
FROM sge.medida
WHERE fecha IS NULL
  AND DATE(created_on) = CURDATE()
  AND created_by = 'SYSTEM';

-- Debería retornar: 0
```

---

## Paso 8️⃣: Ver Por Cliente

```sql
-- Distribuir 3,720 registros por cliente
SELECT 
    id_cliente,
    COUNT(*) as registros_por_cliente
FROM sge.medida
WHERE DATE(created_on) = CURDATE()
  AND created_by = 'SYSTEM'
GROUP BY id_cliente
ORDER BY COUNT(*) DESC;
```

**Resultado esperado**: Verás algo así:
```
id_cliente    registros_por_cliente
1             1200
5             800
10            720
...
```

---

## Paso 9️⃣: Dónde Están los 672 Defectos

Los **672 registros con defectos** están en un **archivo JSON**, no en BD:

```bash
# Ubicación esperada:
ls -la /Users/jesus/Downloads/com4energy/ingestion-service/*/F5D_0031_0894_20250311.0*

# Podrías ver:
# F5D_0031_0894_20250311.0.sge_defects.json
```

Abre ese archivo para ver qué errores tuvo cada registro.

---

## 🔟 Dónde Están los 48,960 Omitidos

Los **48,960 registros omitidos** fueron filtrados automáticamente por:
- **Tarifa = "20TD"** → Se descartan antes de la BD

**No aparecen en BD porque:**
```java
// En JpaMeasurePersistenceAdapter.java, línea 261-263
private boolean is20TdTariff(String tarifa) {
    return tarifa != null && "20TD".equalsIgnoreCase(tarifa.trim());
}

// Si es 20TD → skipped++  (no se inserta)
```

---

## 📊 Resumen Final

| Concepto | Cantidad | Ubicación |
|----------|----------|-----------|
| **Persistidas** | 3,720 | BD: `sge.medida` |
| **Defectos** | 672 | Archivo: `.sge_defects.json` |
| **Omitidas** | 48,960 | Ninguno (filtradas) |
| **TOTAL ARCHIVO** | 53,352 | - |

---

## 🚨 Si No Encuentras los Datos

### **Problema 1: No conexta a MySQL**
```bash
# Verifica que MySQL está corriendo
ps aux | grep mysql

# O si es Docker:
docker ps | grep mysql
```

### **Problema 2: Tabla no existe**
```sql
-- Verifica el schema
SHOW SCHEMAS LIKE 'sge';

-- Si no existe, crea manualmente:
CREATE DATABASE sge;

-- O ejecuta migrations
./mvnw liquibase:update
```

### **Problema 3: No hay datos de hoy**
```sql
-- Busca en cualquier fecha
SELECT COUNT(*) FROM sge.medida LIMIT 1;

-- O de la semana pasada
SELECT COUNT(*) 
FROM sge.medida 
WHERE DATE(created_on) >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
  AND created_by = 'SYSTEM';
```

### **Problema 4: BD incorrecta**
```sql
-- Verifica qué BD estás usando
SELECT DATABASE();

-- Verifica que DB_URL_SGE apunta a la correcta
env | grep DB_URL_SGE
```

---

## 💾 Script Automatizado

He creado un script SQL completo en:
```
/Users/jesus/Development/Com4Energy/scripts/verify_medida_legacy_persistence.sql
```

**Úsalo así:**
```bash
# Opción 1: Copia y pega en MySQL
mysql -u root -p sge < /Users/jesus/Development/Com4Energy/scripts/verify_medida_legacy_persistence.sql

# Opción 2: Desde MySQL prompt
mysql> source /Users/jesus/Development/Com4Energy/scripts/verify_medida_legacy_persistence.sql;
```

---

## 📝 Resumen: El Flujo Completo

```
1. Archivo F5D_0031_0894_20250311.0 sube a ingestion-service
                    ↓
2. MeasureFileTypeProcessor lo detecta como MEDIDA_QH_F5
                    ↓
3. Se parsea: 53,352 líneas
                    ↓
4. Se validan contra cliente (CUPS → id_cliente lookup)
                    ↓
5. 48,960 se descartan (tarifa 20TD)
                    ↓
6. 4,392 avanzan a persistencia
                    ↓
7. De esos, 672 fallan validación (se guardan en JSON)
                    ↓
8. Los 3,720 exitosos se insertan en lotes de 1,000
                    ↓
9. Transacción se confirma en BD
                    ↓
10. Se publica evento al outbox (FILE_DEFECT_REPORT_CREATED, etc.)
                    ↓
11. El evento se replica a records-api (auditoría)

✅ RESULTADO: 3,720 registros en tabla medida
```

---

## ✅ Conclusión

🎯 **Tu respuesta: Los datos están en `sge.medida`**

Para confirmar:
```bash
mysql -u root -p sge -e "SELECT COUNT(*) FROM medida WHERE created_by='SYSTEM' AND DATE(created_on)=CURDATE();"
```

Si devuelve `3720` → **¡Están todos ahí!** ✅


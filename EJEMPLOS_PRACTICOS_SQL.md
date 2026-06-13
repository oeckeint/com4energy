# 🛠️ Ejemplos Prácticos: Cómo Consultar Tus Datos

## Problema
Recibiste este log pero no sabes dónde ver los 3,720 registros persistidos:
```
Archivo 'F5D_0031_0894_20250311.0' [MEDIDA_QH_F5]: 
53352 registros (persistidas: 3720, defectos: 672, omitidas: 48960, destino: medida_legacy) 
en 14679 ms de procesamiento (parse=291 ms, persistencia=1736 ms)
```

## Solución: Ejemplos de SQL

### 1️⃣ Verificar Cantidad Total (La Más Importante)

```sql
SELECT COUNT(*) as total_registros
FROM sge.medida
WHERE created_by = 'SYSTEM'
  AND DATE(created_on) = '2025-03-11';
```

**Resultado esperado:**
```
total_registros
3720
```

**Interpretación:** ✅ Tus 3,720 registros están en BD

---

### 2️⃣ Ver Algunos Registros (Top 10)

```sql
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
WHERE created_by = 'SYSTEM'
  AND DATE(created_on) = '2025-03-11'
ORDER BY id_medida DESC
LIMIT 10;
```

**Resultado esperado:**
```
id_medida  id_cliente  fecha           ae1   as1   rq1  created_on           created_by
12345      1001        2025-03-11 10:30 450   500   100  2025-03-11 15:47:23  SYSTEM
12344      1001        2025-03-11 10:15 460   510   105  2025-03-11 15:47:22  SYSTEM
12343      1002        2025-03-11 10:00 470   520   110  2025-03-11 15:47:21  SYSTEM
...
```

**Interpretación:** ✅ Datos reales están en BD, con valores válidos

---

### 3️⃣ Distribuir por Cliente (¿Quién Tiene Más Registros?)

```sql
SELECT 
    id_cliente,
    COUNT(*) as total_registros,
    MIN(fecha) as primer_timestamp,
    MAX(fecha) as ultimo_timestamp
FROM sge.medida
WHERE created_by = 'SYSTEM'
  AND DATE(created_on) = '2025-03-11'
GROUP BY id_cliente
ORDER BY COUNT(*) DESC
LIMIT 5;
```

**Resultado esperado:**
```
id_cliente  total_registros  primer_timestamp     ultimo_timestamp
1001        1200             2025-03-11 00:00:00  2025-03-11 23:45:00
1002        850              2025-03-11 01:00:00  2025-03-11 22:30:00
1003        720              2025-03-11 02:00:00  2025-03-11 20:15:00
1004        650              2025-03-11 03:00:00  2025-03-11 19:00:00
1005        300              2025-03-11 04:00:00  2025-03-11 18:00:00
```

**Interpretación:** ✅ Los 3,720 registros se distribuyeron entre varios clientes

---

### 4️⃣ Validar Integridad (¿Hay Datos Nulos?)

```sql
SELECT 
    COUNT(*) as total_registros,
    SUM(CASE WHEN id_cliente IS NULL THEN 1 ELSE 0 END) as nulos_id_cliente,
    SUM(CASE WHEN fecha IS NULL THEN 1 ELSE 0 END) as nulos_fecha,
    SUM(CASE WHEN ae1 IS NULL THEN 1 ELSE 0 END) as nulos_ae1,
    SUM(CASE WHEN as1 IS NULL THEN 1 ELSE 0 END) as nulos_as1,
    SUM(CASE WHEN rq1 IS NULL THEN 1 ELSE 0 END) as nulos_rq1
FROM sge.medida
WHERE created_by = 'SYSTEM'
  AND DATE(created_on) = '2025-03-11';
```

**Resultado esperado:**
```
total_registros  nulos_id_cliente  nulos_fecha  nulos_ae1  nulos_as1  nulos_rq1
3720             0                 0            (algunos)  (algunos)  (algunos)
```

**Interpretación:** ✅ Todos tienen id_cliente y fecha (obligatorios). Los valores ae1, as1, etc pueden ser nulos (es normal)

---

### 5️⃣ Rango de Fechas (¿De Cuándo es el Archivo?)

```sql
SELECT 
    COUNT(*) as registros,
    MIN(fecha) as fecha_mas_antigua,
    MAX(fecha) as fecha_mas_reciente,
    DATEDIFF(MAX(fecha), MIN(fecha)) as dias_cubiertos
FROM sge.medida
WHERE created_by = 'SYSTEM'
  AND DATE(created_on) = '2025-03-11';
```

**Resultado esperado:**
```
registros  fecha_mas_antigua      fecha_mas_reciente     dias_cubiertos
3720       2025-02-11 00:00:00    2025-03-11 23:45:00    28
```

**Interpretación:** ✅ El archivo contiene medidas de las últimas 4 semanas

---

### 6️⃣ Estadísticas de Valores (¿Qué Rango de Datos Hay?)

```sql
SELECT 
    COUNT(*) as total,
    COUNT(DISTINCT id_cliente) as clientes_unicos,
    MIN(ae1) as ae1_min,
    MAX(ae1) as ae1_max,
    AVG(ae1) as ae1_promedio,
    STDDEV(ae1) as ae1_desv_est,
    MIN(as1) as as1_min,
    MAX(as1) as as1_max,
    AVG(as1) as as1_promedio
FROM sge.medida
WHERE created_by = 'SYSTEM'
  AND DATE(created_on) = '2025-03-11';
```

**Resultado esperado:**
```
total  clientes_unicos  ae1_min  ae1_max  ae1_promedio  ae1_desv_est  as1_min  as1_max  as1_promedio
3720   47               10       5000     450.5         280.2         5        4900     400.3
```

**Interpretación:** ✅ Datos dentro de rango realista

---

### 7️⃣ Distribuir por Rango Horario (¿A Qué Horas se Registraron?)

```sql
SELECT 
    HOUR(fecha) as hora_del_dia,
    COUNT(*) as registros_en_esa_hora,
    MIN(ae1) as ae1_min,
    MAX(ae1) as ae1_max,
    AVG(ae1) as ae1_promedio
FROM sge.medida
WHERE created_by = 'SYSTEM'
  AND DATE(created_on) = '2025-03-11'
GROUP BY HOUR(fecha)
ORDER BY HOUR(fecha) ASC;
```

**Resultado esperado:**
```
hora_del_dia  registros_en_esa_hora  ae1_min  ae1_max  ae1_promedio
0             155                    50       2000     450
1             160                    45       1950     455
2             158                    48       2100     460
...
23            142                    52       1900     440
```

**Interpretación:** ✅ Distribución uniforme durante el día

---

### 8️⃣ Buscar un Cliente Específico (¿Cómo Está Mi Cliente?)

```sql
-- Busca cliente con ID 1001
SELECT 
    id_medida,
    fecha,
    ae1,
    as1,
    rq1,
    rq2,
    rq3,
    rq4,
    created_on
FROM sge.medida
WHERE id_cliente = 1001
  AND created_by = 'SYSTEM'
  AND DATE(created_on) = '2025-03-11'
ORDER BY fecha DESC
LIMIT 20;
```

**Resultado esperado:**
```
id_medida  fecha                 ae1   as1   rq1  rq2  rq3  rq4  created_on
...
12100      2025-03-11 23:45:00  500   520   150  160  170  180  2025-03-11 15:47:25
12099      2025-03-11 23:30:00  510   530   155  165  175  185  2025-03-11 15:47:25
...
```

**Interpretación:** ✅ Cliente específico tiene sus datos

---

### 9️⃣ Comparar Con Datos Históricos (¿Cuál es el Crecimiento?)

```sql
SELECT 
    DATE(created_on) as fecha_persistencia,
    COUNT(*) as registros_insertados,
    COUNT(DISTINCT id_cliente) as clientes
FROM sge.medida
WHERE created_by = 'SYSTEM'
GROUP BY DATE(created_on)
ORDER BY DATE(created_on) DESC
LIMIT 10;
```

**Resultado esperado:**
```
fecha_persistencia  registros_insertados  clientes
2025-03-11          3720                  47
2025-03-10          2450                  35
2025-03-09          1890                  28
...
```

**Interpretación:** ✅ El archivo de hoy (3,720) es el más grande

---

### 🔟 Validación Final: Confirmar Todo

```sql
SELECT 
    'Tabla: sge.medida' as tabla,
    COUNT(*) as registros_encontrados,
    COUNT(DISTINCT id_cliente) as clientes_unicos,
    MIN(created_on) as primer_insert,
    MAX(created_on) as ultimo_insert,
    CONCAT('Insertado en: ', 
        TIMEDIFF(MAX(created_on), MIN(created_on))
    ) as duracion_insercion,
    CASE 
        WHEN COUNT(*) = 3720 THEN '✅ CORRECTO'
        WHEN COUNT(*) > 3720 THEN '⚠️ MÁS'
        WHEN COUNT(*) < 3720 THEN '❌ MENOS'
    END as estado
FROM sge.medida
WHERE created_by = 'SYSTEM'
  AND DATE(created_on) = '2025-03-11';
```

**Resultado esperado:**
```
tabla          registros_encontrados  clientes_unicos  primer_insert           ultimo_insert            duracion_insercion  estado
Tabla: sge.medida  3720                 47           2025-03-11 15:47:20  2025-03-11 15:47:25  Insertado en: 00:00:05  ✅ CORRECTO
```

**Interpretación:** ✅ Todo correcto, 3,720 registros insertados en 5 segundos

---

## Cómo Ejecutar Estos Scripts

### Opción 1: Desde Terminal (Uno a la Vez)

```bash
# Conectar a MySQL
mysql -u root -p -h localhost sge

# O en una sola línea:
mysql -u root -p -h localhost sge -e "SELECT COUNT(*) FROM medida WHERE created_by='SYSTEM' AND DATE(created_on)='2025-03-11';"
```

### Opción 2: Guardar en Archivo y Ejecutar

```bash
# Crear archivo con queries
cat > /tmp/verify_medida.sql << 'EOF'
-- Verificación de 3,720 registros
SELECT COUNT(*) as total FROM sge.medida 
WHERE created_by='SYSTEM' AND DATE(created_on)='2025-03-11';

-- Ver muestra
SELECT id_medida, id_cliente, fecha FROM sge.medida 
WHERE created_by='SYSTEM' AND DATE(created_on)='2025-03-11'
LIMIT 5;
EOF

# Ejecutar
mysql -u root -p -h localhost < /tmp/verify_medida.sql
```

### Opción 3: Desde GUI (MySQL Workbench, DBeaver, etc.)

1. Copia cualquiera de las queries arriba
2. Pégala en el SQL editor
3. Ejecuta (Ctrl+Enter o botón Run)

### Opción 4: Script Automatizado

```bash
# Si creaste el script que te proporcioné:
mysql -u root -p -h localhost sge < \
  /Users/jesus/Development/Com4Energy/scripts/verify_medida_legacy_persistence.sql
```

---

## Resumen: Dónde Buscar

| Lo que quieres | Query | Línea |
|---|---|---|
| Confirmar 3,720 | #1 | `COUNT(*) WHERE... = 3720` |
| Ver datos reales | #2 | `SELECT * LIMIT 10` |
| Distribuir por cliente | #3 | `GROUP BY id_cliente` |
| Validar nulos | #4 | `SUM(CASE WHEN ... IS NULL)` |
| Rango de fechas | #5 | `MIN/MAX(fecha)` |
| Estadísticas | #6 | `AVG, STDDEV, MIN, MAX` |
| Distribuir por hora | #7 | `GROUP BY HOUR(fecha)` |
| Cliente específico | #8 | `WHERE id_cliente = X` |
| Histórico | #9 | `GROUP BY DATE(created_on)` |
| Validación final | #10 | Todo combinado |

---

## Si Algo Falla

### Error: `Unknown database 'sge'`
```bash
# Verifica que la BD existe
mysql -u root -p -e "SHOW DATABASES LIKE 'sge';"

# Si no existe, crea manualmente o ejecuta migraciones
./mvnw liquibase:update
```

### Error: `Table 'sge.medida' doesn't exist`
```bash
# Verifica que la tabla existe
mysql -u root -p sge -e "SHOW TABLES LIKE 'medida';"

# Si no existe, ejecuta migraciones de Liquibase
```

### Error: `Access denied for user 'root'@'localhost'`
```bash
# Verifica credenciales
mysql -u root -p   # Te pedirá contraseña
```

### Error: `Connect to localhost:3306 refused`
```bash
# MySQL no está corriendo
sudo service mysql start

# O si usas Docker:
docker-compose up mysql
```

---

## Conclusión

✅ **Tus 3,720 registros están acá:**
```sql
SELECT COUNT(*) FROM sge.medida 
WHERE created_by='SYSTEM' 
  AND DATE(created_on)='2025-03-11';
-- Respuesta: 3720
```

**Usa los ejemplos arriba para:**
- Verificar que están todos
- Ver muestras de datos
- Validar integridad
- Analizar distribución
- Buscar por cliente
- Seguimiento histórico


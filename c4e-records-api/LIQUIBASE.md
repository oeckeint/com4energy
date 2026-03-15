# 📦 Liquibase - Gestión de Migraciones con Formato SQL

## 📋 Descripción General

**Liquibase** es una herramienta de código abierto para gestionar cambios de esquema de base de datos de forma versionada y reproducible.

En este proyecto usamos **formato SQL** (no YAML) con integración a **Jira** para máxima trazabilidad.

---

## 🏗️ Estructura de Archivos

```
src/main/resources/db/changelog/
├── db.changelog-master.xml          # Archivo maestro (XML)
├── migrations/                       # Todas las migraciones SQL aquí
│   ├── 000_RA-011_test.sql          # Ejemplo: Migración de prueba
│   ├── 001_RA-012_create_table.sql  # Ejemplo: Crear tabla
│   ├── 002_RA-013_add_column.sql    # Ejemplo: Agregar columna
│   └── 003_RA-014_add_indexes.sql   # Ejemplo: Agregar índices
└── seeds/                            # Datos iniciales (opcional)
    └── 001_RA-12_seed.sql            # Datos de referencia
```

### Recomendación: Estructura Escalable

Para proyectos grandes, puedes organizar por categoría:

```
src/main/resources/db/changelog/
├── db.changelog-master.xml
├── migrations/
│   ├── auto/                         # Cambios AUTOMÁTICOS versionados
│   │   ├── 000_RA-011_test.sql
│   │   ├── 001_RA-012_create_tables.sql
│   │   ├── 002_RA-013_add_columns.sql
│   │   └── 003_RA-014_add_indexes.sql
│   └── manual/                       # Cambios MANUALES u ocasionales
│       ├── 100_RA-050_clean_duplicates.sql
│       └── 101_RA-051_backfill_data.sql
└── seeds/
    ├── 001_RA-100_reference_data.sql
    └── 002_RA-101_test_data.sql
```

**Ventajas:**
- Separar cambios estructurales de scripts ocasionales
- Claridad visual del tipo de cambio
- Facilita auditoría

**Actualizar db.changelog-master.xml:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog>
    <!-- Cambios automáticos/versionados -->
    <includeAll path="migrations/auto" relativeToChangelogFile="true" />
    
    <!-- Scripts ocasionales/manuales -->
    <includeAll path="migrations/manual" relativeToChangelogFile="true" />
    
    <!-- Datos iniciales -->
    <includeAll path="seeds" relativeToChangelogFile="true" />
</databaseChangeLog>
```

### Archivo Maestro (db.changelog-master.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="
        http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.4.xsd">

    <includeAll path="migrations" relativeToChangelogFile="true" />

</databaseChangeLog>
```

**Características:**
- ✅ Incluye automáticamente todos los `.sql` de la carpeta `migrations/`
- ✅ Los ejecuta en orden alfabético (por eso usamos prefijos numéricos)
- ✅ **NO necesitas modificar este archivo** al agregar migraciones
- ✅ Simple y efectivo

---

## 📝 Convención de Nombres de Archivos

### Formato Obligatorio

```
<orden>_<ticket>_<descripcion>.sql
```

**Componentes:**
- `<orden>` = Número secuencial (000, 001, 002, ...) para orden de ejecución
- `<ticket>` = ID del ticket de Jira (ej: RA-011, RA-012, RA-013)
- `<descripcion>` = Descripción breve en snake_case

### Ejemplos Válidos

```
✅ 000_RA-011_test.sql
✅ 001_RA-012_create_medidaqh_table.sql
✅ 002_RA-013_add_status_column.sql
✅ 003_RA-014_add_performance_indexes.sql
✅ 010_RA-025_insert_reference_data.sql
✅ 100_RA-150_add_cliente_table.sql
```

### Ejemplos NO Válidos

```
❌ create-table.sql              (sin orden ni ticket)
❌ RA-012_create_table.sql       (sin orden)
❌ 001_create_table.sql          (sin ticket)
❌ 001-RA-012-create.sql         (guiones en lugar de guiones bajos)
❌ 001_RA012_create.sql          (sin guión en ticket)
```

---

## 📋 Formato de Cada Archivo SQL

### Estructura Completa Obligatoria

```sql
--liquibase formatted sql

--changeset <autor>:<ticket>
--context:dev,qa,prod
--comment <titulo del ticket de Jira>

-- SQL statements aquí
CREATE TABLE ejemplo (
    id INT PRIMARY KEY,
    nombre VARCHAR(255)
);

--rollback <estrategia de rollback>
```

**Nota Importante:** Todos los archivos de migración **DEBEN estar en formato `.sql`** y ejecutarse **solo con XML** usando `includeAll` en el `db.changelog-master.xml`. NO se usan archivos YAML.

### Componentes Explicados

#### 1. Header de Liquibase (Línea 1)

```sql
--liquibase formatted sql
```

**Obligatorio:** DEBE ser la primera línea del archivo.  
**Propósito:** Indica a Liquibase que este archivo usa formato SQL formateado.  
**Sin esto:** Liquibase no procesará el archivo.

#### 2. Changeset (Línea 3)

```sql
--changeset <autor>:<ticket>
```

**Formato:** `autor:TICKET-JIRA`

**Ejemplos:**
```sql
--changeset jesus:RA-011
--changeset maria:RA-012
--changeset juan:RA-013
```

**Propósito:**
- Identifica unívocamente el changeset
- Relaciona el cambio con el ticket de Jira
- Permite trazabilidad completa

#### 3. Contextos (Línea 4)

```sql
--context:dev,qa,prod
```

**Propósito:** Define en qué ambientes se ejecuta este changeset.

**Formatos válidos:**
```sql
--context:dev,qa,prod        # En todos los ambientes
--context:dev,qa             # Solo en dev y qa
--context:prod               # Solo en producción
--context:development        # Solo en desarrollo
```

**Ejemplos de uso:**
```sql
-- Dato de prueba solo para desarrollo
--context:development
INSERT INTO configuracion VALUES ('test_key', 'test_value');

-- Cambio que va a producción
--context:prod
CREATE TABLE audit_log (...);

-- Disponible en todos lados
--context:dev,qa,prod
ALTER TABLE medidaqh ADD COLUMN estado VARCHAR(50);
```

**Configurar en application.properties:**
```properties
# En desarrollo
spring.liquibase.contexts=development

# En QA
spring.liquibase.contexts=dev,qa

# En producción
spring.liquibase.contexts=prod
```

#### 4. Comentario (Línea 5)

```sql
--comment <titulo del ticket de Jira>
```

**Contenido:** El título exacto del ticket de Jira

**Ejemplos:**
```sql
--comment Test de implementación de Liquibase
--comment Crear tabla principal para medidas cuarto-horarias
--comment Agregar columna de estado para tracking
```

**Propósito:**
- Documentar qué hace el changeset
- Relacionar con el ticket de Jira
- Facilitar auditoría

#### 5. SQL Statements (Líneas siguientes)

```sql
-- Tu código SQL aquí
CREATE TABLE ...
INSERT INTO ...
ALTER TABLE ...
```

**Puede ser:**
- CREATE TABLE
- ALTER TABLE
- CREATE INDEX
- INSERT INTO
- UPDATE
- DELETE
- Cualquier SQL válido de MySQL

#### 6. Estrategia de Rollback (Última línea)

```sql
--rollback <SQL para revertir el cambio>
```

**Propósito:** Define cómo deshacer este cambio si es necesario.

**Estrategias según el tipo de cambio:**

```sql
-- Para CREATE TABLE (reversible)
--rollback DROP TABLE medidaqh;

-- Para ALTER TABLE ADD COLUMN (reversible)
--rollback ALTER TABLE medidaqh DROP COLUMN estado;

-- Para CREATE INDEX (reversible)
--rollback DROP INDEX idx_medidaqh_cliente ON medidaqh;

-- Para INSERT/UPDATE (puede no ser reversible)
--rollback DELETE FROM configuracion WHERE clave = 'test';

-- Para cambios NO reversibles o no críticos
--rollback SELECT 1;
```

**Ejemplos en contexto:**

```sql
--liquibase formatted sql
--changeset jesus:RA-012
--comment Crear tabla principal para medidas

CREATE TABLE medidaqh (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    valor INT
);

--rollback DROP TABLE medidaqh;
```

**Rollback múltiple (en orden inverso):**
```sql
CREATE TABLE tabla1 (...);
CREATE TABLE tabla2 (...);
CREATE INDEX idx1 ON tabla1(col);

--rollback DROP INDEX idx1 ON tabla1;
--rollback DROP TABLE tabla2;
--rollback DROP TABLE tabla1;
```

---



## 📋 Ejemplos Completos

### Ejemplo 1: Crear Tabla

**Archivo:** `001_RA-012_create_medidaqh_table.sql`

```sql
--liquibase formatted sql

--changeset jesus:RA-012
--context:dev,qa,prod
--comment Crear tabla principal para medidas cuarto-horarias

CREATE TABLE medidaqh (
    id_medidaQH INT AUTO_INCREMENT PRIMARY KEY,
    id_cliente INT NOT NULL,
    tipomed INT NOT NULL,
    fecha DATETIME NOT NULL,
    bandera_inv_ver INT DEFAULT 0,
    actent INT,
    qactent INT,
    qactsal INT,
    r_q1 INT,
    qr_q1 INT,
    r_q2 INT,
    qr_q2 INT,
    r_q3 INT,
    qr_q3 INT,
    r_q4 INT,
    qr_q4 INT,
    medres1 INT,
    qmedres1 INT,
    medres2 INT,
    qmedres2 INT,
    metod_obt INT,
    origen VARCHAR(255),
    created_on DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by VARCHAR(100),
    updated_on DATETIME,
    updated_by VARCHAR(100),
    temporal INT DEFAULT 0,
    INDEX idx_cliente (id_cliente),
    INDEX idx_fecha (fecha)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Medidas cuarto-horarias';

--rollback DROP TABLE medidaqh;
```

### Ejemplo 2: Agregar Columna

**Archivo:** `002_RA-013_add_status_column.sql`

```sql
--liquibase formatted sql

--changeset maria:RA-013
--comment Agregar columna de estado para seguimiento de procesamiento

ALTER TABLE medidaqh 
ADD COLUMN estado VARCHAR(50) DEFAULT 'PENDIENTE' 
COMMENT 'Estado: PENDIENTE, PROCESADO, ERROR';

--rollback ALTER TABLE medidaqh DROP COLUMN estado;
```

### Ejemplo 3: Crear Índices

**Archivo:** `003_RA-014_add_performance_indexes.sql`

```sql
--liquibase formatted sql

--changeset juan:RA-014
--comment Agregar índices para optimizar queries frecuentes

CREATE INDEX idx_medidaqh_cliente ON medidaqh(id_cliente);
CREATE INDEX idx_medidaqh_fecha ON medidaqh(fecha);
CREATE INDEX idx_medidaqh_cliente_fecha ON medidaqh(id_cliente, fecha);
CREATE INDEX idx_medidaqh_origen ON medidaqh(origen);

--rollback DROP INDEX idx_medidaqh_cliente ON medidaqh;
--rollback DROP INDEX idx_medidaqh_fecha ON medidaqh;
--rollback DROP INDEX idx_medidaqh_cliente_fecha ON medidaqh;
--rollback DROP INDEX idx_medidaqh_origen ON medidaqh;
```

### Ejemplo 4: Insertar Datos de Referencia

**Archivo:** `004_RA-015_insert_reference_data.sql`

```sql
--liquibase formatted sql

--changeset pedro:RA-015
--comment Insertar datos de referencia para configuración inicial

INSERT INTO configuracion (clave, valor, descripcion, created_by) VALUES
('max_retrys', '3', 'Número máximo de reintentos', 'system'),
('timeout_segundos', '30', 'Timeout para operaciones', 'system'),
('cache_ttl', '300', 'TTL del caché en segundos', 'system');

--rollback DELETE FROM configuracion WHERE clave IN ('max_retrys', 'timeout_segundos', 'cache_ttl');
```

### Ejemplo 5: Actualizar Datos Existentes

**Archivo:** `005_RA-016_update_legacy_data.sql`

```sql
--liquibase formatted sql

--changeset ana:RA-016
--comment Actualizar datos legacy con formato antiguo

UPDATE medidaqh 
SET estado = 'PROCESADO' 
WHERE estado IS NULL 
  AND created_on < '2026-01-01';

--rollback UPDATE medidaqh SET estado = NULL WHERE estado = 'PROCESADO' AND created_on < '2026-01-01';
```

### Ejemplo 6: Modificar Columna

**Archivo:** `006_RA-017_modify_column_type.sql`

```sql
--liquibase formatted sql

--changeset carlos:RA-017
--comment Cambiar tipo de columna actent a BIGINT para soportar valores mayores

ALTER TABLE medidaqh 
MODIFY COLUMN actent BIGINT;

--rollback ALTER TABLE medidaqh MODIFY COLUMN actent INT;
```

### Ejemplo 7: Agregar Foreign Key

**Archivo:** `007_RA-018_add_foreign_key.sql`

```sql
--liquibase formatted sql

--changeset laura:RA-018
--comment Agregar relación con tabla clientes

ALTER TABLE medidaqh 
ADD CONSTRAINT fk_medida_cliente 
FOREIGN KEY (id_cliente) REFERENCES clientes(id)
ON DELETE CASCADE
ON UPDATE CASCADE;

--rollback ALTER TABLE medidaqh DROP FOREIGN KEY fk_medida_cliente;
```

### Ejemplo 8: Crear Vista

**Archivo:** `008_RA-019_create_view.sql`

```sql
--liquibase formatted sql

--changeset miguel:RA-019
--comment Crear vista para reportes de consumo diario

CREATE VIEW v_consumo_diario AS
SELECT 
    id_cliente,
    DATE(fecha) as dia,
    SUM(actent) as consumo_total,
    COUNT(*) as num_mediciones
FROM medidaqh
GROUP BY id_cliente, DATE(fecha);

--rollback DROP VIEW v_consumo_diario;
```

### Ejemplo 10: Tabla para Registro de Incidentes/Excepciones/Warnings

**Archivo:** `001_RA-012_create_table_incidents_logs.sql`

**Propósito:** Esta tabla centraliza el registro de excepciones, warnings y errores producidos al procesar archivos, así como datos recibidos desde otros microservicios.

**Características:**
- Registro distribuido con trace_id y span_id
- Clasificación por severidad (CRITICAL, ERROR, WARN, INFO)
- Información del archivo procesado
- Estado para gestión de incidentes
- Datos flexibles con JSON metadata
- Auditoría completa (created_on, updated_on, usuarios)

```sql
--liquibase formatted sql

--changeset jesus:RA-012
--context:dev,qa,prod
--comment Crear DB y flujo para persistencia del registro de incidentes

CREATE TABLE sge.incidents_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- información del servicio
    service_name VARCHAR(100) NOT NULL,
    environment VARCHAR(20), -- dev, qa, prod

    -- endpoint y ejecución
    endpoint VARCHAR(150),
    method_name VARCHAR(150),
    http_method VARCHAR(10),

    -- trazabilidad distribuida
    trace_id VARCHAR(100),
    span_id VARCHAR(100),

    -- usuario
    user_id VARCHAR(50),

    -- información del error
    exception_type VARCHAR(150) NOT NULL,
    message TEXT,
    stack_trace TEXT,

    -- clasificación del error
    severity VARCHAR(20) COMMENT 'CRITICAL, ERROR, WARN, INFO',
    error_code VARCHAR(50),

    -- información del archivo si aplica
    filename VARCHAR(255),
    file_type VARCHAR(50),
    folder_name VARCHAR(50),

    -- estado del error para gestión
    status VARCHAR(20) DEFAULT 'NEW' COMMENT 'NEW, IN_PROGRESS, SOLVED, DISCARDED',

    -- datos adicionales flexibles
    metadata JSON,

    -- auditoría
    created_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by VARCHAR(50)
);

-- índices recomendados
CREATE INDEX idx_exception_service ON incidents_log(service_name);
CREATE INDEX idx_exception_severity ON incidents_log(severity);
CREATE INDEX idx_exception_type ON incidents_log(exception_type);
CREATE INDEX idx_exception_created_on ON incidents_log(created_on);
CREATE INDEX idx_exception_trace ON incidents_log(trace_id);
CREATE INDEX idx_exception_filename ON incidents_log(filename);
CREATE INDEX idx_exception_status ON incidents_log(status);

--rollback DROP TABLE IF EXISTS sge.incidents_log;
```

**Casos de uso:**
- ✅ Registrar excepciones no capturadas
- ✅ Registrar warnings de procesamiento
- ✅ Registrar errores de validación de archivos
- ✅ Recibir datos de incidentes desde otros microservicios
- ✅ Auditoría y análisis de problemas
- ✅ Trazabilidad distribuida con trace_id/span_id

**Queries útiles:**
```sql
-- Ver incidentes críticos sin resolver
SELECT * FROM incidents_log 
WHERE severity = 'CRITICAL' AND status = 'NEW'
ORDER BY created_on DESC;

-- Ver errores por servicio
SELECT service_name, COUNT(*) as count, MAX(created_on) as last_error
FROM incidents_log
WHERE severity IN ('CRITICAL', 'ERROR')
GROUP BY service_name
ORDER BY count DESC;

-- Ver incidentes de un archivo específico
SELECT * FROM incidents_log
WHERE filename LIKE '%archivo_esperado%'
ORDER BY created_on DESC;
```

---



**Archivo:** `009_RA-020_complex_migration.sql`

```sql
--liquibase formatted sql

--changeset roberto:RA-020
--comment Migración compleja: crear tabla, índices y datos iniciales

-- Crear tabla
CREATE TABLE tipo_medida (
    id INT AUTO_INCREMENT PRIMARY KEY,
    codigo VARCHAR(10) UNIQUE NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    created_on DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Crear índices
CREATE INDEX idx_tipo_codigo ON tipo_medida(codigo);
CREATE INDEX idx_tipo_activo ON tipo_medida(activo);

-- Insertar datos iniciales
INSERT INTO tipo_medida (codigo, descripcion) VALUES
('QH', 'Cuarto Horaria'),
('H', 'Horaria'),
('D', 'Diaria');

--rollback DROP TABLE tipo_medida;
```

---

## ⚙️ Configuración

### application.properties

```properties
# ===== Liquibase Configuration =====
# Habilitar Liquibase
spring.liquibase.enabled=true

# Ruta al archivo maestro XML
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.xml

# Schema por defecto
spring.liquibase.default-schema=sge

# No eliminar datos al iniciar (importante!)
spring.liquibase.drop-first=false

# Credenciales (usar variables de entorno)
spring.liquibase.user=${DB_USER_SGE}
spring.liquibase.password=${DB_PASSWORD_SGE}

# Logging de Liquibase
logging.level.liquibase=INFO
logging.level.com.zaxxer.hikari=WARN

# ===== JPA Configuration =====
# Desactivar auto-actualización de esquema (lo maneja Liquibase)
spring.jpa.hibernate.ddl-auto=validate
```

### pom.xml

```xml
<dependencies>
    <!-- Liquibase Core -->
    <dependency>
        <groupId>org.liquibase</groupId>
        <artifactId>liquibase-core</artifactId>
    </dependency>
    
    <!-- MySQL Driver -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <scope>runtime</scope>
    </dependency>
</dependencies>

<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
</properties>
```

---

## 🚀 Cómo Funciona

### Proceso de Inicio

```
1. Spring Boot inicia
   ↓
2. Liquibase se ejecuta automáticamente ANTES de la aplicación
   ↓
3. Lee: db.changelog-master.xml
   ↓
4. Incluye todos los archivos .sql de migrations/ en orden alfabético
   ↓
5. Para cada archivo:
   - Lee el changeset (autor:ticket)
   - Verifica si ya se ejecutó (tabla databasechangelog)
   - Si NO se ejecutó → Ejecuta SQL
   - Si YA se ejecutó → Se salta (idempotencia)
   - Registra en databasechangelog
   ↓
6. Si todos exitosos → Aplicación inicia
   Si alguno falla → Aplicación NO inicia (protección)
   ↓
7. BD sincronizada ✅
```

### Tabla de Control (databasechangelog)

Liquibase crea automáticamente una tabla que registra:

```sql
CREATE TABLE databasechangelog (
    id VARCHAR(255) NOT NULL,           -- autor:ticket (ej: jesus:RA-011)
    author VARCHAR(255) NOT NULL,       -- autor
    filename VARCHAR(255) NOT NULL,     -- migrations/000_RA-011_test.sql
    dateexecuted DATETIME NOT NULL,     -- cuando se ejecutó
    orderexecuted INT NOT NULL,         -- orden de ejecución
    exectype VARCHAR(10) NOT NULL,      -- EXECUTED, FAILED, SKIPPED
    md5sum VARCHAR(35),                 -- checksum del archivo
    description VARCHAR(255),           -- descripción
    comments VARCHAR(255),              -- comentario
    tag VARCHAR(255),
    liquibase VARCHAR(20),
    contexts VARCHAR(255),
    labels VARCHAR(255),
    PRIMARY KEY (id, author, filename)
);
```

**Ver cambios ejecutados:**
```sql
SELECT 
    id,
    author, 
    filename,
    dateexecuted,
    exectype,
    comments
FROM databasechangelog 
ORDER BY orderexecuted;
```

**Resultado ejemplo:**
```
| id          | author | filename                          | dateexecuted        | exectype | comments                |
|-------------|--------|-----------------------------------|---------------------|----------|-------------------------|
| jesus:RA-011| jesus  | migrations/000_RA-011_test.sql   | 2026-03-09 10:00:00 | EXECUTED | Test de implementación  |
| maria:RA-013| maria  | migrations/002_RA-013_add_col.sql| 2026-03-09 10:00:01 | EXECUTED | Agregar columna estado  |
```

---

## 🔧 Cómo Agregar Nueva Migración

### Paso a Paso Completo

#### Paso 1: Obtener ticket de Jira

```
Ticket: RA-017
Título: Agregar columna de email para notificaciones
```

#### Paso 2: Crear archivo con convención

```bash
touch src/main/resources/db/changelog/migrations/010_RA-017_add_email_column.sql
```

**Nota:** Usa el próximo número disponible (000, 001, 002, ... 010)

#### Paso 3: Escribir el contenido

```sql
--liquibase formatted sql

--changeset tu-nombre:RA-017
--comment Agregar columna de email para notificaciones

ALTER TABLE medidaqh 
ADD COLUMN email VARCHAR(255) 
COMMENT 'Email del técnico responsable para notificaciones';

--rollback ALTER TABLE medidaqh DROP COLUMN email;
```

#### Paso 4: Validar sintaxis

```bash
# Verificar que el archivo tiene el formato correcto
cat migrations/010_RA-017_add_email_column.sql

# Primera línea debe ser: --liquibase formatted sql
# Segunda línea debe ser vacía
# Tercera línea debe ser: --changeset autor:TICKET
```

#### Paso 5: Commit a Git

```bash
git add src/main/resources/db/changelog/migrations/010_RA-017_add_email_column.sql
git commit -m "feat(db): RA-017 add email column for notifications"
git push origin feature/RA-017
```

#### Paso 6: Probar localmente

```bash
./mvnw spring-boot:run
```

**En los logs verás:**
```
INFO liquibase: Reading from databasechangelog
INFO liquibase: migrations/010_RA-017_add_email_column.sql: ran successfully
INFO liquibase: Successfully released change log lock
```

#### Paso 7: Verificar en BD

```sql
-- Ver que se ejecutó
SELECT id, filename, exectype 
FROM databasechangelog 
WHERE id = 'tu-nombre:RA-017';

-- Ver la columna
DESCRIBE medidaqh;

-- Resultado esperado:
| Field | Type         | Null | Key | Default | Extra |
|-------|--------------|------|-----|---------|-------|
| email | varchar(255) | YES  |     | NULL    |       |
```

---

## 📊 Integración con Jira

### Flujo Completo

```
1. JIRA: Crear ticket
   RA-017: Agregar email para notificaciones
   ↓
2. GIT: Crear rama
   git checkout -b feature/RA-017-add-email
   ↓
3. SQL: Crear migración
   010_RA-017_add_email_column.sql
   ↓
4. CHANGESET: Usar ticket en changeset
   --changeset jesus:RA-017
   --comment Agregar email para notificaciones
   ↓
5. COMMIT: Mensaje con ticket
   git commit -m "feat(db): RA-017 add email column"
   ↓
6. PR: Code review
   Revisar SQL en PR
   ↓
7. MERGE: A main/develop
   git merge feature/RA-017-add-email
   ↓
8. DEPLOY: Automático
   ./mvnw spring-boot:run
   ↓
9. BD: Actualizada automáticamente
   Liquibase ejecuta 010_RA-017_add_email_column.sql
   ↓
10. JIRA: Marcar como completado
    RA-017: Done ✅
```

### Trazabilidad

Con este flujo puedes:
- ✅ Saber qué cambio de BD corresponde a qué ticket de Jira
- ✅ Ver quién hizo el cambio
- ✅ Cuándo se ejecutó en cada ambiente
- ✅ Auditoría completa en Git + BD + Jira

---

## 🆘 Solución de Problemas

### Problema 1: Changeset ya ejecutado pero necesita modificación

**Síntoma:** Ya ejecutaste el archivo pero necesitas cambiar el SQL.

**❌ MAL: Modificar el archivo existente**
```bash
# NO HACER ESTO:
nano migrations/010_RA-017_add_email_column.sql  # y modificar
```

**✅ BIEN: Crear un nuevo archivo**
```bash
# Crear uno nuevo:
touch migrations/011_RA-018_fix_email_column.sql
```

```sql
--liquibase formatted sql

--changeset tu-nombre:RA-018
--comment Corregir tipo de columna email

ALTER TABLE medidaqh 
MODIFY COLUMN email VARCHAR(500);  -- Ahora más largo

--rollback ALTER TABLE medidaqh MODIFY COLUMN email VARCHAR(255);
```

### Problema 2: Error al ejecutar migration

**Síntoma:** La aplicación no inicia con error de Liquibase.

**Ver logs detallados:**
```bash
# Cambiar nivel de log a DEBUG
# En application.properties:
logging.level.liquibase=DEBUG

# Ejecutar
./mvnw spring-boot:run
```

**Verificar en BD:**
```sql
-- Ver último changeset ejecutado
SELECT * FROM databasechangelog ORDER BY orderexecuted DESC LIMIT 1;

-- Ver si hay locks
SELECT * FROM databasechangeloglock;
```

**Ver error específico:**
```bash
# Logs completos
./mvnw spring-boot:run 2>&1 | grep -A 10 "liquibase"
```

### Problema 3: Lock bloqueado

**Síntoma:** Error "Waiting for changelog lock"

**Causa:** Liquibase dejó un lock porque la app se cerró abruptamente.

**Solución:**
```sql
-- Verificar lock
SELECT * FROM databasechangeloglock;

-- Si está locked=1, liberar:
UPDATE databasechangeloglock SET locked = 0;

-- O simplemente borrar:
DELETE FROM databasechangeloglock;
```

### Problema 4: Checksum mismatch

**Síntoma:** "Checksum does not match"

**Causa:** Se modificó un archivo SQL ya ejecutado.

**Solución:**

**Opción 1 (Recomendada):** Crear nuevo changeset
```bash
touch migrations/012_RA-019_fix_previous.sql
```

**Opción 2 (NO recomendada):** Limpiar checksum
```sql
UPDATE databasechangelog 
SET md5sum = NULL 
WHERE id = 'autor:RA-017';
```

### Problema 5: Orden de ejecución incorrecto

**Síntoma:** Archivo 010 se ejecutó antes que 002.

**Causa:** Orden alfabético incorrecto en nombres.

**Solución:** Usar prefijos con ceros a la izquierda:
```
✅ BIEN:
000_RA-011_test.sql
001_RA-012_create.sql
010_RA-020_add.sql
100_RA-100_big.sql

❌ MAL:
1_RA-011_test.sql      # Se ejecutará después de 10
10_RA-020_add.sql
2_RA-012_create.sql    # Se ejecutará después de 10
```

---

## 📚 Comandos Maven Útiles

### Ver estado de migraciones

```bash
./mvnw liquibase:status
```

**Output:**
```
3 change sets have not been applied to root@localhost@jdbc:mysql://localhost:3306/sge
     migrations/010_RA-017_add_email_column.sql::tu-nombre:RA-017
```

### Ver SQL sin ejecutar

```bash
./mvnw liquibase:updateSQL
```

**Output:** Muestra el SQL que se ejecutaría sin ejecutarlo realmente.

### Ejecutar migraciones manualmente

```bash
./mvnw liquibase:update
```

**Nota:** Normalmente no necesitas esto, Spring Boot lo hace automáticamente.

### Rollback última migración

```bash
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

### Ver SQL de rollback

```bash
./mvnw liquibase:rollbackSQL -Dliquibase.rollbackCount=1
```

### Limpiar BD (¡CUIDADO!)

```bash
./mvnw liquibase:dropAll
```

**⚠️ Advertencia:** Esto elimina TODAS las tablas incluyendo databasechangelog.

---

## 🎯 Mejores Prácticas

### ✅ HACER

1. **Usar convención de nombres**
   ```
   <orden>_<ticket>_<descripcion>.sql
   ```

2. **Incluir ticket de Jira en changeset**
   ```sql
   --changeset jesus:RA-011
   ```

3. **Comentar con título del ticket**
   ```sql
   --comment Crear tabla principal
   ```

4. **Definir rollback siempre que sea posible**
   ```sql
   --rollback DROP TABLE ejemplo;
   ```

5. **Un cambio lógico por archivo**
   - Un archivo = una tabla
   - Un archivo = agregar columna
   - Un archivo = conjunto de índices relacionados

6. **Probar en desarrollo primero**
   ```bash
   ./mvnw spring-boot:run
   # Verificar que funciona antes de merge
   ```

7. **Commits atómicos con mensaje claro**
   ```bash
   git commit -m "feat(db): RA-017 add email column for notifications"
   ```

8. **Prefijos con ceros a la izquierda**
   ```
   000, 001, 002, ... 010, 011, ... 100, 101
   ```

### ❌ EVITAR

1. **Modificar archivos ya ejecutados**
   ```
   ❌ NO: Editar 010_RA-017_add_email.sql después de ejecutar
   ✅ SÍ: Crear 011_RA-018_fix_email.sql
   ```

2. **Cambios muy grandes en un archivo**
   ```
   ❌ NO: 50 tablas en un archivo
   ✅ SÍ: Dividir en múltiples archivos
   ```

3. **Sin estrategia de rollback**
   ```
   ❌ NO: --rollback SELECT 1; (cuando el cambio SÍ es reversible)
   ✅ SÍ: --rollback DROP TABLE ejemplo;
   ```

4. **IDs genéricos sin ticket**
   ```
   ❌ NO: --changeset admin:001
   ✅ SÍ: --changeset jesus:RA-011
   ```

5. **Modificar tabla DATABASECHANGELOG manualmente**
   ```
   ❌ NO: UPDATE databasechangelog ...
   ✅ SÍ: Usar comandos Maven de Liquibase
   ```

6. **Ejecutar SQL directamente en BD**
   ```
   ❌ NO: ALTER TABLE medidaqh ADD COLUMN email ... (directo en MySQL)
   ✅ SÍ: Crear migración SQL con Liquibase
   ```

---

## 🔄 Flujo de Trabajo Recomendado

### Para Desarrollo Individual

```bash
# 1. Pull latest
git pull origin main

# 2. Crear rama con ticket
git checkout -b feature/RA-017-add-email

# 3. Crear migración
touch migrations/010_RA-017_add_email.sql

# 4. Escribir SQL
nano migrations/010_RA-017_add_email.sql

# 5. Probar localmente
./mvnw spring-boot:run

# 6. Verificar en BD
mysql -u root -p sge
> SELECT * FROM databasechangelog WHERE id LIKE '%RA-017%';
> DESCRIBE medidaqh;

# 7. Si funciona, commit
git add migrations/010_RA-017_add_email.sql
git commit -m "feat(db): RA-017 add email column"

# 8. Push y PR
git push origin feature/RA-017-add-email
```

### Para Equipo (Múltiples Desarrolladores)

```bash
# Desarrollador A: RA-015
touch migrations/010_RA-015_add_status.sql
git commit -m "feat(db): RA-015 add status"

# Desarrollador B: RA-016 (al mismo tiempo)
touch migrations/011_RA-016_add_email.sql
git commit -m "feat(db): RA-016 add email"

# Al hacer merge:
# - 010_RA-015 se ejecuta primero (orden alfabético)
# - 011_RA-016 se ejecuta segundo
# - Sin conflictos ✅
```

---

## 📊 Estrategias de Rollback

### Rollback Simple

```sql
-- Para CREATE TABLE
--rollback DROP TABLE nombre_tabla;

-- Para ALTER TABLE ADD COLUMN
--rollback ALTER TABLE nombre_tabla DROP COLUMN nombre_columna;

-- Para CREATE INDEX
--rollback DROP INDEX idx_nombre ON nombre_tabla;
```

### Rollback Complejo

```sql
-- Para UPDATE (guardar estado anterior)
UPDATE medidaqh SET estado = 'PROCESADO' WHERE id > 1000;

--rollback UPDATE medidaqh SET estado = 'PENDIENTE' WHERE id > 1000;
```

### Rollback No Necesario

```sql
-- Para INSERT de datos de prueba
INSERT INTO test_data VALUES (1, 'test');

--rollback SELECT 1;  -- No es crítico revertir
```

### Rollback Múltiple

```sql
-- Para múltiples operaciones
CREATE TABLE tabla1 (...);
CREATE TABLE tabla2 (...);
CREATE INDEX idx1 ON tabla1(col);

--rollback DROP TABLE tabla2;
--rollback DROP TABLE tabla1;
-- Nota: en orden inverso
```

---

## 🎓 Casos de Uso Avanzados

### Migración Condicional

```sql
--liquibase formatted sql

--changeset pedro:RA-020
--comment Agregar columna solo si no existe
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.columns WHERE table_name='medidaqh' AND column_name='email'

ALTER TABLE medidaqh ADD COLUMN email VARCHAR(255);

--rollback ALTER TABLE medidaqh DROP COLUMN email;
```

### Migración con Contextos

```sql
--liquibase formatted sql

--changeset luis:RA-021
--comment Datos de prueba solo para desarrollo
--context:development

INSERT INTO medidaqh (id_cliente, tipomed, fecha, actent) VALUES
(9999, 1, NOW(), 1000);

--rollback DELETE FROM medidaqh WHERE id_cliente = 9999;
```

**Ejecutar solo en dev:**
```bash
./mvnw spring-boot:run -Dliquibase.contexts=development
```

### Migración con Múltiples Changesets

**Archivo:** `020_RA-030_refactor_tables.sql`

```sql
--liquibase formatted sql

--changeset carlos:RA-030-1
--comment Crear tabla auxiliar
CREATE TABLE tmp_medidas (id INT);
--rollback DROP TABLE tmp_medidas;

--changeset carlos:RA-030-2
--comment Migrar datos
INSERT INTO tmp_medidas SELECT id_medidaQH FROM medidaqh;
--rollback DELETE FROM tmp_medidas;

--changeset carlos:RA-030-3
--comment Limpiar tabla temporal
DROP TABLE tmp_medidas;
--rollback CREATE TABLE tmp_medidas (id INT);
```

---

## 📋 Checklist Antes de Commit

Antes de hacer commit de una nueva migración, verifica:

- [ ] Nombre del archivo sigue convención: `<orden>_<ticket>_<descripcion>.sql`
- [ ] Primera línea es: `--liquibase formatted sql`
- [ ] Changeset incluye: `--changeset <autor>:<ticket>`
- [ ] Comentario tiene: `--comment <titulo del ticket>`
- [ ] SQL es válido y probado
- [ ] Rollback está definido correctamente
- [ ] Archivo probado localmente: `./mvnw spring-boot:run`
- [ ] No hay errores en logs de Liquibase
- [ ] Verificado en BD que el cambio se aplicó
- [ ] Commit message incluye ticket: `feat(db): RA-017 ...`

---

## 🔍 Comandos de Verificación

### Ver estructura de archivos

```bash
ls -la src/main/resources/db/changelog/migrations/
```

### Ver contenido del maestro

```bash
cat src/main/resources/db/changelog/db.changelog-master.xml
```

### Ver changesets ejecutados

```sql
SELECT 
    CONCAT(author, ':', id) as changeset_id,
    filename,
    dateexecuted,
    exectype
FROM databasechangelog 
ORDER BY orderexecuted;
```

### Ver changesets pendientes

```bash
./mvnw liquibase:status
```

---

## 📖 Referencias

- [Liquibase Documentation](https://docs.liquibase.com/)
- [SQL Format Documentation](https://docs.liquibase.com/concepts/changelogs/sql-format.html)
- [Liquibase Best Practices](https://docs.liquibase.com/workflows/liquibase-community/liquibase-best-practices.html)
- [Spring Boot & Liquibase](https://docs.spring.io/spring-boot/reference/features/sql.html#features.sql.liquibase)

---

## ✨ Resumen

**Liquibase con formato SQL proporciona:**

✅ **Simplicidad** - SQL puro, fácil de entender  
✅ **Trazabilidad** - Integración completa con Jira  
✅ **Versionado** - Control total con Git  
✅ **Auditoría** - Historial en databasechangelog  
✅ **Automatización** - Se ejecuta al iniciar  
✅ **Reversibilidad** - Estrategia de rollback clara  
✅ **Reproducibilidad** - Mismo esquema en todos los ambientes  

**Resultado:** Una BD profesionalmente gestionada, trazable desde Jira hasta producción.

---

<div align="center">

**¡Gestión profesional de BD con Liquibase + SQL + Jira!** 🚀

</div>


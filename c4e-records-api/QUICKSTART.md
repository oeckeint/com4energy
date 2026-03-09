# 🚀 Quick Start Guide - Records API

> **Guía rápida para tener la aplicación corriendo en menos de 5 minutos**

---

## ⚡ Inicio Rápido (3 Pasos)

### 1️⃣ Configurar Base de Datos

```bash
# Crea un archivo .env en la raíz del proyecto
cp .env.example .env

# Edita .env con tus credenciales de MySQL
DB_URL_SGE=jdbc:mysql://localhost:3306/tu_base_datos
DB_USER_SGE=tu_usuario
DB_PASSWORD_SGE=tu_password
```

### 2️⃣ Ejecutar la Aplicación

```bash
# Opción A: Con Maven
./mvnw spring-boot:run

# Opción B: Con Docker
docker-compose up -d
```

### 3️⃣ Probar la API

```bash
# Verificar que está corriendo
curl http://localhost:8082/actuator/health

# Hacer una petición de prueba
curl "http://localhost:8082/medidaqh?page=0&size=5"
```

**¡Listo!** La API está corriendo en `http://localhost:8082` 🎉

---

## 📋 Tabla de Contenidos

- [Requisitos Previos](#-requisitos-previos)
- [Instalación Detallada](#-instalación-detallada)
- [Primeros Pasos](#-primeros-pasos)
- [Endpoints Principales](#-endpoints-principales)
- [Pruebas Rápidas](#-pruebas-rápidas)
- [Troubleshooting](#-troubleshooting)

---

## ✅ Requisitos Previos

Antes de comenzar, asegúrate de tener instalado:

- ☑️ **Java 17** o superior
  ```bash
  java -version
  # Debería mostrar: openjdk version "17.x.x" o superior
  ```

- ☑️ **Maven 3.9+** (incluido con el wrapper `./mvnw`)
  ```bash
  ./mvnw -version
  ```

- ☑️ **MySQL 8.0+** corriendo
  ```bash
  mysql --version
  # Debería mostrar: mysql Ver 8.0.x
  ```

- ☑️ **Docker** (opcional, para despliegue con contenedores)
  ```bash
  docker --version
  docker-compose --version
  ```

---

## 🔧 Instalación Detallada

### Opción 1: Instalación Local (Desarrollo)

#### Paso 1: Clonar el Repositorio

```bash
git clone <repository-url>
cd c4e-records-api
```

#### Paso 2: Configurar Variables de Entorno

```bash
# Copiar el template de configuración
cp .env.example .env

# Editar con tus valores reales
nano .env   # o usa tu editor favorito
```

**Contenido mínimo de `.env`:**
```env
DB_URL_SGE=jdbc:mysql://localhost:3306/com4energy_db?useSSL=false&serverTimezone=UTC
DB_USER_SGE=root
DB_PASSWORD_SGE=tu_password
```

#### Paso 3: Crear la Base de Datos (si no existe)

```bash
mysql -u root -p
```

```sql
CREATE DATABASE IF NOT EXISTS com4energy_db;
USE com4energy_db;

-- La aplicación creará las tablas automáticamente con JPA
-- O puedes importar tu schema existente
```

#### Paso 4: Compilar el Proyecto

```bash
./mvnw clean install
```

**Output esperado:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: XX.XXX s
```

#### Paso 5: Ejecutar la Aplicación

```bash
./mvnw spring-boot:run
```

**Output esperado:**
```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.5.4)

...
Started RecordsApiApplication in X.XXX seconds
```

#### Paso 6: Verificar que está corriendo

```bash
# Health check
curl http://localhost:8082/actuator/health

# Debería responder:
# {"status":"UP"}
```

---

### Opción 2: Instalación con Docker (Producción)

#### Paso 1: Configurar Variables de Entorno

```bash
cp .env.example .env
# Editar .env con tus configuraciones
```

#### Paso 2: Construir y Ejecutar con Docker Compose

```bash
# Construir la imagen
docker-compose build

# Iniciar el contenedor
docker-compose up -d

# Ver logs
docker-compose logs -f records-api
```

#### Paso 3: Verificar que está corriendo

```bash
curl http://localhost:8082/actuator/health
```

#### Comandos Útiles de Docker

```bash
# Ver logs
docker-compose logs -f

# Detener la aplicación
docker-compose down

# Reiniciar
docker-compose restart

# Ver estado
docker-compose ps

# Entrar al contenedor
docker-compose exec records-api bash
```

---

## 🎯 Primeros Pasos

### 1. Verificar Endpoints Disponibles

```bash
# Actuator - Información de la aplicación
curl http://localhost:8082/actuator

# Health check
curl http://localhost:8082/actuator/health
```

### 2. Consultar Medidas (GET)

```bash
# Obtener primeras 10 medidas
curl "http://localhost:8082/medidaqh?page=0&size=10"

# Filtrar por cliente
curl "http://localhost:8082/medidaqh?idCliente=1"

# Filtrar por rango de fechas
curl "http://localhost:8082/medidaqh?startDate=2026-01-01&endDate=2026-03-08"

# Últimas 24 horas
curl "http://localhost:8082/medidaqh/last24h?idCliente=1"

# Buscar por ID
curl "http://localhost:8082/medidaqh/123"
```

### 3. Crear una Medida (POST)

```bash
curl -X POST "http://localhost:8082/medidaqh" \
  -H "Content-Type: application/json" \
  -d '{
    "id_cliente": 1,
    "tipomed": 1,
    "fecha": "2026-03-08T10:00:00",
    "bandera_inv_ver": 0,
    "actent": 1500,
    "qactent": 500,
    "qactsal": 200,
    "r_q1": 100,
    "qr_q1": 50,
    "r_q2": 100,
    "qr_q2": 50,
    "r_q3": 100,
    "qr_q3": 50,
    "r_q4": 100,
    "qr_q4": 50,
    "medres1": 0,
    "qmedres1": 0,
    "medres2": 0,
    "qmedres2": 0,
    "metod_obt": 1,
    "origen": "API",
    "temporal": 0
  }'
```

### 4. Ver Logs de AOP en Acción

```bash
# En una terminal, ejecuta:
./mvnw spring-boot:run

# En otra terminal, ejecuta:
./test-aop.sh

# Observa los logs de AOP:
# 🔵 [CONTROLLER] - Logging automático
# ⚙️  [SERVICE] - Ejecución de servicios
# ⚡ [PERFORMANCE] - Métricas de rendimiento
# 📝 [AUDIT] - Auditoría de operaciones
```

---

## 📡 Endpoints Principales

### Base URL
```
http://localhost:8082
```

### Endpoints de Medidas

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/medidaqh` | Listar medidas con filtros y paginación |
| `GET` | `/medidaqh/{id}` | Obtener medida por ID |
| `GET` | `/medidaqh/last24h` | Medidas de últimas 24 horas |
| `POST` | `/medidaqh` | Crear nueva medida |

### Endpoints de Monitoreo

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/actuator/health` | Estado de salud de la aplicación |
| `GET` | `/actuator/info` | Información de la aplicación |

---

## 🧪 Pruebas Rápidas

### Colección Postman/Insomnia

Crea una colección con estas peticiones:

#### 1. Health Check
```http
GET http://localhost:8082/actuator/health
```

#### 2. Listar Medidas
```http
GET http://localhost:8082/medidaqh?page=0&size=5
```

#### 3. Buscar por Cliente
```http
GET http://localhost:8082/medidaqh?idCliente=1&page=0&size=10
```

#### 4. Buscar por Fechas
```http
GET http://localhost:8082/medidaqh?startDate=2026-01-01&endDate=2026-03-08&page=0&size=10
```

#### 5. Crear Medida
```http
POST http://localhost:8082/medidaqh
Content-Type: application/json

{
  "id_cliente": 1,
  "tipomed": 1,
  "fecha": "2026-03-08T10:00:00",
  "actent": 1500,
  "qactent": 500
}
```

### Script de Prueba Automatizado

```bash
# Ejecutar todas las pruebas de AOP
./test-aop.sh

# Ver los logs de la aplicación para ver los aspectos en acción
```

---

## 🐛 Troubleshooting

### Problema: "Error connecting to database"

**Síntomas:**
```
Unable to open JDBC Connection for DDL execution
```

**Solución:**
1. Verificar que MySQL está corriendo:
   ```bash
   mysql -u root -p
   ```

2. Verificar las credenciales en `.env`:
   ```env
   DB_URL_SGE=jdbc:mysql://localhost:3306/com4energy_db
   DB_USER_SGE=root
   DB_PASSWORD_SGE=tu_password_correcto
   ```

3. Verificar que la base de datos existe:
   ```sql
   SHOW DATABASES;
   ```

---

### Problema: "Port 8082 already in use"

**Síntomas:**
```
Port 8082 was already in use
```

**Solución:**

**Opción 1: Cambiar el puerto**
```bash
# En .env o application.properties
SERVER_PORT=8083
```

**Opción 2: Matar el proceso que usa el puerto**
```bash
# macOS/Linux
lsof -ti:8082 | xargs kill -9

# Windows
netstat -ano | findstr :8082
taskkill /PID <PID> /F
```

---

### Problema: "Cannot resolve symbol in IDE"

**Síntomas:**
- IDE muestra errores en imports de AOP
- Lombok no funciona

**Solución:**

**Para IntelliJ IDEA:**
1. File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
2. Enable annotation processing ✓
3. File → Invalidate Caches → Invalidate and Restart

**Para VS Code:**
1. Instalar extensión "Java Extension Pack"
2. Instalar extensión "Lombok Annotations Support"
3. Recargar window

**Maven:**
```bash
./mvnw clean install
```

---

### Problema: "Tests failing"

**Síntomas:**
```
Tests run: X, Failures: Y
```

**Solución:**
```bash
# Saltar tests temporalmente
./mvnw clean install -DskipTests

# O ejecutar solo compilación
./mvnw clean compile
```

---

### Problema: "OutOfMemoryError"

**Síntomas:**
```
java.lang.OutOfMemoryError: Java heap space
```

**Solución:**
```bash
# Aumentar memoria heap
export MAVEN_OPTS="-Xmx1024m"
./mvnw spring-boot:run

# O en application.properties
spring.jpa.properties.hibernate.jdbc.batch_size=20
```

---

### Problema: "Aspect not working"

**Síntomas:**
- No ves logs de aspectos (🔵, ⚙️, ⚡, etc.)

**Solución:**

1. Verificar que la dependencia AOP está en pom.xml:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-aop</artifactId>
   </dependency>
   ```

2. Verificar que los aspectos tienen `@Component`:
   ```java
   @Aspect
   @Component  // ← Esto es necesario
   public class LoggingAspect { ... }
   ```

3. Recompilar:
   ```bash
   ./mvnw clean compile
   ```

---

## 📚 Siguientes Pasos

Una vez que tengas la aplicación corriendo:

1. **Explorar la API** - Lee el [README.md](README.md) completo
2. **Entender AOP** - Revisa [AOP-QUICKSTART.md](AOP-QUICKSTART.md)
3. **Conocer la Arquitectura** - Lee [ARCHITECTURE.md](ARCHITECTURE.md)
4. **Probar Aspectos** - Ejecuta `./test-aop.sh`

---

## 🆘 Obtener Ayuda

Si tienes problemas:

1. **Revisar logs**: `./mvnw spring-boot:run` (salida en consola)
2. **Docker logs**: `docker-compose logs -f`
3. **Consultar documentación**: README.md, AOP-README.md, ARCHITECTURE.md
4. **Contactar al equipo**: [info de contacto]

---

## ✅ Checklist de Verificación

Antes de considerar que todo está corriendo correctamente:

- [ ] MySQL está corriendo y accesible
- [ ] Base de datos existe y tiene las tablas
- [ ] Archivo `.env` configurado con credenciales correctas
- [ ] Aplicación inicia sin errores
- [ ] `curl http://localhost:8082/actuator/health` responde `{"status":"UP"}`
- [ ] Puedes consultar medidas: `curl http://localhost:8082/medidaqh`
- [ ] Logs de AOP se muestran en consola
- [ ] Puerto 8082 está disponible y responde

---

<div align="center">

**¡Todo listo! Ahora puedes empezar a usar Records API** 🚀

</div>


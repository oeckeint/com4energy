# 📊 Records API - Sistema Central de Gestión de Datos

![Java](https://img.shields.io/badge/Java-17-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-orange)
![License](https://img.shields.io/badge/License-Proprietary-red)

> **Microservicio central** para la gestión de registros de medidas energéticas en el ecosistema Com4Energy. Este servicio actúa como el **corazón de la arquitectura de microservicios**, proporcionando acceso centralizado a los datos de mediciones y operaciones CRUD sobre la base de datos principal.

---

## 📋 Tabla de Contenidos

- [Descripción General](#-descripción-general)
- [Arquitectura](#-arquitectura)
- [Características](#-características)
- [Tecnologías](#-tecnologías)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Instalación y Configuración](#-instalación-y-configuración)
- [Uso de la API](#-uso-de-la-api)
- [Programación Orientada a Aspectos (AOP)](#-programación-orientada-a-aspectos-aop)
- [Roadmap](#-roadmap)
- [Arquitectura Futura](#-arquitectura-futura)
- [Consideraciones de Performance](#-consideraciones-de-performance)
- [Documentación Adicional](#-documentación-adicional)
- [Contribución](#-contribución)

---

## 🎯 Descripción General

**Records API** es el microservicio principal del ecosistema Com4Energy, diseñado para:

- **Centralizar el acceso a datos** de mediciones energéticas (MedidaQH)
- **Proporcionar operaciones CRUD** seguras y eficientes sobre la base de datos
- **Servir como fuente única de verdad** para otros microservicios
- **Procesar y almacenar** datos enviados desde múltiples aplicaciones
- **Escalar horizontalmente** para soportar alta concurrencia

### Rol en la Arquitectura

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Frontend App   │     │  Mobile App     │     │  External Apps  │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         └───────────────┬───────┴───────────────────────┘
                         │  HTTP/REST
                    ┌────▼─────┐
                    │          │
                    │ Records  │◄────── API Gateway
                    │   API    │
                    │          │
                    └────┬─────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌─────▼─────┐   ┌────▼────┐
    │ MySQL   │    │ RabbitMQ  │   │  Cache  │
    │   DB    │    │  (Futuro) │   │ (Redis) │
    └─────────┘    └───────────┘   └─────────┘
```

---

## 🏗️ Arquitectura

### Arquitectura en Capas (Actual)

```
┌─────────────────────────────────────────────────────────┐
│                    Controllers                          │
│  (Capa de Presentación - REST Endpoints)                │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    Services                             │
│  (Capa de Lógica de Negocio)                            │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                  Repositories                           │
│  (Capa de Acceso a Datos - JPA)                         │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│                    MySQL Database                       │
└─────────────────────────────────────────────────────────┘
```

### Preocupaciones Transversales (AOP)

```
                  ┌─→ LoggingAspect (Logs automáticos)
                  ├─→ AuditAspect (Auditoría de cambios)
                  ├─→ PerformanceAspect (Monitoreo)
Controllers ──────┼─→ ValidationAspect (Validaciones)
                  └─→ CacheAspect (Caché en memoria)
```

---

## ✨ Características

### Características Actuales

✅ **API RESTful**
- Endpoints para operaciones CRUD sobre mediciones
- Paginación y ordenamiento de resultados
- Filtrado por cliente y rango de fechas
- Consultas optimizadas con ventanas de tiempo

✅ **Gestión de Medidas Cuarto-Horarias (MedidaQH)**
- Almacenamiento de lecturas energéticas
- Consulta de últimas 24 horas
- Filtrado avanzado por cliente y fechas
- Búsqueda por ID

✅ **Programación Orientada a Aspectos (AOP)**
- Logging automático con métricas de rendimiento
- Auditoría de operaciones de escritura
- Validaciones reutilizables con anotaciones
- Sistema de caché simple en memoria
- Monitoreo de performance de queries

✅ **Manejo de Errores**
- Gestión global de excepciones
- Respuestas de error estandarizadas
- Mensajes internacionalizables

✅ **Configuración CORS**
- Configurado para desarrollo con frontend local
- Fácilmente extensible para producción

✅ **Dockerización**
- Dockerfile multi-stage para optimización
- Docker Compose para despliegue fácil
- Variables de entorno para configuración

✅ **Observabilidad**
- Spring Boot Actuator integrado
- Logs estructurados con niveles configurables
- Métricas de tiempo de ejecución automáticas

---

## 🛠️ Tecnologías

### Backend
- **Java 17** - Lenguaje de programación
- **Spring Boot 3.5.4** - Framework principal
- **Spring Data JPA** - Persistencia de datos
- **Spring AOP** - Programación orientada a aspectos
- **Spring Boot Actuator** - Monitoreo y métricas

### Base de Datos
- **MySQL 8.0** - Base de datos relacional principal

### Herramientas
- **Maven 3.9+** - Gestión de dependencias
- **Lombok** - Reducción de código boilerplate
- **Docker** - Contenerización

### Futuras
- **RabbitMQ** - Mensajería asíncrona (En roadmap)
- **Redis** - Caché distribuido (En roadmap)
- **Spring Cloud** - Microservicios avanzados (En evaluación)

---

## 📁 Estructura del Proyecto

```
c4e-records-api/
├── src/
│   └── main/
│       ├── java/com/com4energy/recordsapi/
│       │   ├── RecordsApiApplication.java          # Punto de entrada
│       │   ├── aspect/                             # Aspectos AOP
│       │   │   ├── LoggingAspect.java             # Logging automático
│       │   │   ├── AuditAspect.java               # Auditoría
│       │   │   ├── PerformanceAspect.java         # Monitoreo
│       │   │   ├── ValidationAspect.java          # Validaciones
│       │   │   ├── CacheAspect.java               # Caché
│       │   │   └── annotation/                    # Anotaciones custom
│       │   ├── common/                            # Constantes y utilidades
│       │   │   ├── CoreConstants.java
│       │   │   ├── MessageKey.java
│       │   │   └── Messages.java
│       │   ├── config/                            # Configuración
│       │   │   └── CorsConfig.java
│       │   ├── controller/                        # Controladores REST
│       │   │   ├── cliente/
│       │   │   ├── common/                        # DTOs y mappers comunes
│       │   │   └── medidas/
│       │   │       └── qh/
│       │   │           └── MedidaQHController.java
│       │   ├── dto/                               # Entidades JPA
│       │   │   └── MedidaQH.java
│       │   ├── exception/                         # Manejo de excepciones
│       │   │   ├── BusinessException.java
│       │   │   ├── ResourceNotFoundException.java
│       │   │   └── handler/
│       │   │       └── GlobalExceptionHandler.java
│       │   ├── repository/                        # Repositorios JPA
│       │   │   └── MedidaQHRepository.java
│       │   ├── response/                          # Respuestas estandarizadas
│       │   │   └── ApiError.java
│       │   └── service/                           # Lógica de negocio
│       │       └── MedidaQHService.java
│       └── resources/
│           ├── application.properties             # Configuración principal
│           └── messages.properties                # Mensajes i18n
├── docker-compose.yml                             # Orquestación Docker
├── Dockerfile                                     # Imagen Docker
├── pom.xml                                        # Dependencias Maven
├── AOP-README.md                                  # Documentación AOP completa
├── AOP-QUICKSTART.md                              # Guía rápida AOP
├── AOP-SUMMARY.md                                 # Resumen AOP
├── test-aop.sh                                    # Script de pruebas AOP
└── README.md                                      # Este archivo
```

---

## 🚀 Instalación y Configuración

### Requisitos Previos

- **Java 17+** instalado
- **Maven 3.9+** instalado
- **MySQL 8.0+** corriendo
- **Docker** (opcional, para despliegue con contenedores)

### Configuración de Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto:

```env
# Database Configuration
DB_URL_SGE=jdbc:mysql://localhost:3306/com4energy_db?useSSL=false&serverTimezone=UTC
DB_USER_SGE=tu_usuario
DB_PASSWORD_SGE=tu_password
```

### Instalación Local

#### 1. Clonar el repositorio
```bash
git clone <repository-url>
cd c4e-records-api
```

#### 2. Compilar el proyecto
```bash
./mvnw clean install
```

#### 3. Ejecutar la aplicación
```bash
./mvnw spring-boot:run
```

La aplicación estará disponible en `http://localhost:8082`

### Instalación con Docker

#### 1. Construir la imagen
```bash
docker-compose build
```

#### 2. Iniciar el contenedor
```bash
docker-compose up -d
```

#### 3. Ver logs
```bash
docker-compose logs -f records-api
```

#### 4. Detener el contenedor
```bash
docker-compose down
```

---

## 📡 Uso de la API

### Base URL
```
http://localhost:8082
```

### Endpoints Disponibles

#### 1. Obtener Medidas con Filtros
```http
GET /medidaqh?idCliente={id}&startDate={fecha}&endDate={fecha}&page={n}&size={m}
```

**Parámetros:**
- `idCliente` (opcional): ID del cliente
- `startDate` (opcional): Fecha inicio (formato: `YYYY-MM-DD` o `YYYY-MM-DDTHH:mm:ss`)
- `endDate` (opcional): Fecha fin (formato: `YYYY-MM-DD` o `YYYY-MM-DDTHH:mm:ss`)
- `page` (opcional, default: 0): Número de página
- `size` (opcional, default: 20): Tamaño de página
- `sort` (opcional, default: fecha,asc): Campo y dirección de ordenamiento

**Ejemplo:**
```bash
curl "http://localhost:8082/medidaqh?idCliente=1&startDate=2026-01-01&endDate=2026-03-08&page=0&size=10"
```

**Respuesta:**
```json
{
  "content": [
    {
      "idMedidaQH": 1,
      "id_cliente": 1,
      "tipomed": 1,
      "fecha": "2026-03-08T10:00:00",
      "actent": 1500,
      "qactent": 500,
      ...
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 100,
  "totalPages": 10
}
```

#### 2. Obtener Medidas de Últimas 24 Horas
```http
GET /medidaqh/last24h?idCliente={id}
```

**Ejemplo:**
```bash
curl "http://localhost:8082/medidaqh/last24h?idCliente=1"
```

#### 3. Obtener Medida por ID
```http
GET /medidaqh/{id}
```

**Ejemplo:**
```bash
curl "http://localhost:8082/medidaqh/123"
```

**Respuesta:**
```json
{
  "idMedidaQH": 123,
  "id_cliente": 1,
  "tipomed": 1,
  "fecha": "2026-03-08T10:00:00",
  "actent": 1500,
  ...
}
```

#### 4. Crear Nueva Medida
```http
POST /medidaqh
Content-Type: application/json
```

**Body:**
```json
{
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
}
```

**Ejemplo:**
```bash
curl -X POST "http://localhost:8082/medidaqh" \
  -H "Content-Type: application/json" \
  -d '{
    "id_cliente": 1,
    "tipomed": 1,
    "fecha": "2026-03-08T10:00:00",
    "actent": 1500,
    "qactent": 500
  }'
```

**Respuesta:**
```json
{
  "idMedidaQH": 124,
  "id_cliente": 1,
  "tipomed": 1,
  "fecha": "2026-03-08T10:00:00",
  "actent": 1500,
  ...
}
```

### Códigos de Respuesta HTTP

| Código | Significado |
|--------|-------------|
| `200 OK` | Solicitud exitosa |
| `201 Created` | Recurso creado exitosamente |
| `400 Bad Request` | Solicitud inválida o parámetros incorrectos |
| `404 Not Found` | Recurso no encontrado |
| `500 Internal Server Error` | Error interno del servidor |

### Formato de Error

```json
{
  "timestamp": "2026-03-08T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "MedidaQH with id 999 was not found",
  "path": "/medidaqh/999"
}
```

---

## 🎯 Programación Orientada a Aspectos (AOP)

Este proyecto utiliza **Spring AOP** para implementar funcionalidades transversales de manera centralizada y reutilizable.

### Aspectos Implementados

#### 1. 📝 LoggingAspect
**Automático** - Logging de todas las operaciones

```
🔵 [CONTROLLER] MedidaQHController → getAll() - Parámetros: [...]
⚙️  [SERVICE] MedidaQHService → findAll() - Completado en 45 ms
✅ [CONTROLLER] MedidaQHController → getAll() - Completado en 52 ms
```

#### 2. 📊 AuditAspect
**Automático** - Auditoría de operaciones de escritura

```
📝 [AUDIT] 2026-03-08 14:30:15 - MedidaQHService → save() - GUARDADO
🗑️  [AUDIT] 2026-03-08 14:35:22 - MedidaQHService → deleteById() - ELIMINACIÓN
```

#### 3. ⚡ PerformanceAspect
**Automático** - Monitoreo de performance

```
⚡ [PERFORMANCE] findByFilters() - 23 ms
🐢 [PERFORMANCE] OPERACIÓN LENTA - findAll() - 1250 ms ⚠️
```

#### 4. ✓ ValidationAspect
**Uso con anotación** - Validaciones reutilizables

```java
@ValidateClienteExists(paramName = "clienteId")
public Page<MedidaQH> findAll(Integer clienteId, ...) {
    // Validación automática antes de ejecutar
}
```

#### 5. 💾 CacheAspect
**Uso con anotación** - Caché en memoria

```java
@Cacheable(ttlSeconds = 60)
public Optional<MedidaQH> findById(int id) {
    // Primera vez: consulta BD
    // Siguientes veces: devuelve desde caché
}
```

### Documentación AOP Completa

Para más información sobre AOP, consulta:
- **AOP-QUICKSTART.md** - Guía rápida con ejemplos
- **AOP-README.md** - Documentación completa
- **AOP-SUMMARY.md** - Resumen ejecutivo

### Probar Aspectos AOP

```bash
# 1. Iniciar la aplicación
./mvnw spring-boot:run

# 2. Ejecutar script de pruebas
./test-aop.sh
```

---

## 🗺️ Roadmap

### ✅ Fase 1: MVP Funcional (Completado)
- [x] API REST básica con CRUD
- [x] Integración con MySQL
- [x] Dockerización
- [x] Implementación de AOP
- [x] Logging y auditoría automáticos
- [x] Manejo de errores global

### 🚧 Fase 2: Expansión de Endpoints (En Progreso)
- [ ] Endpoints para gestión de clientes
- [ ] Endpoints para diferentes tipos de medidas
- [ ] Endpoints de agregación y estadísticas
- [ ] Búsqueda avanzada con múltiples filtros
- [ ] Exportación de datos (CSV, Excel)

### 📋 Fase 3: Mensajería Asíncrona (Planificado)
- [ ] Integración con RabbitMQ
- [ ] Cola de procesamiento de medidas entrantes
- [ ] Publicación de eventos de cambios
- [ ] Dead Letter Queue para errores
- [ ] Retry automático de mensajes fallidos

### 🔐 Fase 4: Seguridad y Autenticación (Planificado)
- [ ] Integración con Spring Security
- [ ] Autenticación con JWT
- [ ] Roles y permisos por endpoint
- [ ] Rate limiting por usuario/IP
- [ ] API Keys para aplicaciones externas

### ⚡ Fase 5: Optimización y Escalabilidad (Planificado)
- [ ] Implementación de Redis para caché distribuido
- [ ] Optimización de queries con índices
- [ ] Paginación optimizada (cursor-based)
- [ ] Compresión de respuestas HTTP
- [ ] Connection pooling avanzado

### 📊 Fase 6: Observabilidad Avanzada (Planificado)
- [ ] Integración con Prometheus
- [ ] Dashboards en Grafana
- [ ] Tracing distribuido con Zipkin/Jaeger
- [ ] Alertas automáticas de errores
- [ ] Health checks personalizados

---

## 🏛️ Arquitectura Futura

### Arquitectura Event-Driven con RabbitMQ

```
┌────────────────┐                    ┌──────────────────┐
│  External App  │────JSON────────────►│                  │
└────────────────┘                    │   API Gateway    │
                                      │                  │
┌────────────────┐                    └────────┬─────────┘
│  Mobile App    │────HTTP────────────────────►│
└────────────────┘                             │
                                               │
                ┌──────────────────────────────▼─────────┐
                │          Records API                   │
                │  ┌──────────────┐  ┌─────────────┐    │
                │  │ REST Layer   │  │ Async Layer │    │
                │  └──────┬───────┘  └──────┬──────┘    │
                │         │                  │           │
                │         │     ┌────────────▼──────┐    │
                │         │     │  RabbitMQ Client  │    │
                │         │     └────────────┬──────┘    │
                │         │                  │           │
                │    ┌────▼──────────────────▼───┐       │
                │    │     Service Layer        │       │
                │    └────────────┬─────────────┘       │
                │                 │                      │
                │    ┌────────────▼─────────────┐       │
                │    │   Repository Layer       │       │
                │    └────────────┬─────────────┘       │
                └─────────────────┼────────────────────┘
                                  │
        ┌─────────────────────────┼──────────────────────┐
        │                         │                      │
   ┌────▼────┐            ┌───────▼──────┐      ┌───────▼──────┐
   │ MySQL   │            │   RabbitMQ   │      │    Redis     │
   │   DB    │            │   Message    │      │    Cache     │
   │         │            │    Broker    │      │              │
   └─────────┘            └──────────────┘      └──────────────┘
                                  │
                                  │ (Consumer)
                                  │
                    ┌─────────────▼──────────────┐
                    │   Processing Service       │
                    │   (Validación + Transform) │
                    └────────────────────────────┘
```

### Flujos de Datos

#### Flujo Síncrono (REST)
```
Cliente → REST API → Validación → Service → Repository → MySQL → Respuesta
```

#### Flujo Asíncrono (RabbitMQ) - Futuro
```
Cliente → REST API → RabbitMQ Queue → Background Worker → Validación → Repository → MySQL
                              ↓
                         (Respuesta inmediata: 202 Accepted)
```

---

## ⚡ Consideraciones de Performance

### ¿Procesar JSON internamente hace la API más lenta?

**Respuesta corta:** Depende del volumen y complejidad del procesamiento.

#### ✅ **Ventajas de Procesamiento Interno**

1. **Validación Centralizada**
   - Una sola fuente de verdad para reglas de negocio
   - Consistencia de datos garantizada
   - Reducción de datos inválidos en BD

2. **Simplicidad para Clientes**
   - Clientes no necesitan conocer lógica de negocio
   - API más intuitiva y fácil de usar
   - Menos código duplicado en clientes

3. **Control Total**
   - Transformaciones y enriquecimiento de datos
   - Logging y auditoría unificados
   - Manejo de errores consistente

#### ⚠️ **Desventajas Potenciales**

1. **Carga en el Servidor**
   - CPU adicional para procesamiento
   - Memoria para parsear y validar JSON
   - Posible cuello de botella en alta concurrencia

2. **Latencia**
   - Tiempo adicional de procesamiento
   - Puede afectar tiempo de respuesta

### 🚀 **Estrategias de Optimización**

#### 1. **Procesamiento Asíncrono con RabbitMQ** (Recomendado)

```java
@PostMapping("/medidaqh/async")
public ResponseEntity<AsyncResponse> saveAsync(@RequestBody MedidaQH medida) {
    // Publicar en cola RabbitMQ
    String messageId = rabbitTemplate.convertAndSend("medidas.queue", medida);
    
    // Respuesta inmediata
    return ResponseEntity.accepted()
        .body(new AsyncResponse(messageId, "Processing"));
}
```

**Ventajas:**
- ✅ Respuesta inmediata al cliente (< 50ms)
- ✅ Procesamiento en background
- ✅ Desacopla carga de trabajo
- ✅ Retry automático en caso de error
- ✅ Escalable horizontalmente

#### 2. **Batch Processing para Volumen Alto**

```java
@PostMapping("/medidaqh/batch")
public ResponseEntity<BatchResponse> saveBatch(@RequestBody List<MedidaQH> medidas) {
    // Procesar en lote
    String batchId = batchService.processBatch(medidas);
    
    return ResponseEntity.accepted()
        .body(new BatchResponse(batchId, medidas.size()));
}
```

#### 3. **Validación en Capas**

```java
// Validación básica (rápida) en REST
@PostMapping("/medidaqh")
public ResponseEntity<?> save(@Valid @RequestBody MedidaQH medida) {
    // @Valid hace validación básica (< 1ms)
    
    // Procesamiento complejo en background
    asyncService.processAndValidate(medida);
    
    return ResponseEntity.accepted().build();
}
```

#### 4. **Caché Inteligente**

```java
@Cacheable(ttlSeconds = 300)
public ValidationResult validateCliente(Integer clienteId) {
    // Resultados de validación cacheados
    // Evita consultas repetitivas a BD
}
```

#### 5. **Connection Pool Optimizado**

```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
```

### 📊 **Benchmarks Estimados**

| Escenario | Tiempo | Throughput |
|-----------|--------|------------|
| REST síncrono simple | 50-100ms | ~200 req/s |
| REST con validación compleja | 100-300ms | ~50 req/s |
| REST + RabbitMQ (async) | 10-30ms | ~1000 req/s |
| Batch processing | 5-10ms/item | ~5000 items/s |

### 🎯 **Recomendación Final**

**Para el caso de uso de Com4Energy:**

1. **Operaciones de Lectura** → REST síncrono con caché
2. **Operaciones de Escritura Individuales** → REST síncrono (si < 1000 req/min)
3. **Operaciones de Escritura Masivas** → RabbitMQ asíncrono (si > 1000 req/min)
4. **Procesamiento Complejo** → RabbitMQ + Worker dedicado

**Conclusión:** El procesamiento interno NO hará la API significativamente lenta si:
- ✅ Usas procesamiento asíncrono para cargas altas
- ✅ Implementas caché estratégicamente
- ✅ Optimizas queries de BD
- ✅ Escalas horizontalmente cuando sea necesario

---

## 📚 Documentación Adicional

### Documentos Disponibles

- **README.md** (este archivo) - Documentación principal del proyecto
- **AOP-QUICKSTART.md** - Guía rápida de uso de aspectos AOP
- **AOP-README.md** - Documentación completa de AOP
- **AOP-SUMMARY.md** - Resumen ejecutivo de AOP
- **test-aop.sh** - Script de pruebas de aspectos

### Enlaces Útiles

- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Data JPA](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)
- [Spring AOP](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#aop)
- [RabbitMQ Tutorials](https://www.rabbitmq.com/getstarted.html)
- [Redis Documentation](https://redis.io/documentation)

---

## 🤝 Contribución

### Flujo de Trabajo

1. **Crea una rama feature**
   ```bash
   git checkout -b feature/nueva-funcionalidad
   ```

2. **Realiza tus cambios**
   ```bash
   git add .
   git commit -m "feat: descripción de la funcionalidad"
   ```

3. **Push a la rama**
   ```bash
   git push origin feature/nueva-funcionalidad
   ```

4. **Crea un Pull Request**

### Convenciones de Código

- **Nombres de variables**: camelCase
- **Nombres de clases**: PascalCase
- **Constantes**: UPPER_SNAKE_CASE
- **Paquetes**: lowercase
- **Usar Lombok** para reducir boilerplate
- **Javadoc** en clases y métodos públicos
- **Tests unitarios** para nueva funcionalidad

### Estructura de Commits

```
feat: nueva funcionalidad
fix: corrección de bug
docs: cambios en documentación
style: formateo de código
refactor: refactorización
test: añadir tests
chore: tareas de mantenimiento
```

---

## 📄 Licencia

Este proyecto es propiedad de **Com4Energy**. Todos los derechos reservados.

---

## 👥 Equipo

**Com4Energy Development Team**

---

## 📞 Contacto

Para preguntas o soporte, contacta al equipo de desarrollo de Com4Energy.

---

## 🏷️ Versión

**Versión Actual:** 0.0.1-SNAPSHOT

**Última Actualización:** Marzo 2026

---

<div align="center">

**Construido con ❤️ por el equipo de Com4Energy**

</div>


# 🏗️ Arquitectura y Patrones de Diseño - Records API

## Índice
- [Patrones de Diseño Implementados](#patrones-de-diseño-implementados)
- [Decisiones Arquitectónicas](#decisiones-arquitectónicas)
- [Escalabilidad con RabbitMQ](#escalabilidad-con-rabbitmq)
- [Análisis de Performance](#análisis-de-performance)
- [Mejores Prácticas](#mejores-prácticas)

---

## 🎨 Patrones de Diseño Implementados

### 1. Repository Pattern
**Propósito:** Abstracción del acceso a datos

```java
public interface MedidaQHRepository extends JpaRepository<MedidaQH, Integer> {
    @Query("select m from MedidaQH m where (:clienteId is null or m.id_cliente = :clienteId)")
    Page<MedidaQH> findByFilters(@Param("clienteId") Integer clienteId, ...);
}
```

**Ventajas:**
- ✅ Desacopla lógica de negocio del acceso a datos
- ✅ Facilita testing con mocks
- ✅ Permite cambiar implementación de persistencia

### 2. Service Layer Pattern
**Propósito:** Centralizar lógica de negocio

```java
@Service
public class MedidaQHService {
    @ValidateClienteExists
    @Cacheable(ttlSeconds = 60)
    public Page<MedidaQH> findAll(...) {
        // Lógica de negocio aquí
    }
}
```

**Ventajas:**
- ✅ Lógica reutilizable
- ✅ Transacciones manejadas por Spring
- ✅ Punto único para aplicar aspectos

### 3. Aspect-Oriented Programming (AOP)
**Propósito:** Separar preocupaciones transversales

```java
@Aspect
@Component
public class LoggingAspect {
    @Around("controllerMethods()")
    public Object logExecution(ProceedingJoinPoint joinPoint) {
        // Logging, timing, etc.
    }
}
```

**Ventajas:**
- ✅ Código más limpio y mantenible
- ✅ Funcionalidades transversales centralizadas
- ✅ Fácil activar/desactivar features

### 4. DTO Pattern (Data Transfer Object)
**Propósito:** Transferir datos entre capas

```java
@Entity
@Data
public class MedidaQH {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int idMedidaQH;
    // ...
}
```

**Ventajas:**
- ✅ Separación entre modelo de BD y API
- ✅ Control sobre qué datos se exponen
- ✅ Versionado de API más fácil

### 5. Exception Handler Pattern
**Propósito:** Manejo global de excepciones

```java
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(...) {
        // Manejo centralizado
    }
}
```

**Ventajas:**
- ✅ Respuestas de error consistentes
- ✅ Menos código duplicado
- ✅ Logging automático de errores

### 6. Factory Pattern (Helper Classes)
**Propósito:** Crear objetos complejos

```java
public class DateRangeHelper {
    public static DateRange applyDefaultWindow(...) {
        // Lógica de creación
    }
}
```

### 7. Builder Pattern (con Lombok)
**Propósito:** Construcción fluida de objetos

```java
@Builder
@Data
public class ApiError {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
}
```

---

## 🏛️ Decisiones Arquitectónicas

### ¿Por qué Arquitectura en Capas?

```
┌─────────────────────────────────────┐
│         Controllers                 │  ← Expone API REST
├─────────────────────────────────────┤
│         Services                    │  ← Lógica de negocio
├─────────────────────────────────────┤
│         Repositories                │  ← Acceso a datos
├─────────────────────────────────────┤
│         Database                    │  ← Persistencia
└─────────────────────────────────────┘
```

**Razones:**
1. **Separación de Responsabilidades (SRP)**
   - Cada capa tiene un propósito específico
   - Fácil de entender y mantener

2. **Testabilidad**
   - Cada capa se puede probar independientemente
   - Mocks fáciles de crear

3. **Escalabilidad**
   - Fácil agregar nuevas funcionalidades
   - No se viola Open/Closed Principle

4. **Reutilización**
   - Services pueden ser usados por múltiples controllers
   - Repositories compartidos entre services

### ¿Por qué Spring Boot?

**Ventajas para Records API:**

1. **Rapidez de Desarrollo**
   - Auto-configuración
   - Starter dependencies
   - Embedded server

2. **Ecosistema Robusto**
   - Spring Data JPA para BD
   - Spring AOP para aspectos
   - Spring Actuator para monitoring

3. **Producción-Ready**
   - Health checks
   - Metrics
   - Logging configurado

4. **Integración Fácil**
   - RabbitMQ con Spring AMQP
   - Redis con Spring Data Redis
   - Security con Spring Security

---

## 🐰 Escalabilidad con RabbitMQ

### Problema: Alta Carga de Escritura

**Escenario:**
- 10,000 mediciones/minuto desde múltiples fuentes
- Procesamiento complejo (validación, transformación, cálculos)
- No puede afectar tiempo de respuesta de API

### Solución: Event-Driven Architecture

#### Arquitectura Propuesta

```
┌─────────────────────────────────────────────────────────────────┐
│                      Records API (Producer)                     │
│                                                                 │
│  POST /medidaqh/async                                           │
│       │                                                         │
│       ├─► Validación Básica (< 5ms)                            │
│       │                                                         │
│       ├─► Publica a RabbitMQ                                   │
│       │                                                         │
│       └─► Return 202 Accepted + messageId                      │
│                                                                 │
└─────────────────────┬───────────────────────────────────────────┘
                      │
                      │ JSON Message
                      ▼
        ┌─────────────────────────────┐
        │        RabbitMQ             │
        │  ┌───────────────────────┐  │
        │  │  medidas.input.queue  │  │  ← Entrada
        │  └───────────┬───────────┘  │
        │              │               │
        │  ┌───────────▼───────────┐  │
        │  │  Exchange (Topic)     │  │  ← Enrutamiento
        │  └───────────┬───────────┘  │
        │              │               │
        │  ┌───────────▼───────────┐  │
        │  │ medidas.process.queue │  │  ← Procesamiento
        │  └───────────┬───────────┘  │
        │              │               │
        │  ┌───────────▼───────────┐  │
        │  │  medidas.dlq.queue    │  │  ← Errores
        │  └───────────────────────┘  │
        └─────────────────────────────┘
                      │
                      │ Consume
                      ▼
        ┌─────────────────────────────┐
        │   Processing Workers        │
        │   (N instancias)            │
        │                             │
        │  ┌──────────────────────┐   │
        │  │ 1. Validación Full   │   │
        │  │ 2. Transformación    │   │
        │  │ 3. Cálculos          │   │
        │  │ 4. Enriquecimiento   │   │
        │  └──────────┬───────────┘   │
        └─────────────┼─────────────────┘
                      │
                      │ Save
                      ▼
        ┌─────────────────────────────┐
        │         MySQL DB            │
        └─────────────────────────────┘
```

### Implementación con Spring AMQP

#### 1. Configuración RabbitMQ

```java
@Configuration
public class RabbitMQConfig {
    
    public static final String INPUT_QUEUE = "medidas.input.queue";
    public static final String PROCESS_QUEUE = "medidas.process.queue";
    public static final String DLQ_QUEUE = "medidas.dlq.queue";
    public static final String EXCHANGE = "medidas.exchange";
    
    @Bean
    public Queue inputQueue() {
        return QueueBuilder.durable(INPUT_QUEUE)
            .withArgument("x-dead-letter-exchange", "")
            .withArgument("x-dead-letter-routing-key", DLQ_QUEUE)
            .build();
    }
    
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }
    
    @Bean
    public Binding binding(Queue inputQueue, TopicExchange exchange) {
        return BindingBuilder.bind(inputQueue)
            .to(exchange)
            .with("medida.#");
    }
}
```

#### 2. Producer (REST Controller)

```java
@RestController
@RequestMapping("/medidaqh")
public class MedidaQHController {
    
    private final RabbitTemplate rabbitTemplate;
    
    @PostMapping("/async")
    public ResponseEntity<AsyncResponse> saveAsync(@Valid @RequestBody MedidaQH medida) {
        // Validación básica ya hecha por @Valid (< 5ms)
        
        // Generar ID único para tracking
        String messageId = UUID.randomUUID().toString();
        
        // Crear mensaje
        MedidaMessage message = MedidaMessage.builder()
            .messageId(messageId)
            .medida(medida)
            .timestamp(LocalDateTime.now())
            .build();
        
        // Publicar a RabbitMQ (< 10ms)
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EXCHANGE,
            "medida.create",
            message
        );
        
        // Respuesta inmediata
        return ResponseEntity.accepted()
            .body(AsyncResponse.builder()
                .messageId(messageId)
                .status("ACCEPTED")
                .message("Medida en proceso")
                .estimatedProcessingTime("1-5 segundos")
                .statusUrl("/medidaqh/status/" + messageId)
                .build());
    }
    
    @GetMapping("/status/{messageId}")
    public ResponseEntity<ProcessingStatus> getStatus(@PathVariable String messageId) {
        // Consultar estado del procesamiento
        return ResponseEntity.ok(statusService.getStatus(messageId));
    }
}
```

#### 3. Consumer (Background Worker)

```java
@Component
@Slf4j
public class MedidaConsumer {
    
    private final MedidaQHService medidaService;
    private final StatusService statusService;
    
    @RabbitListener(queues = RabbitMQConfig.INPUT_QUEUE)
    public void processMedida(MedidaMessage message) {
        log.info("Procesando medida con messageId: {}", message.getMessageId());
        
        try {
            // Actualizar estado a PROCESSING
            statusService.updateStatus(message.getMessageId(), "PROCESSING");
            
            // Validación completa (puede ser costosa)
            validationService.validateFull(message.getMedida());
            
            // Transformaciones
            MedidaQH transformed = transformationService.transform(message.getMedida());
            
            // Cálculos adicionales
            calculationService.calculateDerived(transformed);
            
            // Enriquecimiento con datos externos (si aplica)
            enrichmentService.enrich(transformed);
            
            // Guardar en BD
            MedidaQH saved = medidaService.save(transformed);
            
            // Actualizar estado a COMPLETED
            statusService.updateStatus(
                message.getMessageId(), 
                "COMPLETED", 
                saved.getIdMedidaQH()
            );
            
            log.info("Medida procesada exitosamente: {}", saved.getIdMedidaQH());
            
        } catch (ValidationException e) {
            log.error("Error de validación: {}", e.getMessage());
            statusService.updateStatus(message.getMessageId(), "FAILED", e.getMessage());
            throw new AmqpRejectAndDontRequeueException(e);
            
        } catch (Exception e) {
            log.error("Error procesando medida: {}", e.getMessage(), e);
            statusService.updateStatus(message.getMessageId(), "FAILED", e.getMessage());
            
            // Reintentar (irá a DLQ después de max retries)
            throw e;
        }
    }
    
    @RabbitListener(queues = RabbitMQConfig.DLQ_QUEUE)
    public void processDeadLetter(MedidaMessage message) {
        log.error("Medida en DLQ (Dead Letter Queue): {}", message.getMessageId());
        
        // Guardar en tabla de errores para revisión manual
        errorService.saveForManualReview(message);
        
        // Enviar alerta
        alertService.sendAlert("Medida falló después de reintentos", message);
    }
}
```

#### 4. Tracking de Estado

```java
@Service
public class StatusService {
    
    private final RedisTemplate<String, ProcessingStatus> redisTemplate;
    
    public void updateStatus(String messageId, String status) {
        ProcessingStatus ps = ProcessingStatus.builder()
            .messageId(messageId)
            .status(status)
            .timestamp(LocalDateTime.now())
            .build();
            
        // Guardar en Redis con TTL de 24 horas
        redisTemplate.opsForValue().set(
            "status:" + messageId, 
            ps, 
            24, 
            TimeUnit.HOURS
        );
    }
    
    public ProcessingStatus getStatus(String messageId) {
        ProcessingStatus status = redisTemplate.opsForValue()
            .get("status:" + messageId);
            
        if (status == null) {
            throw new ResourceNotFoundException("Status not found");
        }
        
        return status;
    }
}
```

### Ventajas del Modelo Asíncrono

| Aspecto | Síncrono | Asíncrono con RabbitMQ |
|---------|----------|------------------------|
| **Tiempo de Respuesta** | 100-500ms | 10-30ms |
| **Throughput** | ~200 req/s | ~1000+ req/s |
| **Escalabilidad** | Vertical | Horizontal |
| **Resiliencia** | Falla = Pérdida | Retry automático |
| **Complejidad** | Baja | Media |
| **Observabilidad** | Básica | Avanzada (tracking) |

### Consideraciones

#### ¿Cuándo usar Asíncrono?

✅ **SÍ, usa asíncrono cuando:**
- Alta carga de escritura (> 1000 req/min)
- Procesamiento costoso (> 100ms)
- Múltiples fuentes de datos
- Necesitas resiliencia (retry automático)
- Escalabilidad horizontal es prioritaria

❌ **NO uses asíncrono cuando:**
- Baja carga (< 100 req/min)
- Cliente necesita resultado inmediato
- Procesamiento simple (< 10ms)
- Arquitectura simple es prioritaria

---

## ⚡ Análisis de Performance

### Bottlenecks Comunes

#### 1. **Query N+1 Problem**

**Problema:**
```java
// ❌ MAL: 1 query para medidas + N queries para clientes
List<MedidaQH> medidas = repository.findAll();
for (MedidaQH medida : medidas) {
    Cliente cliente = clienteRepository.findById(medida.getIdCliente());
    // ... N queries adicionales
}
```

**Solución:**
```java
// ✅ BIEN: 1 query con JOIN
@Query("SELECT m FROM MedidaQH m LEFT JOIN FETCH m.cliente WHERE ...")
List<MedidaQH> findAllWithCliente();
```

#### 2. **Missing Indexes**

**Problema:** Queries lentas en tablas grandes

**Solución:**
```sql
-- Crear índices en columnas frecuentemente filtradas
CREATE INDEX idx_medida_cliente ON medidaqh(id_cliente);
CREATE INDEX idx_medida_fecha ON medidaqh(fecha);
CREATE INDEX idx_medida_cliente_fecha ON medidaqh(id_cliente, fecha);
```

#### 3. **Connection Pool Exhaustion**

**Problema:** Requests esperando conexiones de BD

**Solución:**
```properties
# Optimizar HikariCP
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000
```

#### 4. **Large Result Sets**

**Problema:** Cargar miles de registros en memoria

**Solución:**
```java
// ✅ Usar paginación siempre
Page<MedidaQH> findAll(Pageable pageable);

// ✅ O streaming para procesamiento
@QueryHints(@QueryHint(name = "org.hibernate.fetchSize", value = "50"))
Stream<MedidaQH> streamAll();
```

### Métricas Objetivo

| Métrica | Objetivo | Crítico |
|---------|----------|---------|
| **API Response Time** | < 200ms | > 1000ms |
| **Database Query Time** | < 50ms | > 500ms |
| **Memory Usage** | < 70% | > 90% |
| **CPU Usage** | < 60% | > 80% |
| **Error Rate** | < 0.1% | > 1% |
| **Throughput** | > 500 req/s | < 100 req/s |

---

## 🎯 Mejores Prácticas

### 1. Validación en Capas

```java
// Controller: Validación básica (estructura)
@PostMapping
public ResponseEntity<?> save(@Valid @RequestBody MedidaQH medida) {
    // @Valid valida anotaciones de Bean Validation
}

// Service: Validación de negocio
@Service
public class MedidaQHService {
    public MedidaQH save(MedidaQH medida) {
        // Validar reglas de negocio complejas
        if (medida.getActent() < 0) {
            throw new BusinessException("Actent no puede ser negativo");
        }
    }
}
```

### 2. Manejo de Transacciones

```java
@Service
public class MedidaQHService {
    
    @Transactional(readOnly = true)
    public Page<MedidaQH> findAll(...) {
        // Solo lectura, optimiza performance
    }
    
    @Transactional(
        isolation = Isolation.READ_COMMITTED,
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public MedidaQH save(MedidaQH medida) {
        // Transacción con configuración explícita
    }
}
```

### 3. Logging Estructurado

```java
// ✅ BIEN: Logging estructurado
log.info("Procesando medida - clienteId: {}, fecha: {}, tipo: {}", 
    medida.getIdCliente(), 
    medida.getFecha(), 
    medida.getTipomed()
);

// ❌ MAL: String concatenation
log.info("Procesando medida " + medida.toString());
```

### 4. Exception Handling

```java
// Crear excepciones específicas
public class InvalidMedidaException extends BusinessException {
    public InvalidMedidaException(String message) {
        super(message);
    }
}

// Manejar específicamente
@ExceptionHandler(InvalidMedidaException.class)
public ResponseEntity<?> handleInvalidMedida(InvalidMedidaException e) {
    return ResponseEntity.badRequest()
        .body(ApiError.builder()
            .message(e.getMessage())
            .status(400)
            .build());
}
```

### 5. Configuración Externalizada

```properties
# ✅ BIEN: Configuración en properties
app.medidas.max-batch-size=1000
app.medidas.validation-enabled=true
app.medidas.cache-ttl=300

# Inyectar en código
@Value("${app.medidas.max-batch-size}")
private int maxBatchSize;
```

---

## 📊 Monitoreo y Observabilidad

### Métricas a Monitorear

1. **Application Metrics**
   - Request rate
   - Response time (p50, p95, p99)
   - Error rate
   - Active requests

2. **Database Metrics**
   - Query time
   - Connection pool usage
   - Slow queries
   - Deadlocks

3. **RabbitMQ Metrics** (cuando se implemente)
   - Message rate
   - Queue depth
   - Consumer lag
   - Message age

4. **JVM Metrics**
   - Heap usage
   - GC pauses
   - Thread count
   - CPU usage

### Herramientas Recomendadas

- **Prometheus** - Recolección de métricas
- **Grafana** - Visualización
- **Zipkin/Jaeger** - Distributed tracing
- **ELK Stack** - Logs centralizados

---

## 🔐 Seguridad (Futuro)

### Consideraciones

1. **Autenticación**
   - JWT tokens
   - OAuth2 para apps externas
   - API Keys para servicios

2. **Autorización**
   - Role-based access control (RBAC)
   - Cliente solo puede ver sus medidas
   - Admin puede ver todo

3. **Rate Limiting**
   - Límite por usuario/IP
   - Protección contra DDoS

4. **Input Validation**
   - Sanitización de inputs
   - Prevención de SQL injection
   - Validación de tipos

---

## 📖 Conclusión

La arquitectura de Records API está diseñada para:

✅ **Escalabilidad** - Puede crecer horizontalmente con RabbitMQ  
✅ **Mantenibilidad** - Código limpio con AOP y capas separadas  
✅ **Performance** - Optimizaciones en caché, queries, y async processing  
✅ **Resiliencia** - Retry automático y Dead Letter Queues  
✅ **Observabilidad** - Logging, metrics, y tracing integrados  

**La arquitectura actual es sólida para el MVP y está preparada para escalar según las necesidades futuras.**



# RabbitMQ - Documentacion de Incidentes

## Objetivo
Documentar la topologia RabbitMQ usada por `records-api` para manejar incidentes por tipo (`validation`, `integration`, `system`) y definir una guia operativa clara para DLQ.

## Alcance actual (estado real)
- Existe configuracion de colas, exchanges, bindings, DLX y DLQ por tipo.
- La topologia se construye desde propiedades (`rabbitmq.incidents.types`).
- El contrato de evento y los componentes `IncidentProducer` / `IncidentConsumer` estan creados, pero aun sin logica de negocio implementada.

## Fuente de configuracion
Configuracion y binding:
- `src/main/resources/application.yml` (seccion `rabbitmq.incidents.types`)
- `src/main/java/com/com4energy/recordsapi/messaging/incident/IncidentRabbitProperties.java`
- `src/main/java/com/com4energy/recordsapi/messaging/config/RabbitMQIncidentsConfig.java`

## Topologia actual por tipo de incidente

| Tipo | Queue | Exchange | Routing Key | Dead Letter Exchange (DLX) | Dead Letter Queue (DLQ) |
|---|---|---|---|---|---|
| `validation` | `incident.validation.queue` | `incident.validation.exchange` | `incident.validation.key` | `incident.validation.exchange.dlx` | `incident.validation.queue.dlq` |
| `integration` | `incident.integration.queue` | `incident.integration.exchange` | `incident.integration.key` | `incident.integration.exchange.dlx` | `incident.integration.queue.dlq` |
| `system` | `incident.system.queue` | `incident.system.exchange` | `incident.system.key` | `incident.system.exchange.dlx` | `incident.system.queue.dlq` |

## Contrato de configuracion (referencia)

```yaml
rabbitmq:
  incidents:
    types:
      <type>:
        queue: incident.<type>.queue
        exchange: incident.<type>.exchange
        routingKey: incident.<type>.key
        deadLetterExchange: incident.<type>.exchange.dlx
        deadLetterQueue: incident.<type>.queue.dlq
```

## Como agregar un nuevo tipo/topic de incidente

Ejemplo: agregar el tipo `processing`.

1. Agregar el bloque en `src/main/resources/application.yml` dentro de `rabbitmq.incidents.types`:

```yaml
rabbitmq:
  incidents:
    types:
      processing:
        queue: incident.processing.queue
        exchange: incident.processing.exchange
        routingKey: incident.processing.key
        deadLetterExchange: incident.processing.exchange.dlx
        deadLetterQueue: incident.processing.queue.dlq
```

2. Declarar beans para el nuevo tipo en `src/main/java/com/com4energy/recordsapi/messaging/config/RabbitMQIncidentsConfig.java`:
   - `processingQueue`
   - `processingExchange`
   - `processingBinding`
   - `processingDeadLetterQueue`
   - `processingDeadLetterExchange`
   - `processingDeadLetterBinding`

3. (Cuando aplique) conectar publicacion y consumo de negocio:
   - Publicar eventos del nuevo tipo desde `IncidentProducer`.
   - Consumir y manejar errores en `IncidentConsumer`.

4. Validar cambios:
   - La aplicacion arranca sin errores de `@ConfigurationProperties`.
   - Se crean queue, exchange y bindings en RabbitMQ.
   - Un mensaje fallido termina en su `*.dlq` correspondiente.

Nota importante: hoy `RabbitMQIncidentsConfig` define beans por tipo de forma explicita. Agregar un nuevo tipo requiere configuracion en `application.yml` y alta en codigo.

## Checklist de PR para nuevos topics/tipos

- Se agrego el nuevo tipo en `rabbitmq.incidents.types` dentro de `src/main/resources/application.yml`.
- Se definieron nombres consistentes (`incident.<tipo>.queue`, `incident.<tipo>.exchange`, `incident.<tipo>.key`, `*.dlx`, `*.dlq`).
- Se agregaron beans del nuevo tipo en `src/main/java/com/com4energy/recordsapi/messaging/config/RabbitMQIncidentsConfig.java`.
- Se valido el arranque de la aplicacion sin errores de `@ConfigurationProperties`.
- Se verifico en RabbitMQ que existen queue, exchange y bindings (incluyendo DLX/DLQ).
- Se probo un caso fallido y el mensaje llego a la `deadLetterQueue` esperada.
- Se actualizo esta documentacion si hubo cambios de convencion, naming o flujo.

## Flujo de mensajes y DLQ
1. El productor publica al `exchange` del tipo con su `routingKey`.
2. El mensaje entra en la `queue` principal del tipo.
3. Si falla el procesamiento y se rechaza/no procesa, RabbitMQ enruta al `deadLetterExchange` (`x-dead-letter-exchange`).
4. El mensaje queda en la `deadLetterQueue` para analisis, reproceso o descarte controlado.

Nota tecnica: tambien se define `x-dead-letter-routing-key` en `RabbitMQIncidentsConfig`.

## Guia operativa por tipo de incidente

| Tipo de incidente | Ejemplos | Que hacer con la DLQ |
|---|---|---|
| `validation` | Campos requeridos vacios, formato invalido, reglas de negocio | Revisar manual o por batch, notificar al equipo de datos |
| `integration` | Fallo en llamadas a APIs externas, timeouts, errores HTTP | Reintento programado, notificacion |
| `system` | NullPointerException, RuntimeException inesperada, fallos del microservicio | Logging central, alertas inmediatas, analisis de bugs |

## Runbook minimo de DLQ
1. Identificar tipo (`validation`, `integration`, `system`) y volumen afectado.
2. Tomar muestra de mensajes para clasificar causa raiz.
3. Aplicar accion segun tipo de incidente (tabla anterior).
4. Definir reproceso parcial o total con ventana controlada.
5. Confirmar resultado del reproceso (exitos/fallos repetidos).
6. Registrar incidente: causa, accion, owner, fecha y lecciones aprendidas.

## Alertas y KPIs recomendados
- Profundidad de cada `*.dlq` (mensajes acumulados).
- Edad del mensaje mas antiguo en DLQ.
- Tasa de ingreso a DLQ por tipo (mensajes/hora).
- Tasa de reproceso exitoso por tipo.
- Tiempo medio de resolucion (MTTR) de incidentes en DLQ.

## Buenas practicas de evolucion
- Mantener nombres estables y semanticos (`incident.<tipo>.*`).
- Evitar hardcodear nombres en codigo; usar siempre propiedades.
- Si un entorno necesita nombres distintos, sobrescribir solo `rabbitmq.incidents.types` en perfil.
- Versionar cambios de topologia en PR con impacto operativo documentado.
- Definir politica explicita de retry y descarte para evitar bucles de reproceso.

## Pendientes sugeridos
- Implementar logica real en `IncidentProducer` y `IncidentConsumer`.
- Definir estrategia de retry (backoff, max intentos y circuit breaker).
- Agregar dashboards y alertas automaticas por tipo de DLQ.

## Perfiles de entorno
Actualmente, en `application.yml` la configuracion RabbitMQ de incidentes es comun para `dev`, `qa` y `prod`.
Si cambia por entorno, sobrescribir solo las claves de `rabbitmq.incidents.types` por perfil.

---

## Enumeraciones del dominio de incidentes

Existen cuatro enums relacionados con incidentes. Su distincion es importante para no confundirlos.

### `IncidentType` — selector de canal RabbitMQ
**Paquete:** `domain.enums.incident`

Determina **por qué cola se publica** el evento. Cada valor mapea a un bloque en `rabbitmq.incidents.types` del `application.yml`. Su responsabilidad es exclusivamente de infraestructura/mensajería.

| Valor | Canal activo | Descripción |
|---|---|---|
| `VALIDATION` | `incident.validation.*` | Reglas de negocio, campos inválidos, formatos |
| `INTEGRATION` | `incident.integration.*` | APIs externas, timeouts, errores HTTP |
| `SYSTEM` | `incident.system.*` | RuntimeException, errores inesperados, infraestructura |

> **Importante:** agregar un valor aquí sin su bloque en `application.yml` lanza `BusinessException` en tiempo de ejecución.

---

### `IncidentCategory` — clasificación de negocio del incidente
**Paquete:** `domain.enums.incident`

Describe **qué tipo de problema ocurrió** desde el punto de vista del negocio. Se persiste en la columna `category` de la tabla `incidents`. Tiene más valores que `IncidentType` porque no todos los tipos de error tienen canal de mensajería propio todavía.

| Valor | Ejemplos |
|---|---|
| `APPLICATION` | Errores del API, NullPointerException en lógica propia |
| `FILE_PROCESSING` | Parsing fallido, formato CSV inválido, pipeline roto |
| `INTEGRATION` | Timeout a API externa, respuesta HTTP 5xx, fallo AMQP |
| `VALIDATION` | Campo requerido vacío, violación de regla de negocio |
| `SECURITY` | Token inválido, acceso denegado, intento de escalada |
| `SYSTEM` | OOM, fallo de BD, error de red, JVM crash |

---

### `IncidentSeverity` — nivel de criticidad
**Paquete:** `domain.enums.incident`

Describe **qué tan grave es** el incidente. Se persiste en `severity`. A mayor nivel numérico, mayor prioridad. `isAlertRequired()` retorna `true` para `CRITICAL` y `ERROR`.

| Valor | Nivel | Cuándo usarlo |
|---|---|---|
| `CRITICAL` | 4 | Impacto en producción, servicio caído o datos corruptos |
| `ERROR` | 3 | Fallo funcional real que el usuario o sistema percibe |
| `WARN` | 2 | Anomalía detectada, degradación potencial sin fallo todavía |
| `INFO` | 1 | Registro informativo, sin impacto operativo |

---

### `IncidentStatus` — estado del ciclo de vida
**Paquete:** `domain.enums.incident`

Representa **en qué etapa de gestión** está el incidente. Se persiste en `status`. Valor inicial por defecto: `NEW`.

```
NEW → IN_PROGRESS → SOLVED
                 ↘ DISCARDED
```

`isFinalState()` retorna `true` para `SOLVED` y `DISCARDED`. Ningún estado final debería volver a `NEW` o `IN_PROGRESS`.

---

## Preguntas frecuentes (FAQ)

**¿Cuál es la diferencia entre `IncidentType` e `IncidentCategory`?**

Son conceptos distintos que conviven en el mismo evento:
- `IncidentType` = **canal de mensajería** (dónde viaja el mensaje en RabbitMQ)
- `IncidentCategory` = **clasificación de negocio** (qué tipo de error ocurrió)

Un evento tiene ambos. Ejemplo:
```java
// El tipo de negocio es VALIDATION, y viaja por el canal VALIDATION
producer.send(IncidentType.VALIDATION, new IncidentEvent(
    ..., IncidentCategory.VALIDATION, IncidentSeverity.WARN, ...
));

// Pero también puede haber una validación que viaje por el canal SYSTEM
// si no tiene canal propio aún:
producer.send(IncidentType.SYSTEM, new IncidentEvent(
    ..., IncidentCategory.SECURITY, IncidentSeverity.ERROR, ...
));
```

---

**¿Por qué `IncidentCategory` tiene más valores que `IncidentType`?**

Porque `IncidentType` solo define los canales que están **activos en RabbitMQ hoy**. Valores como `APPLICATION`, `FILE_PROCESSING` y `SECURITY` no tienen cola dedicada todavía. Cuando se agregue una cola para ellos, se añade el valor correspondiente en `IncidentType` y su bloque en `application.yml`.

---

**¿Cuándo agrego un nuevo valor a `IncidentType`?**

Solo cuando se crea el bloque completo en `application.yml` (queue, exchange, routingKey, DLX, DLQ). Ver sección _"Como agregar un nuevo tipo/topic de incidente"_ más arriba.

---

**¿Puedo tener un `IncidentCategory.SECURITY` y publicarlo por `IncidentType.SYSTEM`?**

Sí. El canal que elijas en `IncidentType` es independiente de la categoría del evento. Mientras no exista un canal dedicado para `SECURITY`, publicarlo por `SYSTEM` es válido y esperado.

---

**¿`IncidentSeverity.isAlertRequired()` genera la alerta automáticamente?**

No todavía. El método solo expone la intención. La lógica de alertas real (notificaciones, dashboards, PagerDuty, etc.) es un pendiente — ver sección _"Pendientes sugeridos"_.

---

**¿Qué pasa si envío un `IncidentType` que no tiene bloque en `application.yml`?**

`IncidentProducer` lanza `BusinessException` con clave `INCIDENT_TYPE_NOT_CONFIGURED`. Esto ocurre en tiempo de ejecución al llamar a `producer.send(...)`, no al arrancar la aplicación.

---

## Publicacion desde otros microservicios (Spring Boot)

Esta API recibe incidentes por RabbitMQ. Cualquier microservicio externo que publique debe enviar al **exchange** y **routingKey** correctos segun el tipo de incidente.

### Matriz de enrutamiento para publishers externos

| Caso de uso en el microservicio origen | `IncidentType` sugerido | Exchange destino | Routing key |
|---|---|---|---|
| Errores de validacion/negocio | `VALIDATION` | `incident.validation.exchange` | `incident.validation.key` |
| Errores al llamar APIs/servicios externos | `INTEGRATION` | `incident.integration.exchange` | `incident.integration.key` |
| Errores inesperados/runtime/infra | `SYSTEM` | `incident.system.exchange` | `incident.system.key` |

> Regla practica: elegir el canal por objetivo operativo de la cola (quien lo atiende y como se procesa), no solo por el nombre del error.

### Contrato minimo que debe cumplir el publisher

- Mensaje JSON compatible con `IncidentEvent`.
- Campos obligatorios: `id`, `serviceName`, `exceptionType`, `severity`, `category`.
- Si `timestamp` es `null`, el consumidor lo completa automaticamente.
- `payload` debe ser JSON o texto corto (maximo 1000 caracteres).

### Ejemplo rapido (microservicio Spring Boot publisher)

```yaml
# application.yml del microservicio origen
spring:
  rabbitmq:
    host: ${RABBIT_HOST}
    port: ${RABBIT_PORT:5672}
    username: ${RABBIT_USER}
    password: ${RABBIT_PASSWORD}

c4e:
  incidents:
    enabled: true
    types:
      integration:
        exchange: incident.integration.exchange
        routingKey: incident.integration.key
      validation:
        exchange: incident.validation.exchange
        routingKey: incident.validation.key
      system:
        exchange: incident.system.exchange
        routingKey: incident.system.key
```

```java
// Con c4e-incidents-shared: inyectar IncidentPublisher y publicar por tipo
@Service
@RequiredArgsConstructor
public class ExternalIncidentService {

    private final IncidentPublisher incidentPublisher;

    public void report(IncidentEvent event) {
        incidentPublisher.publish(IncidentType.INTEGRATION, event);
    }
}
```

---

## FAQ de integracion entre microservicios

**¿Otros microservicios pueden usar directamente el JAR generado por este proyecto?**

Si, tecnicamente se puede **si publicas el artefacto en un repositorio Maven** (`Nexus`, `Artifactory`, `GitHub Packages`, etc.) y lo declaras como dependencia.

Pero para integracion entre microservicios, usar el JAR completo de `records-api` normalmente **no es lo ideal** porque:
- arrastra dependencias de web/jpa/infra que el publisher no necesita,
- aumenta acoplamiento entre servicios,
- complica upgrades y compatibilidad.

**Recomendacion adoptada:** usar un unico artefacto compartido llamado `c4e-incidents-shared`.

Contenido del artefacto:
- contrato AMQP (`IncidentEvent` + enums),
- publisher comun (`IncidentPublisher`),
- auto-configuracion Spring Boot para publicar con `RabbitTemplate`.

Coordenadas Maven propuestas:

```xml
<dependency>
  <groupId>com.com4energy</groupId>
  <artifactId>c4e-incidents-shared</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Asi cada microservicio depende solo del contrato+publisher AMQP y no de toda la API.

**¿Si no creamos libreria compartida, se puede igual publicar?**

Si. Basta con acordar y respetar el JSON de `IncidentEvent` + exchange/routing key correctos.

**¿Que cambia cuando agreguemos nuevos canales (nuevas queues)?**

Se debe actualizar:
1. `rabbitmq.incidents.types` en `records-api`.
2. Esta matriz de enrutamiento en `RABBITMQ.md`.
3. La configuracion de publishers externos que deban usar el nuevo canal.
